/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse

import androidx.compose.foundation.lazy.LazyListScope
import us.blindmint.codex.presentation.settings.browse.display.BrowseDisplaySubcategory
import us.blindmint.codex.presentation.settings.browse.filter.BrowseFilterSubcategory
import us.blindmint.codex.presentation.settings.browse.scan.BrowseScanSubcategory
import us.blindmint.codex.presentation.settings.browse.sort.BrowseSortSubcategory

fun LazyListScope.BrowseSettingsCategory() {
    BrowseScanSubcategory()
    BrowseDisplaySubcategory()
    BrowseFilterSubcategory()
    BrowseSortSubcategory(
        showDivider = false
    )
}