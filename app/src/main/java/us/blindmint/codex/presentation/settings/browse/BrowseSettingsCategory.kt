/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListScope
import us.blindmint.codex.presentation.settings.browse.scan.BrowseScanSubcategory
import us.blindmint.codex.presentation.settings.browse.opds.BrowseOpdsSubcategory

fun LazyListScope.BrowseSettingsCategory() {
    item {
        Text(
            text = "Local Files",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    BrowseScanSubcategory()

    item {
        Text(
            text = "OPDS",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    BrowseOpdsSubcategory()
}