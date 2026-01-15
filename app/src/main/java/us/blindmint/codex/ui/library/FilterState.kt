/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.library

import androidx.compose.runtime.Immutable

@Immutable
data class FilterState(
    val selectedStatuses: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val selectedAuthors: Set<String> = emptySet(),
    val selectedSeries: Set<String> = emptySet(),
    val publicationYearRange: ClosedRange<Int> = 1900..2026,
    val selectedLanguages: Set<String> = emptySet()
)