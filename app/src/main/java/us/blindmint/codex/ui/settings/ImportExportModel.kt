/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.use_case.import_export.ExportSettings
import us.blindmint.codex.domain.use_case.import_export.ImportSettings
import us.blindmint.codex.ui.main.MainState
import javax.inject.Inject

@HiltViewModel
class ImportExportModel @Inject constructor(
    private val exportSettings: ExportSettings,
    private val importSettings: ImportSettings
) : ViewModel() {

    fun exportSettings(uri: Uri, currentState: MainState, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = exportSettings.execute(uri, currentState)
            onResult(result)
        }
    }

    fun importSettings(uri: Uri, onResult: (Result<Unit>) -> Unit, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = importSettings.execute(uri)
            if (result.isSuccess) {
                // Notify that import was successful so the UI can reload MainModel
                onSuccess()
            }
            onResult(result)
        }
    }

    fun getSuggestedFileName(): String {
        return exportSettings.getSuggestedFileName()
    }
}