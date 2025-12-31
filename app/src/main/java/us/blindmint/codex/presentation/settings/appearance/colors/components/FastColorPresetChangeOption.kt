/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.appearance.colors.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun FastColorPresetChangeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SwitchWithTitle(
        selected = state.value.fastColorPresetChange,
        title = stringResource(id = R.string.fast_color_preset_change_option),
        description = stringResource(id = R.string.fast_color_preset_change_option_desc),
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeFastColorPresetChange(
                    !state.value.fastColorPresetChange
                )
            )
        }
    )
}