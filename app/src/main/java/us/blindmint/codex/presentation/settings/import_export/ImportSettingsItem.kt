/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.import_export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import us.blindmint.codex.R
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.settings.ImportExportModel
import us.blindmint.codex.ui.settings.SettingsModel

@Composable
fun ImportSettingsItem(
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importExportModel = hiltViewModel<ImportExportModel>()
    val mainModel = hiltViewModel<MainModel>()
    val settingsModel = hiltViewModel<SettingsModel>()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            showConfirmDialog = true
        }
    }

    if (showConfirmDialog && selectedUri != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedUri = null
            },
            title = {
                Text(text = stringResource(id = R.string.confirm_import_title))
            },
            text = {
                Text(text = stringResource(id = R.string.confirm_import_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedUri?.let { uri ->
                            importExportModel.importSettings(uri, onResult = { result ->
                                scope.launch {
                                    val message = if (result.isSuccess) {
                                        context.getString(R.string.import_success)
                                    } else {
                                        context.getString(R.string.import_error)
                                    }
                                    snackbarHostState.showSnackbar(message)
                                }
                            }, onSuccess = {
                                // Reload MainModel state to reflect imported settings
                                mainModel.reloadSettings()
                                // Also reload SettingsModel color presets
                                settingsModel.reloadColorPresets()
                            })
                        }
                        selectedUri = null
                    }
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedUri = null
                    }
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Button(
        onClick = {
            filePickerLauncher.launch("application/json")
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.import_settings),
            style = MaterialTheme.typography.titleMedium
        )
    }
}