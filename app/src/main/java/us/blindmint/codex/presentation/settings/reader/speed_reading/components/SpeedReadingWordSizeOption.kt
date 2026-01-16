/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun SpeedReadingWordSizeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SliderWithTitle(
        title = stringResource(id = R.string.speed_reading_word_size),
        value = Pair(48, "sp"), // TODO: Add to MainState, default 48sp
        fromValue = 24,
        toValue = 96,
        onValueChange = { /* TODO: Save to settings */ }
    )
}