/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.domain.import_progress.ImportOperation
import us.blindmint.codex.ui.import_progress.ImportProgressViewModel
import us.blindmint.codex.ui.settings.SettingsEvent
import us.blindmint.codex.ui.settings.SettingsModel
import us.blindmint.codex.presentation.settings.browse.scan.components.FolderImportProgress
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.theme.dynamicListItemColor

@Composable
fun StorageLocationPicker(
    modifier: Modifier = Modifier
) {
    val settingsModel = hiltViewModel<SettingsModel>()
    val importProgressViewModel = hiltViewModel<ImportProgressViewModel>()
    val state by settingsModel.state.collectAsStateWithLifecycle()
    val importOperations by importProgressViewModel.importOperations.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val codexImportOperation = importOperations.find { op ->
        op.folderUri == android.net.Uri.EMPTY
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            settingsModel.onEvent(SettingsEvent.OnSetCodexRootFolder(it))
        }
        showPicker = false
    }

    Column(
        modifier = modifier
            .padding(
                horizontal = 18.dp,
                vertical = 8.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.clickable {
                if (state.codexRootDisplayPath == null) {
                    showInfoDialog = true
                } else {
                    showPicker = true
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.dynamicListItemColor(0))
                    .padding(11.dp)
                    .size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                StyledText(
                    text = "Codex Directory",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                StyledText(
                    text = state.codexRootDisplayPath ?: "Tap to configure",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (state.codexRootDisplayPath != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        settingsModel.onEvent(SettingsEvent.OnScanCodexDirectory)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Rescan Codex Directory",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { showRemoveConfirmation = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Remove Codex Directory",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (codexImportOperation != null) {
            FolderImportProgress(
                operation = codexImportOperation
            )
        }
    }

    if (showPicker) {
        folderPicker.launch(null)
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text("Remove Codex Directory") },
            text = { Text("Are you sure? This will remove the Codex Directory configuration.") },
            confirmButton = {
                TextButton(onClick = {
                    settingsModel.onEvent(SettingsEvent.OnRemoveCodexRootFolder)
                    showRemoveConfirmation = false
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Codex Directory") },
            text = {
                Text(
                    "Codex Directory is the primary storage location for the app and should be a dedicated, " +
                    "top-level folder such as /storage/emulated/0/codex. This is where books and metadata " +
                    "from OPDS sources will be stored. It is not recommended to use built-in Android folders " +
                    "like Documents or Downloads.\n\n" +
                    "Local folders are for adding books from local folders on your system."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfoDialog = false
                    showPicker = true
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
