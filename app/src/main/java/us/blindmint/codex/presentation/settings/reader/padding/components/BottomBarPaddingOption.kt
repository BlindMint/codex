/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.padding.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun BottomBarPaddingOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SliderWithTitle(
        value = state.value.bottomBarPadding to "pt",
        fromValue = 0,
        toValue = 24,
        title = stringResource(id = R.string.bottom_bar_padding_option),
        onValueChange = {
            mainModel.onEvent(
                MainEvent.OnChangeBottomBarPadding(it)
            )
        }
    )
}