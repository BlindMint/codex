/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.util.BookFileAvailability
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.repository.BookRepository
import javax.inject.Inject

class RemoveMissingBooks @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookFileAvailability: BookFileAvailability
) {
    suspend fun count(): Int = withContext(Dispatchers.IO) {
        findMissingBooks().size
    }

    suspend fun execute(): Int = withContext(Dispatchers.IO) {
        // Recheck immediately before deletion in case storage became available
        // while the confirmation dialog was open.
        val missingBooks = findMissingBooks()
        if (missingBooks.isNotEmpty()) {
            bookRepository.deleteBooks(missingBooks)
        }
        missingBooks.size
    }

    private suspend fun findMissingBooks(): List<Book> =
        bookRepository.getBooks("").filterNot(bookFileAvailability::isAvailable)
}
