/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.opds

import us.blindmint.codex.data.repository.OpdsRefreshRepository
import us.blindmint.codex.domain.opds.OpdsEntry
import javax.inject.Inject

/**
 * Refreshes metadata for all books from a specific OPDS source.
 * Implements non-destructive merge to preserve user edits.
 * Returns count of successfully refreshed books.
 */
class RefreshAllBooksFromOpdsSource @Inject constructor(
    private val opdsRefreshRepository: OpdsRefreshRepository
) {
    suspend fun execute(
        opdsSourceId: Int,
        opdsEntryFinder: suspend (String?, String?) -> OpdsEntry?
    ): Int {
        return opdsRefreshRepository.refreshAllBooksFromSource(opdsSourceId, opdsEntryFinder)
    }
}
