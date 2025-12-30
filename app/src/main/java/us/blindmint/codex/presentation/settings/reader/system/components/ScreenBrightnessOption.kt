/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.system.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.theme.ExpandingTransition

@Composable
fun ScreenBrightnessOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ExpandingTransition(visible = state.value.customScreenBrightness) {
        SliderWithTitle(
            value = state.value.screenBrightness to "",
            toValue = 100,
            title = stringResource(id = R.string.screen_brightness_option),
            onValueChange = {
                mainModel.onEvent(
                    MainEvent.OnChangeScreenBrightness(it)
                )
            }
        )
    }
}