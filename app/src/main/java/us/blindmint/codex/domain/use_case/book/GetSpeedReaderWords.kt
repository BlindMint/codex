/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.use_case.book

import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.domain.repository.BookRepository
import javax.inject.Inject

class GetSpeedReaderWords @Inject constructor(
    private val repository: BookRepository
) {

    suspend fun execute(bookId: Int): List<SpeedReaderWord> {
        return repository.getSpeedReaderWords(bookId)
    }
}
