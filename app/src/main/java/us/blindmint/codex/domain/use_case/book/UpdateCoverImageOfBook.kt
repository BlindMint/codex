/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.util.CoverImage
import javax.inject.Inject

class UpdateCoverImageOfBook @Inject constructor(
    private val repository: BookRepository
) {

    suspend fun execute(bookWithOldCover: Book, newCoverImage: CoverImage?) {
        repository.updateCoverImageOfBook(bookWithOldCover, newCoverImage)
    }
}