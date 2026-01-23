/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.appearance.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderScreenOrientation
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun ScreenOrientationOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = "Screen Orientation",
        chips = ReaderScreenOrientation.entries.map {
            ButtonItem(
                id = it.toString(),
                title = when (it) {
                    ReaderScreenOrientation.DEFAULT -> "Device"
                    ReaderScreenOrientation.FREE -> stringResource(id = R.string.screen_orientation_free)
                    ReaderScreenOrientation.PORTRAIT -> "Portrait"
                    ReaderScreenOrientation.LANDSCAPE -> "Landscape"
                    ReaderScreenOrientation.REVERSE_PORTRAIT -> "Portrait (inverse)"
                    ReaderScreenOrientation.REVERSE_LANDSCAPE -> "Landscape (inverse)"
                    ReaderScreenOrientation.LOCKED_PORTRAIT -> "Portrait (locked)"
                    ReaderScreenOrientation.LOCKED_LANDSCAPE -> "Landscape (locked)"
                },
                textStyle = MaterialTheme.typography.labelLarge,
                selected = it == state.value.screenOrientation
            )
        },
        onClick = {
            mainModel.onEvent(
                MainEvent.OnChangeScreenOrientation(
                    it.id
                )
            )
        }
    )
}
