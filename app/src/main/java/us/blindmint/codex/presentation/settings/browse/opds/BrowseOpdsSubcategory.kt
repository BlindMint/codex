/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.browse.opds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel

fun LazyListScope.BrowseOpdsSubcategory(onNavigateToSettings: () -> Unit = {}) {
    item {
        us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle(title = "OPDS Sources")
    }
    item {
        BrowseOpdsManagementContent()
    }
}