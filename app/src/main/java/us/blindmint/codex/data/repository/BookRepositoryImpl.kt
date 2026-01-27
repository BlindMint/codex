/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.local.dto.BookEntity
import us.blindmint.codex.data.local.dto.BookProgressHistoryEntity
import us.blindmint.codex.data.local.dto.HistoryEntity
import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.mapper.book.BookMapper
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.data.parser.SpeedReaderWordExtractor
import us.blindmint.codex.data.parser.TextParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import me.xdrop.fuzzywuzzy.FuzzySearch
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.util.CoverImage
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
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
private const val CAN_RESET_COVER = "CAN RESET COVER, REPO"
private const val RESET_COVER = "RESET COVER, REPO"

@Suppress("DEPRECATION")
@Singleton
class BookRepositoryImpl @Inject constructor(
    private val application: Application,
    private val database: BookDao,
    private val fileParser: FileParser,
    private val textParser: TextParser,
    private val bookMapper: BookMapper
) : BookRepository {

    // LRU cache for recently opened book text (100MB cache size for 3-5 books)
    private val textCache = LruCache<Int, List<ReaderText>>(1024 * 1024 * 100)

    // LRU cache for speed reader words (50MB cache size for 10-15 books)
    private val speedReaderWordCache = LruCache<Int, List<SpeedReaderWord>>(1024 * 1024 * 50)

    /**
     * Creates a CachedFile from book.filePath, handling both file paths and content URIs.
     */
    private fun getCachedFile(book: BookEntity): CachedFile? {
        val uri = book.filePath.toUri()
        return if (!uri.scheme.isNullOrBlank()) {
            // It's a URI (content:// or file://)
            val name = if (uri.scheme == "content") {
                uri.lastPathSegment?.let { Uri.decode(it) } ?: "unknown"
            } else {
                uri.lastPathSegment ?: book.filePath.substringAfterLast(File.separator)
            }
            CachedFileCompat.fromUri(
                context = application,
                uri = uri,
                builder = CachedFileCompat.build(
                    name = name,
                    path = book.filePath,
                    isDirectory = false
                )
            )
        } else {
            // It's a file path
            CachedFileCompat.fromFullPath(
                context = application,
                path = book.filePath,
                builder = CachedFileCompat.build(
                    name = book.filePath.substringAfterLast(File.separator),
                    path = book.filePath,
                    isDirectory = false
                )
            )
        }
    }

    /**
     * Get all books matching [query] from database.
     * Empty [query] equals to all books.
     * Uses fuzzy search to match against title and authors.
     */
    override suspend fun getBooks(query: String): List<Book> {
        Log.i(GET_BOOKS, "Searching for books with query: \"$query\".")
        
        val allBooks = database.getAllBooks()
        
        val filteredBooks = if (query.isBlank()) {
            allBooks
        } else {
            allBooks.filter { bookEntity ->
                // Fuzzy match on title
                val titleMatch = FuzzySearch.partialRatio(query.lowercase(), bookEntity.title.lowercase()) > 60
                // Fuzzy match on authors
                val authorMatch = bookEntity.authors.any { author ->
                    FuzzySearch.partialRatio(query.lowercase(), author.lowercase()) > 60
                }
                titleMatch || authorMatch
            }
        }

        Log.i(GET_BOOKS, "Found ${filteredBooks.size} books.")

        val bookIds = filteredBooks.map { it.id }
        val histories = database.getLatestHistoryForBooks(bookIds)
        val historyMap: Map<Int, HistoryEntity?> = histories.groupBy { it.bookId }.mapValues { entry -> entry.value.firstOrNull() }

        return filteredBooks.map { entity ->
            val book = bookMapper.toBook(entity)
            val lastHistory = historyMap[entity.id]

            book.copy(
                lastOpened = lastHistory?.time
            )
        }
    }

    /**
     * Get all books that match given [ids].
     */
    override suspend fun getBooksById(ids: List<Int>): List<Book> {
        Log.i(GET_BOOKS_BY_ID, "Getting books with ids: $ids.")
        val books = database.findBooksById(ids)

        return books.map { entity ->
            Log.d("SPEED_READER_DB", "Loading book ${entity.id}, speedReaderWordIndex=${entity.speedReaderWordIndex}")
            val book = bookMapper.toBook(entity)
            val lastHistory = database.getLatestHistoryForBook(
                book.id
            )

            book.copy(
                lastOpened = lastHistory?.time
            )
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

        val filesDir = application.filesDir
        val coversDir = File(filesDir, "covers")

        if (!coversDir.exists()) {
            Log.i(INSERT_BOOK, "Created covers folder.")
            coversDir.mkdirs()
        }

        var coverUri = ""

        if (bookWithCover.coverImage != null) {
            try {
                coverUri = "${UUID.randomUUID()}.webp"
                val cover = File(coversDir, coverUri)

                withContext(Dispatchers.IO) {
                    BufferedOutputStream(FileOutputStream(cover)).use { output ->
                        bookWithCover.coverImage
                            .copy(Bitmap.Config.RGB_565, false)
                            .compress(Bitmap.CompressFormat.WEBP, 20, output)
                            .let { success ->
                                if (success) return@let
                                throw Exception("Couldn't save cover image")
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e(INSERT_BOOK, "Could not save cover.")
                coverUri = ""
                e.printStackTrace()
            }
        }

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
            coverImage = if (coverUri.isNotBlank()) {
                Uri.fromFile(File("$coversDir/$coverUri"))
            } else null
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

    override suspend fun updateCoverImageOfBook(bookWithOldCover: Book, newCoverImage: CoverImage?) {
        // Implementation needed
        TODO("Implement updateCoverImageOfBook")
    }

    override suspend fun updateSpeedReaderProgress(bookId: Int, wordIndex: Int) {
        Log.d("SPEED_READER_DB", "Updating speed reader progress: bookId=$bookId, wordIndex=$wordIndex")
        database.updateSpeedReaderProgress(bookId, wordIndex)
        // Verify the update worked
        val updatedBook = database.findBookById(bookId)
        Log.d("SPEED_READER_DB", "After update, speedReaderWordIndex=${updatedBook?.speedReaderWordIndex}")
    }

    override suspend fun markSpeedReaderOpened(bookId: Int) {
        database.markSpeedReaderOpened(bookId)
    }

    override suspend fun updateSpeedReaderTotalWords(bookId: Int, totalWords: Int) {
        Log.d("SPEED_READER_DB", "Updating speed reader total words: bookId=$bookId, totalWords=$totalWords")
        database.updateSpeedReaderTotalWords(bookId, totalWords)
    }

    override suspend fun canResetCover(bookId: Int): Boolean {
        // Implementation
        val book = database.findBookById(bookId)
        val cachedFile = getCachedFile(book)
        return cachedFile != null && cachedFile.canAccess()
    }

    override suspend fun deleteBooks(books: List<Book>) {
        Log.i(DELETE_BOOKS, "Deleting ${books.size} books")

        books.forEach { book ->
            val bookEntity = database.findBookById(book.id) ?: return@forEach

            database.deleteBookmarksByBookId(book.id)
            database.deleteBookProgressHistory(book.filePath)

            bookEntity.image?.let { imagePath ->
                val coverFile = File(application.filesDir, "covers/$imagePath")
                if (coverFile.exists()) {
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
     * Reset cover image to the default one from the book file.
     */
    override suspend fun resetCoverImage(bookId: Int): Boolean {
        if (!canResetCover(bookId)) {
            Log.w(RESET_COVER, "Cannot reset cover image.")
            return false
        }

        val book = database.findBookById(bookId)
        val cachedFile = getCachedFile(book)

        if (cachedFile == null || !cachedFile.canAccess()) {
            return false
        }

        val defaultCover = fileParser.parse(cachedFile)?.coverImage
            ?: return false
        updateCoverImageOfBook(bookMapper.toBook(book), defaultCover)

        Log.i(RESET_COVER, "Successfully reset cover image.")
        return true
    }

    /**
     * Delete progress history for a specific book.
     */
    override suspend fun deleteProgressHistory(book: Book) {
        Log.i("DELETE_HISTORY", "Deleting progress history for: ${book.title}")
        database.deleteBookProgressHistory(book.filePath)
        Log.i("DELETE_HISTORY", "Successfully deleted progress history.")
    }

    /**
     * Preload text for recently opened books into cache for instant loading.
     * Caches up to 3 most recent books to balance performance and memory usage.
     */
    override suspend fun preloadRecentBooksText() {
        // Implementation here
    }

    override suspend fun getAllAuthors(): List<String> = withContext(Dispatchers.IO) {
        val allBooks = database.getAllBooks()
        val authorsSet = mutableSetOf<String>()
        allBooks.forEach { book ->
            authorsSet.addAll(book.authors.filter { it.isNotBlank() })
        }

        // Check if any books have no authors
        val hasUnknownAuthors = allBooks.any { it.authors.isEmpty() }

        val sortedAuthors = authorsSet.sorted()
        if (hasUnknownAuthors) {
            listOf("Unknown") + sortedAuthors
        } else {
            sortedAuthors
        }
    }

    override suspend fun getAllSeries(): List<String> = withContext(Dispatchers.IO) {
        val allBooks = database.getAllBooks()
        val seriesSet = mutableSetOf<String>()
        allBooks.forEach { book ->
            seriesSet.addAll(book.series.filter { it.isNotBlank() })
        }

        seriesSet.sorted()
    }

    override suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        val allBooks = database.getAllBooks()
        val tagsSet = mutableSetOf<String>()
        allBooks.forEach { book ->
            tagsSet.addAll(book.tags)
        }

        tagsSet.sorted()
    }

    override suspend fun getAllLanguages(): List<String> = withContext(Dispatchers.IO) {
        val allBooks = database.getAllBooks()
        val languagesSet = mutableSetOf<String>()
        allBooks.forEach { book ->
            languagesSet.addAll(book.languages.filter { it.isNotBlank() })
        }

        languagesSet.sorted()
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
            database.getAllBooks()
                .filter { it.opdsSourceUrl == opdsSourceUrl }
                .map { bookEntity -> bookMapper.toBook(bookEntity) }
        }

    /**
     * Get all books downloaded from a specific OPDS source by ID.
     * Used for metadata refresh operations and source management.
     */
    override suspend fun getBooksByOpdsSourceId(opdsSourceId: Int): List<Book> =
        withContext(Dispatchers.IO) {
            database.getAllBooks()
                .filter { it.opdsSourceId == opdsSourceId }
                .map { bookEntity -> bookMapper.toBook(bookEntity) }
        }

    /**
     * Get a book by its Calibre ID.
     * Returns null if no book with that Calibre ID exists.
     */
    override suspend fun getBookByCalibreId(calibreId: String): Book? {
        val entity = database.findBookByCalibreId(calibreId) ?: return null
        val book = bookMapper.toBook(entity)
        val lastHistory = database.getLatestHistoryForBook(book.id)
        return book.copy(lastOpened = lastHistory?.time)
    }
}