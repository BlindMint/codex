/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.mapper.opds.OpdsMetadataMapper
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.domain.repository.BookRepository
import javax.inject.Inject

/**
 * Repository for handling OPDS metadata refresh operations.
 * Implements non-destructive merge logic to preserve user edits while adding new metadata.
 */
class OpdsRefreshRepository @Inject constructor(
    private val bookRepository: BookRepository,
    private val opdsMetadataMapper: OpdsMetadataMapper
) {

    /**
     * Non-destructively merge OPDS metadata into a book.
     * User-added metadata is preserved, and OPDS metadata is merged via union logic.
     */
    suspend fun mergeOpdsMetadata(userBook: Book, opdsEntry: OpdsEntry): Book =
        withContext(Dispatchers.Default) {
            userBook.copy(
                // Overwrite these fields with latest from OPDS
                title = opdsEntry.title,
                description = opdsEntry.summary ?: userBook.description,
                publisher = opdsEntry.publisher ?: userBook.publisher,
                publicationDate = opdsEntry.published?.let { parseIsoDate(it) }
                    ?: userBook.publicationDate,

                // Union these lists: keep existing + add from OPDS if not already present
                authors = (userBook.authors + listOfNotNull(opdsEntry.author))
                    .distinct()
                    .filter { it.isNotBlank() },

                tags = (userBook.tags + opdsEntry.categories)
                    .distinct()
                    .filter { it.isNotBlank() },

                languages = (userBook.languages + listOfNotNull(opdsEntry.language))
                    .distinct()
                    .filter { it.isNotBlank() },

                // For series, add if not already present
                series = if (opdsEntry.series != null && opdsEntry.series !in userBook.series)
                    userBook.series + opdsEntry.series
                else
                    userBook.series,

                // Update metadata refresh timestamp
                metadataLastRefreshTime = System.currentTimeMillis(),

                // Update identifiers if they were missing
                uuid = userBook.uuid.takeIf { !it.isNullOrBlank() }
                    ?: opdsEntry.identifiers.firstOrNull { it.startsWith("urn:uuid:") }
                        ?.removePrefix("urn:uuid:"),
                isbn = userBook.isbn.takeIf { !it.isNullOrBlank() }
                    ?: opdsEntry.identifiers.firstOrNull { it.startsWith("urn:isbn:") }
                        ?.removePrefix("urn:isbn:")
            )
        }

    /**
     * Refresh a single book's metadata from OPDS source.
     * Returns the updated book if successful, null if OPDS lookup fails.
     */
    suspend fun refreshBookMetadata(
        book: Book,
        opdsEntryFinder: suspend (String?, String?) -> OpdsEntry?
    ): Book? = withContext(Dispatchers.IO) {
        try {
            // Find matching OPDS entry by UUID or ISBN
            val opdsEntry = opdsEntryFinder(book.uuid, book.isbn)

            if (opdsEntry != null) {
                val mergedBook = mergeOpdsMetadata(book, opdsEntry)
                bookRepository.updateBook(mergedBook)
                Log.i(
                    "OpdsRefreshRepository",
                    "Successfully refreshed metadata for book: ${book.title}"
                )
                mergedBook
            } else {
                Log.w(
                    "OpdsRefreshRepository",
                    "Could not find matching OPDS entry for book: ${book.title}"
                )
                null
            }
        } catch (e: Exception) {
            Log.e("OpdsRefreshRepository", "Error refreshing book metadata", e)
            null
        }
    }

    /**
     * Refresh all books from a specific OPDS source.
     * Returns count of successfully refreshed books.
     */
    suspend fun refreshAllBooksFromSource(
        opdsSourceId: Int,
        opdsEntryFinder: suspend (String?, String?) -> OpdsEntry?
    ): Int = withContext(Dispatchers.IO) {
        var refreshedCount = 0
        try {
            val booksFromSource = bookRepository.getBooksByOpdsSourceId(opdsSourceId)
            Log.i(
                "OpdsRefreshRepository",
                "Starting refresh for ${booksFromSource.size} books from OPDS source: $opdsSourceId"
            )

            booksFromSource.forEach { book ->
                val result = refreshBookMetadata(book, opdsEntryFinder)
                if (result != null) {
                    refreshedCount++
                }
            }

            Log.i(
                "OpdsRefreshRepository",
                "Completed refresh: $refreshedCount/${booksFromSource.size} books updated"
            )
        } catch (e: Exception) {
            Log.e("OpdsRefreshRepository", "Error during bulk refresh from OPDS source", e)
        }

        refreshedCount
    }

    /**
     * Parses ISO 8601 date strings to milliseconds since epoch.
     * Used for converting OPDS publication dates.
     */
    private fun parseIsoDate(dateString: String): Long? {
        return try {
            val instant = if (dateString.contains("T")) {
                java.time.Instant.parse(dateString)
            } else {
                // Parse date-only format (YYYY-MM-DD)
                val date = java.time.LocalDate.parse(dateString)
                date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            }
            instant.toEpochMilli()
        } catch (e: Exception) {
            Log.w("OpdsRefreshRepository", "Could not parse date: $dateString", e)
            null
        }
    }
}
