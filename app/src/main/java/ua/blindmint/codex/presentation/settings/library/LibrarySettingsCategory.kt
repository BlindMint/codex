/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package ua.blindmint.codex.presentation.settings.library

import androidx.compose.foundation.lazy.LazyListScope
import ua.blindmint.codex.presentation.settings.library.display.LibraryDisplaySubcategory
import ua.blindmint.codex.presentation.settings.library.sort.LibrarySortSubcategory
import ua.blindmint.codex.presentation.settings.library.tabs.LibraryTabsSubcategory

fun LazyListScope.LibrarySettingsCategory() {
    LibraryDisplaySubcategory()
    LibraryTabsSubcategory()
    LibrarySortSubcategory(
        showDivider = false
    )
}