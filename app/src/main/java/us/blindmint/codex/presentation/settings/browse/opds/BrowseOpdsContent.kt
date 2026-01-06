/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.opds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel

@Composable
fun BrowseOpdsContent(onNavigateToSettings: () -> Unit) {
    val opdsModel = hiltViewModel<OpdsSourcesModel>()
    val sources = opdsModel.state.collectAsStateWithLifecycle().value.sources

    Text(
        "Manage OPDS Sources",
        modifier = Modifier
            .clickable { onNavigateToSettings() }
            .padding(16.dp)
    )
}
