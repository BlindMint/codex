/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.main.MainState

data class OptionConfig<T>(
    val stateSelector: (MainState) -> T,
    val eventCreator: (T) -> MainEvent,
    val component: @Composable (T, (T) -> Unit) -> Unit
)

@Composable
fun <T> GenericOption(config: OptionConfig<T>) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    val value = remember(state.value) {
        config.stateSelector(state.value)
    }

    config.component(value) { newValue ->
        mainModel.onEvent(config.eventCreator(newValue))
    }
}