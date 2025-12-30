/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.progress.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun ProgressBarOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SwitchWithTitle(
        selected = state.value.progressBar,
        title = stringResource(id = R.string.progress_bar_option),
        description = stringResource(id = R.string.progress_bar_option_desc),
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeProgressBar(
                    !state.value.progressBar
                )
            )
        }
    )
}