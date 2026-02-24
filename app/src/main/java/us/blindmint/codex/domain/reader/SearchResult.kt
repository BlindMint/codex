/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.reader

import androidx.compose.runtime.Immutable

@Immutable
data class SearchResult(
    val textIndex: Int,
    val fullText: String,
    val matchedText: String,
    val beforeContext: String = "",
    val afterContext: String = "",
    val pageNumber: Int? = null
)
