/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import us.blindmint.codex.presentation.settings.browse.components.StorageLocationPicker
import us.blindmint.codex.presentation.settings.browse.opds.BrowseOpdsSubcategory
import us.blindmint.codex.presentation.settings.browse.scan.BrowseScanSubcategory
import us.blindmint.codex.ui.browse.OpdsRootScreen

fun LazyListScope.BrowseSettingsCategory(
    onNavigateToOpdsCatalog: (OpdsRootScreen) -> Unit
) {
    item {
        StorageLocationPicker()
    }
    item {
        HorizontalDivider()
    }
    BrowseScanSubcategory()
    BrowseOpdsSubcategory(
        onNavigateToOpdsCatalog = onNavigateToOpdsCatalog,
        showDivider = false
    )
}