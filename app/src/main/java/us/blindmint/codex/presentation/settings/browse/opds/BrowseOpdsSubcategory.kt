/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse.opds

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun LazyListScope.BrowseOpdsSubcategory() {
    item {
        Text(
            text = "OPDS Sources",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    item {
        Button(
            onClick = { /* TODO: Open add source dialog */ },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Add OPDS Source")
        }
    }
    // TODO: Add OPDS source list and management UI
}