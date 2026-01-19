/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.opds

import us.blindmint.codex.data.repository.OpdsRefreshRepository
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.opds.OpdsEntry
import javax.inject.Inject

/**
 * Refreshes a single book's metadata from its OPDS source.
 * Implements non-destructive merge to preserve user edits.
 */
class RefreshBookMetadataFromOpds @Inject constructor(
    private val opdsRefreshRepository: OpdsRefreshRepository
) {
    suspend fun execute(
        book: Book,
        opdsEntryFinder: suspend (String?, String?) -> OpdsEntry?
    ): Book? {
        return opdsRefreshRepository.refreshBookMetadata(book, opdsEntryFinder)
    }
}
