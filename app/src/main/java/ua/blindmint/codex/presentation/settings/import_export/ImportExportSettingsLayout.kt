/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.import_export

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ua.blindmint.codex.R
import ua.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar

@Composable
fun ImportExportSettingsLayout(
    listState: LazyListState,
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumnWithScrollbar(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        state = listState
    ) {
        item {
            ImportSettingsItem(snackbarHostState)
        }

        item {
            ExportSettingsItem { result ->
                scope.launch {
                    val message = if (result.isSuccess) {
                        context.getString(R.string.export_success)
                    } else {
                        context.getString(R.string.export_error)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }
}