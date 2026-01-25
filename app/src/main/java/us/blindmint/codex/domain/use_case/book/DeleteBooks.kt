/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.repository.BookRepository
import javax.inject.Inject

class DeleteBooks @Inject constructor(
    private val bookRepository: BookRepository
) {

    suspend fun execute(books: List<Book>) {
        bookRepository.deleteBooks(books)
    }
}