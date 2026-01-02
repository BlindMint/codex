/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.bookmark

import us.blindmint.codex.domain.repository.BookmarkRepository
import javax.inject.Inject

class DeleteBookmarksByBookId @Inject constructor(
    private val repository: BookmarkRepository
) {

    suspend fun execute(bookId: Int) {
        repository.deleteBookmarksByBookId(bookId)
    }
}
