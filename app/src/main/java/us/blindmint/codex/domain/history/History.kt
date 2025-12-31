/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.history

import androidx.compose.runtime.Immutable
import us.blindmint.codex.domain.library.book.Book

@Immutable
data class History(
    val id: Int = 0,
    val bookId: Int,
    val book: Book?,
    val time: Long
)