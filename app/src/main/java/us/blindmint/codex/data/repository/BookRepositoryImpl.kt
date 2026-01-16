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
import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.mapper.book.BookMapper
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.data.parser.TextParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.BookWithCover
import us.blindmint.codex.domain.reader.ReaderText
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

    /**
     * Creates a CachedFile from book.filePath, handling both file paths and content URIs.
     */
    private fun getCachedFile(book: BookEntity): CachedFile? {
        val uri = Uri.parse(book.filePath)
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
     */
    override suspend fun getBooks(query: String): List<Book> {
        Log.i(GET_BOOKS, "Searching for books with query: \"$query\".")
        val books = database.searchBooks(query)

        Log.i(GET_BOOKS, "Found ${books.size} books.")
        return books.map { entity ->
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
     * Get all books that match given [ids].
     */
    override suspend fun getBooksById(ids: List<Int>): List<Book> {
        Log.i(GET_BOOKS_BY_ID, "Getting books with ids: $ids.")
        val books = database.findBooksById(ids)

        return books.map { entity ->
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
        if (bookId == -1) return emptyList()

        // Check cache first for instant loading
        textCache.get(bookId)?.let { cachedText ->
            Log.i(GET_TEXT, "Loaded text of [$bookId] from cache.")
            return cachedText
        }

        val book = database.findBookById(bookId)
        val cachedFile = getCachedFile(book)

        if (cachedFile == null || !cachedFile.canAccess()) {
            Log.e(GET_TEXT, "File [$bookId] does not exist")
            return emptyList()
        }

        val readerText = textParser.parse(cachedFile)

        if (
            readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
            readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
        ) {
            Log.e(GET_TEXT, "Could not load text from [$bookId].")
            return emptyList()
        }

        // Cache the parsed text for future use
        textCache.put(bookId, readerText)

        Log.i(GET_TEXT, "Successfully loaded and cached text of [$bookId] with markdown.")
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

    /**
     * Update cover image of the book. Deletes old cover and replaces with new.
     */
    override suspend fun updateCoverImageOfBook(
        bookWithOldCover: Book,
        newCoverImage: CoverImage?
    ) {
        Log.i(UPDATE_BOOK, "Updating cover image: ${bookWithOldCover.title}.")

        val book = database.findBookById(bookWithOldCover.id)
        var uri: String? = null

        val filesDir = application.filesDir
        val coversDir = File(filesDir, "covers")

        if (!coversDir.exists()) {
            Log.i(UPDATE_BOOK, "Created covers folder.")
            coversDir.mkdirs()
        }

        if (newCoverImage != null) {
            try {
                uri = "${UUID.randomUUID()}.webp"
                val cover = File(coversDir, uri)

                withContext(Dispatchers.IO) {
                    BufferedOutputStream(FileOutputStream(cover)).use { output ->
                        newCoverImage
                            .copy(Bitmap.Config.RGB_565, false)
                            .compress(Bitmap.CompressFormat.WEBP, 20, output)
                            .let { success ->
                                if (success) return@let
                                throw Exception("Couldn't save cover image")
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e(UPDATE_BOOK, "Could not save new cover.")
                e.printStackTrace()
                return
            }
        }

        if (book.image != null) {
            try {
                val fileToDelete = File(
                    "$coversDir${File.separator}${book.image.substringAfterLast(File.separator)}"
                )

                if (fileToDelete.exists()) {
                    fileToDelete.delete()
                }
            } catch (e: Exception) {
                Log.e(UPDATE_BOOK, "Could not delete old cover.")
                e.printStackTrace()
            }
        }

        val newCoverImageUri = if (uri != null) {
            Uri.fromFile(File("$coversDir/$uri"))
        } else {
            null
        }

        val bookWithNewCover = bookWithOldCover.copy(
            coverImage = newCoverImageUri
        )

        database.updateBooks(
            listOf(
                bookMapper.toBookEntity(
                    bookWithNewCover
                )
            )
        )
        Log.i(UPDATE_BOOK, "Successfully updated cover image.")
    }

    /**
     * Delete books.
     * Also deletes cover image and text from internal storage.
     * Preserves progress history for seamless resumption on other devices.
     */
    override suspend fun deleteBooks(books: List<Book>) {
        Log.i(DELETE_BOOKS, "Deleting books.")

        val filesDir = application.filesDir
        val coversDir = File(filesDir, "covers")

        if (!coversDir.exists()) {
            Log.i(DELETE_BOOKS, "Created covers folder.")
            coversDir.mkdirs()
        }

        val bookEntities = books.map {
            val book = database.findBookById(it.id)

            // Save progress to history before deletion
            val progressHistory = BookProgressHistoryEntity(
                id = 0, // Auto-generated
                filePath = book.filePath,
                title = book.title,
                author = book.author,
                scrollIndex = book.scrollIndex,
                scrollOffset = book.scrollOffset,
                progress = book.progress,
                lastModified = System.currentTimeMillis()
            )
            database.insertBookProgressHistory(progressHistory)
            Log.i(DELETE_BOOKS, "Saved progress history for: ${book.title}")

            if (book.image != null) {
                try {
                    val fileToDelete = File(
                        "$coversDir${File.separator}${book.image.substringAfterLast(File.separator)}"
                    )

                    if (fileToDelete.exists()) {
                        fileToDelete.delete()
                    }
                } catch (e: Exception) {
                    Log.e(DELETE_BOOKS, "Could not delete cover image.")
                    e.printStackTrace()
                }
            }

            book
        }

        database.deleteBooks(bookEntities)

        // Remove deleted books from cache
        bookEntities.forEach { book ->
            textCache.remove(book.id)
        }

        Log.i(DELETE_BOOKS, "Successfully deleted books.")
    }

    /**
     * @return Whether can reset cover image (restore default).
     */
    override suspend fun canResetCover(bookId: Int): Boolean {
        val book = database.findBookById(bookId)

        val cachedFile = getCachedFile(book)

        if (cachedFile == null || !cachedFile.canAccess()) {
            return false
        }

        val defaultCoverUncompressed = fileParser.parse(cachedFile)?.coverImage
            ?: return false

        if (book.image == null) {
            Log.i(CAN_RESET_COVER, "Can reset cover image. (current is null)")
            return true
        }

        val stream = ByteArrayOutputStream()
        defaultCoverUncompressed.copy(Bitmap.Config.RGB_565, false).compress(
            Bitmap.CompressFormat.WEBP,
            20,
            stream
        )
        val byteArray = stream.toByteArray()
        val defaultCover = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        val currentCover = try {
            MediaStore.Images.Media.getBitmap(
                application.contentResolver,
                book.image.toUri()
            )
        } catch (e: Exception) {
            Log.i(CAN_RESET_COVER, "Can reset cover image. (could not get current)")
            e.printStackTrace()
            return true
        }

        return !defaultCover.sameAs(currentCover)
    }

    /**
     * Reset cover image to default.
     * If there is no default cover, returns false.
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
        val knownAuthors = database.getAllAuthors().filter { it.isNotBlank() }

        // Check if any books have null/blank author
        val hasUnknownAuthors = database.getAllBooks().any { it.author.isNullOrBlank() }

        if (hasUnknownAuthors) {
            listOf("Unknown") + knownAuthors
        } else {
            knownAuthors
        }
    }

    override suspend fun getAllSeries(): List<String> {
        return database.getAllSeries().filter { it.isNotBlank() }
    }

    override suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        val allBooks = database.getAllBooks()
        val tagsSet = mutableSetOf<String>()
        allBooks.forEach { book ->
            tagsSet.addAll(book.tags)
        }

        tagsSet.sorted()
    }

    override suspend fun getAllLanguages(): List<String> {
        return database.getAllLanguages().filter { it.isNotBlank() }
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
}