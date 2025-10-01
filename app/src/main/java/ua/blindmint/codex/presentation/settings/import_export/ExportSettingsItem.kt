/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.import_export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ua.blindmint.codex.R
import ua.blindmint.codex.ui.main.MainModel
import ua.blindmint.codex.ui.settings.ImportExportModel

@Composable
fun ExportSettingsItem(
    onResult: (Result<Unit>) -> Unit
) {
    val importExportModel = hiltViewModel<ImportExportModel>()
    val mainModel = hiltViewModel<MainModel>()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val currentState = mainModel.state.value
            importExportModel.exportSettings(uri, currentState, onResult)
        }
    }

    Button(
        onClick = {
            val suggestedName = importExportModel.getSuggestedFileName()
            exportLauncher.launch(suggestedName)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.export_settings),
            style = MaterialTheme.typography.titleMedium
        )
    }
}