/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.reading_mode.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderHorizontalGesture
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun HorizontalGestureOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(id = R.string.horizontal_gesture_option),
        chips = ReaderHorizontalGesture.entries.map {
            ButtonItem(
                id = it.toString(),
                title = when (it) {
                    ReaderHorizontalGesture.OFF -> {
                        stringResource(R.string.horizontal_gesture_off)
                    }

                    ReaderHorizontalGesture.ON -> {
                        stringResource(R.string.horizontal_gesture_on)
                    }

                    ReaderHorizontalGesture.INVERSE -> {
                        stringResource(R.string.horizontal_gesture_inverse)
                    }
                },
                textStyle = MaterialTheme.typography.labelLarge,
                selected = it == state.value.horizontalGesture
            )
        },
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeHorizontalGesture(
                    it.id
                )
            )
        }
    )
}