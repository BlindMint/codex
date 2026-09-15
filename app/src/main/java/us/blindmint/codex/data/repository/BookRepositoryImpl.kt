/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.local.dto.BookEntity
import us.blindmint.codex.data.local.dto.BookProgressHistoryEntity
import us.blindmint.codex.data.local.dto.HistoryEntity
import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.mapper.book.BookMapper
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.data.util.CoverExtractor
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.data.parser.SpeedReaderWordExtractor
import us.blindmint.codex.data.parser.TextParser
import us.blindmint.codex.utils.minSubstringDistance
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.domain.repository.BookRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.collection.LruCache

private const val GET_TEXT = "GET TEXT, REPO"
private const val GET_BOOKS = "GET BOOKS, REPO"
private const val GET_BOOKS_BY_ID = "GET BOOKS, REPO"
private const val INSERT_BOOK = "INSERT BOOK, REPO"
private const val UPDATE_BOOK = "UPDATE BOOK, REPO"
private const val DELETE_BOOKS = "DELETE BOOKS, REPO"

@Suppress("DEPRECATION")
@Singleton
class BookRepositoryImpl @Inject constructor(
    private val application: Application,
    private val database: BookDao,
    private val fileParser: FileParser,
    private val textParser: TextParser,
    private val bookMapper: BookMapper,
    private val coverExtractor: CoverExtractor
) : BookRepository {

    private val textCache = LruCache<Int, List<ReaderText>>(5)

    private val speedReaderWordCache = LruCache<Int, List<SpeedReaderWord>>(10)

    /**
     * Creates a CachedFile from book.filePath, handling both file paths and content URIs.
     */
    private fun getCachedFile(book: BookEntity): CachedFile? {
        return CachedFileFactory.fromBookEntity(application, book)
    }

    /**
     * Get all books matching [query] from database.
     * Empty [query] equals to all books.
     * Uses native fuzzy search to match against title and authors.
     */
    override suspend fun getBooks(query: String): List<Book> {
        Log.i(GET_BOOKS, "Searching for books with query: \"$query\".")

        val lowerQuery = query.lowercase()
        val threshold = maxOf(1, lowerQuery.length / 3)  // Dynamic threshold

        // Pre-filter with DB LIKE for speed (checks if title or any author contains any query char)
        val candidates = if (lowerQuery.isBlank()) {
            database.getAllBooks()
        } else {
            database.getAllBooks().filter { book ->
                val lowerTitle = book.title.lowercase()
                val lowerAuthors = book.authors.joinToString(" ").lowercase()
                lowerQuery.any { char -> char in lowerTitle || char in lowerAuthors }
            }
        }

        val filteredBooks = if (lowerQuery.isBlank()) {
            candidates
        } else {
            candidates.filter { bookEntity ->
                val lowerTitle = bookEntity.title.lowercase()
                val titleDist = minSubstringDistance(lowerQuery, lowerTitle, threshold)
                val authorDist = bookEntity.authors.minOfOrNull { author ->
                    minSubstringDistance(lowerQuery, author.lowercase(), threshold)
                } ?: Int.MAX_VALUE

                minOf(titleDist, authorDist) <= threshold
            }.sortedBy { bookEntity ->
                val lowerTitle = bookEntity.title.lowercase()
                val titleDist = minSubstringDistance(lowerQuery, lowerTitle, threshold)
                val authorDist = bookEntity.authors.minOfOrNull { author ->
                    minSubstringDistance(lowerQuery, author.lowercase(), threshold)
                } ?: Int.MAX_VALUE
                minOf(titleDist, authorDist)
            }
        }

        Log.i(GET_BOOKS, "Found ${filteredBooks.size} books (from ${candidates.size} candidates).")

        // Rest of the method (history lookup, mapping) remains unchanged
        val bookIds = filteredBooks.map { it.id }
        val histories = database.getLatestHistoryForBooks(bookIds)
        val historyMap: Map<Int, HistoryEntity?> = histories.groupBy { it.bookId }.mapValues { entry -> entry.value.firstOrNull() }

        return filteredBooks.map { entity ->
            bookMapper.toBook(entity).copy(lastOpened = historyMap[entity.id]?.time)
        }
    }

    /**
     * Get all books that match given [ids].
     */
    override suspend fun getBooksById(ids: List<Int>): List<Book> {
        Log.i(GET_BOOKS_BY_ID, "Getting books with ids: $ids.")
        val books = database.findBooksById(ids)

        // Batch history lookup instead of N+1 individual queries
        val histories = database.getLatestHistoryForBooks(books.map { it.id })
        val historyMap = histories.groupBy { it.bookId }.mapValues { it.value.firstOrNull() }

        return books.map { entity ->
            bookMapper.toBook(entity).copy(lastOpened = historyMap[entity.id]?.time)
        }
    }

    /**
     * Get speed reader words for a book.
     * Uses LRU cache for instant loading on subsequent opens.
     */
    override suspend fun getSpeedReaderWords(bookId: Int): List<SpeedReaderWord> {
        Log.d("SPEED_READER_WORDS", "[START] getSpeedReaderWords called - bookId=$bookId")

        if (bookId == -1) {
            Log.w("SPEED_READER_WORDS", "[START] Invalid bookId, returning empty list")
            return emptyList()
        }

        // Check cache first
        speedReaderWordCache.get(bookId)?.let { cachedWords ->
            Log.d("SPEED_READER_WORDS", "[CACHE HIT] Loaded words for [$bookId] from cache")
            Log.d("SPEED_READER_WORDS", "[CACHE HIT]   cachedWords.size=${cachedWords.size}")
            Log.d("SPEED_READER_WORDS", "[CACHE HIT]   Returning cached words immediately")
            return cachedWords
        }

        Log.d("SPEED_READER_WORDS", "[CACHE MISS] Words not in cache, extracting from text...")
        Log.d("SPEED_READER_WORDS", "[CACHE MISS] Calling getBookText($bookId)...")

        // Load text and extract words
        val readerText = getBookText(bookId)
        if (readerText.isEmpty()) {
            Log.e("SPEED_READER_WORDS", "[CACHE MISS] getBookText returned empty list!")
            return emptyList()
        }

        Log.d("SPEED_READER_WORDS", "[EXTRACTION] readerText.size=${readerText.size}, extracting words...")
        val words = SpeedReaderWordExtractor.extractWithPreprocessing(readerText)
        speedReaderWordCache.put(bookId, words)

        Log.d("SPEED_READER_WORDS", "[EXTRACTION] Extracted ${words.size} words for [$bookId]")
        Log.d("SPEED_READER_WORDS", "[EXTRACTION] Cached words in speedReaderWordCache")
        Log.d("SPEED_READER_WORDS", "[END] Returning ${words.size} words")

        return words
    }

    /**
     * Get a book by its file path.
     * Returns null if no book with that path exists.
     */
    override suspend fun getBookByFilePath(filePath: String): Book? {
        val entity = database.findBookByFilePath(filePath) ?: return null
        val book = bookMapper.toBook(entity)
        val lastHistory = database.getLatestHistoryForBook(book.id)
        return book.copy(lastOpened = lastHistory?.time)
    }

    /**
     * Loads text from the book. Already formatted.
     * Uses LRU cache for recently opened books to enable instant loading.
     */
    override suspend fun getBookText(bookId: Int): List<ReaderText> {
        Log.d("SPEED_READER_GET_TEXT", "[START] getBookText called - bookId=$bookId")

        if (bookId == -1) {
            Log.w("SPEED_READER_GET_TEXT", "[START] Invalid bookId, returning empty list")
            return emptyList()
        }

        // Check cache first for instant loading
        textCache.get(bookId)?.let { cachedText ->
            Log.d("SPEED_READER_GET_TEXT", "[TEXT CACHE HIT] Loaded text of [$bookId] from cache")
            Log.d("SPEED_READER_GET_TEXT", "[TEXT CACHE HIT]   cachedText.size=${cachedText.size}")
            return cachedText
        }

        Log.d("SPEED_READER_GET_TEXT", "[TEXT CACHE MISS] Text not in cache, parsing file...")
        val book = database.findBookById(bookId)
        Log.d("SPEED_READER_GET_TEXT", "[TEXT CACHE MISS]   book.title=${book?.title}")
        val cachedFile = getCachedFile(book)

        if (cachedFile == null || !cachedFile.canAccess()) {
            Log.e("SPEED_READER_GET_TEXT", "[TEXT CACHE MISS] File [$bookId] does not exist")
            return emptyList()
        }

        Log.d("SPEED_READER_GET_TEXT", "[PARSING] Calling textParser.parse()...")
        val readerText = textParser.parse(cachedFile)
        Log.d("SPEED_READER_GET_TEXT", "[PARSING]   readerText.size=${readerText.size}")

        if (
            readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
            readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
        ) {
            Log.e("SPEED_READER_GET_TEXT", "[PARSING] Could not load text from [$bookId].")
            return emptyList()
        }

        // Cache the parsed text for future use
        textCache.put(bookId, readerText)
        Log.d("SPEED_READER_GET_TEXT", "[CACHING] Cached text in textCache")

        // Extract and cache words for speed reader
        val wordsAlreadyCached = speedReaderWordCache.get(bookId)
        Log.d("SPEED_READER_GET_TEXT", "[WORD CACHE CHECK] wordsAlreadyCached=${wordsAlreadyCached != null}")

        if (wordsAlreadyCached == null) {
            Log.d("SPEED_READER_GET_TEXT", "[WORD EXTRACTION] Extracting words from readerText...")
            val words = SpeedReaderWordExtractor.extractWithPreprocessing(readerText)
            speedReaderWordCache.put(bookId, words)
            Log.d("SPEED_READER_GET_TEXT", "[WORD EXTRACTION]   Extracted ${words.size} words")
            Log.d("SPEED_READER_GET_TEXT", "[WORD EXTRACTION]   Cached in speedReaderWordCache")
        } else {
            Log.d("SPEED_READER_GET_TEXT", "[WORD CACHE] Words already cached, skipping extraction")
        }

        Log.i("SPEED_READER_GET_TEXT", "[END] Successfully loaded and cached text of [$bookId] with ${readerText.size} items")
        return readerText
    }

    /**
     * Inserts book in database.
     * Creates covers folder, which contains cover.
     * Restores progress from history if available.
     */
    override suspend fun insertBook(
        bookWithCover: BookWithCover
    ) {
        Log.i(INSERT_BOOK, "Inserting ${bookWithCover.book.title}.")

        val coverUri = bookWithCover.coverImage?.let { saveCover(it) }

        // Check for existing progress history
        val existingProgress = database.getBookProgressHistory(bookWithCover.book.filePath)
        val bookWithRestoredProgress = if (existingProgress != null) {
            Log.i(INSERT_BOOK, "Restoring progress from history for: ${bookWithCover.book.title}")
            bookWithCover.book.copy(
                scrollIndex = existingProgress.scrollIndex,
                scrollOffset = existingProgress.scrollOffset,
                progress = existingProgress.progress
            )
        } else {
            bookWithCover.book
        }

        val updatedBook = bookWithRestoredProgress.copy(
            coverImage = coverUri
        )

        val bookToInsert = bookMapper.toBookEntity(updatedBook)
        database.insertBook(bookToInsert)

        // Clean up progress history after successful restoration
        if (existingProgress != null) {
            database.deleteBookProgressHistory(bookWithCover.book.filePath)
            Log.i(INSERT_BOOK, "Cleaned up progress history after restoration.")
        }

        Log.i(INSERT_BOOK, "Successfully inserted book.")
    }

    /**
     * Update book without cover image.
     */
    override suspend fun updateBook(book: Book) {
        val entity = database.findBookById(book.id)
        database.updateBooks(
            listOf(
                bookMapper.toBookEntity(
                    book.copy(
                        coverImage = entity.image?.toUri()
                    )
                )
            )
        )
    }

    override suspend fun replaceCover(bookId: Int, cover: Bitmap): Uri? {
        val entity = database.findBookById(bookId)
        val newUri = saveCover(cover) ?: return null

        return try {
            database.updateCover(bookId, newUri.toString())
            entity.image?.let(::managedCoverFile)?.takeIf { it.toUri() != newUri }?.delete()
            newUri
        } catch (e: Exception) {
            managedCoverFile(newUri.toString())?.delete()
            Log.e(UPDATE_BOOK, "Could not replace cover for book $bookId", e)
            null
        }
    }

    private suspend fun saveCover(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val coversDir = File(application.filesDir, "covers")
        if (!coversDir.exists() && !coversDir.mkdirs()) return@withContext null

        val fileName = "${UUID.randomUUID()}.webp"
        val coverFile = File(coversDir, fileName)
        val temporaryFile = File(coversDir, "$fileName.tmp")
        val resized = coverExtractor.resizeForStorage(bitmap)

        try {
            BufferedOutputStream(FileOutputStream(temporaryFile)).use { output ->
                if (!resized.compress(Bitmap.CompressFormat.WEBP, 84, output)) {
                    throw IllegalStateException("Could not encode cover image")
                }
            }
            if (!temporaryFile.renameTo(coverFile)) {
                throw IllegalStateException("Could not finalize cover image")
            }
            coverFile.toUri()
        } catch (e: Exception) {
            temporaryFile.delete()
            coverFile.delete()
            Log.e(INSERT_BOOK, "Could not save cover", e)
            null
        } finally {
            if (resized !== bitmap) resized.recycle()
        }
    }

    private fun managedCoverFile(storedPath: String): File? {
        val coversDir = File(application.filesDir, "covers")
        val uri = storedPath.toUri()
        val candidate = when (uri.scheme) {
            "file" -> uri.path?.let(::File)
            null, "" -> File(coversDir, storedPath)
            else -> null
        } ?: return null

        return candidate.takeIf {
            runCatching {
                it.canonicalPath.startsWith(coversDir.canonicalPath + File.separator)
            }.getOrDefault(false)
        }
    }

    override suspend fun updateSpeedReaderProgress(bookId: Int, wordIndex: Int) {
        database.updateSpeedReaderProgress(bookId, wordIndex)
    }

    override suspend fun updateNormalReaderProgress(bookId: Int, scrollIndex: Int, scrollOffset: Int, progress: Float) {
        Log.d("NORMAL_READER_DB", "Updating normal reader progress: bookId=$bookId, scrollIndex=$scrollIndex, scrollOffset=$scrollOffset, progress=$progress")
        database.updateNormalReaderProgress(bookId, scrollIndex, scrollOffset, progress)
        val updatedBook = database.findBookById(bookId)
        Log.d("NORMAL_READER_DB", "After update, scrollIndex=${updatedBook?.scrollIndex}, scrollOffset=${updatedBook?.scrollOffset}, progress=${updatedBook?.progress}")
    }

    override suspend fun updateComicPdfProgress(bookId: Int, lastPageRead: Int, progress: Float) {
        Log.d("COMIC_PDF_PROGRESS", "Updating comic/PDF progress: bookId=$bookId, lastPageRead=$lastPageRead, progress=$progress")
        database.updateComicPdfProgress(bookId, lastPageRead, progress)
        val updatedBook = database.findBookById(bookId)
        Log.d("COMIC_PDF_PROGRESS", "After update, lastPageRead=${updatedBook?.lastPageRead}, progress=${updatedBook?.progress}")
    }

    override suspend fun markSpeedReaderOpened(bookId: Int) {
        database.markSpeedReaderOpened(bookId)
    }

    override suspend fun updateSpeedReaderTotalWords(bookId: Int, totalWords: Int) {
        Log.d("SPEED_READER_DB", "Updating speed reader total words: bookId=$bookId, totalWords=$totalWords")
        database.updateSpeedReaderTotalWords(bookId, totalWords)
    }

    override suspend fun deleteBooks(books: List<Book>) {
        Log.i(DELETE_BOOKS, "Deleting ${books.size} books")

        books.forEach { book ->
            val bookEntity = database.findBookById(book.id) ?: return@forEach

            database.deleteBookmarksByBookId(book.id)
            database.deleteBookProgressHistory(book.filePath)

            bookEntity.image?.let { imagePath ->
                val coverFile = managedCoverFile(imagePath)
                if (coverFile?.exists() == true) {
                    coverFile.delete()
                    Log.i(DELETE_BOOKS, "Deleted cover image: $imagePath")
                }
            }

            database.deleteBooks(listOf(bookEntity))
            Log.i(DELETE_BOOKS, "Deleted book: ${book.title}")
        }

        Log.i(DELETE_BOOKS, "Successfully deleted ${books.size} books")
    }





    /**
     * Delete progress history for a specific book.
     */
    override suspend fun deleteProgressHistory(book: Book) {
        Log.i("DELETE_HISTORY", "Deleting progress history for: ${book.title}")
        database.deleteBookProgressHistory(book.filePath)
        Log.i("DELETE_HISTORY", "Successfully deleted progress history.")
    }

    override suspend fun getAllAuthors(): List<String> = withContext(Dispatchers.IO) {
        getAllMetadata().authors
    }

    override suspend fun getAllSeries(): List<String> = withContext(Dispatchers.IO) {
        getAllMetadata().series
    }

    override suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        getAllMetadata().tags
    }

    override suspend fun getAllLanguages(): List<String> = withContext(Dispatchers.IO) {
        getAllMetadata().languages
    }

    override suspend fun getAllMetadata(): BookRepository.LibraryMetadata = withContext(Dispatchers.IO) {
        val allBooks = database.getAllBooks()
        val authors = mutableSetOf<String>()
        val series = mutableSetOf<String>()
        val tags = mutableSetOf<String>()
        val languages = mutableSetOf<String>()
        var hasUnknownAuthors = false

        allBooks.forEach { book ->
            authors.addAll(book.authors.filter { it.isNotBlank() })
            series.addAll(book.series.filter { it.isNotBlank() })
            tags.addAll(book.tags)
            languages.addAll(book.languages.filter { it.isNotBlank() })
            if (book.authors.isEmpty()) hasUnknownAuthors = true
        }

        BookRepository.LibraryMetadata(
            authors = if (hasUnknownAuthors) listOf("Unknown") + authors.sorted() else authors.sorted(),
            series = series.sorted(),
            tags = tags.sorted(),
            languages = languages.sorted()
        )
    }

    override suspend fun getPublicationYearRange(): Pair<Int, Int> {
        val yearRange = database.getPublicationYearRange()
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        val minYear = yearRange?.minYear?.let {
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).year
        } ?: 1900

        val maxYear = yearRange?.maxYear?.let {
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).year
        } ?: currentYear

        return Pair(minYear, maxYear)
    }

    /**
     * Clean up old progress history entries to maintain lightweight storage.
     * Removes entries older than 90 days.
     */
    suspend fun cleanupOldProgressHistory() {
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000) // 90 days in milliseconds
        val deletedCount = database.deleteOldProgressHistory(ninetyDaysAgo)
        if (deletedCount > 0) {
            Log.i("CLEANUP", "Cleaned up $deletedCount old progress history entries.")
        }
    }

    /**
     * Get all books downloaded from a specific OPDS source by URL.
     * Used for metadata refresh operations.
     */
    override suspend fun getBooksByOpdsSourceUrl(opdsSourceUrl: String): List<Book> =
        withContext(Dispatchers.IO) {
            database.findBooksByOpdsSourceUrl(opdsSourceUrl)
                .map { bookEntity -> bookMapper.toBook(bookEntity) }
        }

    /**
     * Get all books downloaded from a specific OPDS source by ID.
     * Used for metadata refresh operations and source management.
     */
    override suspend fun getBooksByOpdsSourceId(opdsSourceId: Int): List<Book> =
        withContext(Dispatchers.IO) {
            database.findBooksByOpdsSourceId(opdsSourceId)
                .map { bookEntity -> bookMapper.toBook(bookEntity) }
        }

    override suspend fun getBookByCalibreId(calibreId: String): Book? {
        val entity = database.findBookByCalibreId(calibreId) ?: return null
        val book = bookMapper.toBook(entity)
        val lastHistory = database.getLatestHistoryForBook(book.id)
        return book.copy(lastOpened = lastHistory?.time)
    }

    override suspend fun getBookByContentHash(contentHash: String): Book? {
        if (contentHash.isBlank()) return null
        val entity = database.findBookByContentHash(contentHash) ?: return null
        val book = bookMapper.toBook(entity)
        val lastHistory = database.getLatestHistoryForBook(book.id)
        return book.copy(lastOpened = lastHistory?.time)
    }

    override suspend fun findExistingBook(
        filePath: String,
        fileName: String?,
        fileSize: Long?
    ): Book? {
        val pathLookupKeys = buildList {
            add(filePath)
            add(filePath.removePrefix("file://"))
            if (!filePath.startsWith("file://")) add("file://$filePath")
            add(Uri.decode(filePath))
            add(Uri.decode(filePath).removePrefix("file://"))
        }.distinct()

        val existingByPath = database.findBooksByFilePaths(pathLookupKeys).firstOrNull()
        if (existingByPath != null) {
            val book = bookMapper.toBook(existingByPath)
            val lastHistory = database.getLatestHistoryForBook(book.id)
            return book.copy(lastOpened = lastHistory?.time)
        }

        return null
    }

    override suspend fun getAllFilePathsAndHashes(): List<Pair<String, String>> {
        return database.getAllFilePathsAndHashes().map { it.filePath to it.contentHash }
    }
}
