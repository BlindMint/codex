/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.ui.settings.SettingsEvent
import us.blindmint.codex.ui.settings.SettingsModel

/**
 * Storage location picker component.
 * Shows storage location as a single clickable ListItem with headline and supporting content.
 */
@Composable
fun StorageLocationPicker(
    modifier: Modifier = Modifier
) {
    val settingsModel = hiltViewModel<SettingsModel>()
    val state by settingsModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { settingsModel.onEvent(SettingsEvent.OnSetCodexRootFolder(it)) }
        showPicker = false
    }

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        headlineContent = {
            Text(
                text = "Storage Location",
                style = MaterialTheme.typography.titleSmall
            )
        },
        supportingContent = {
            Text(
                text = state.codexRootDisplayPath ?: "Tap to configure",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.codexRootDisplayPath != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    )

    if (showPicker) {
        folderPicker.launch(null)
    }
}