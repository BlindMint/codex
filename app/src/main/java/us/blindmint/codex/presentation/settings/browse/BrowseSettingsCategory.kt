/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.browse.components.StorageLocationPicker
import us.blindmint.codex.presentation.settings.browse.opds.BrowseOpdsSubcategory
import us.blindmint.codex.presentation.settings.browse.scan.BrowseScanSubcategory
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.ui.browse.OpdsRootScreen

fun LazyListScope.BrowseSettingsCategory(
    onNavigateToOpdsCatalog: (OpdsRootScreen) -> Unit
) {
    StorageLocationSubcategory()
    BrowseScanSubcategory()
    BrowseOpdsSubcategory(
        onNavigateToOpdsCatalog = onNavigateToOpdsCatalog,
        showDivider = false
    )
}

fun LazyListScope.StorageLocationSubcategory() {
    SettingsSubcategory(
        titleColor = { MaterialTheme.colorScheme.primary },
        title = { stringResource(id = R.string.codex_directory_browse_settings) },
        showTitle = true,
        showDivider = true
    ) {
        item {
            StorageLocationPicker()
        }
    }
}