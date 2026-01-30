/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.appearance.theme_preferences.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.domain.ui.DarkTheme
import us.blindmint.codex.domain.ui.isDark
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.settings.SettingsEvent
import us.blindmint.codex.ui.settings.SettingsModel

@Composable
fun DarkThemeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val settingsModel = hiltViewModel<SettingsModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    var previousDarkTheme by remember { mutableStateOf(state.value.darkTheme) }

    LaunchedEffect(state.value.darkTheme) {
        if (previousDarkTheme != state.value.darkTheme) {
            previousDarkTheme = state.value.darkTheme
            val isDarkMode = state.value.darkTheme == DarkTheme.ON || state.value.darkTheme == DarkTheme.FOLLOW_SYSTEM
            settingsModel.syncThemePresetWithSystemDarkMode(isDarkMode)
        }
    }

    SegmentedButtonWithTitle(
        title = stringResource(id = R.string.dark_theme_option),
        buttons = DarkTheme.entries.map {
            ButtonItem(
                it.toString(),
                title = when (it) {
                    DarkTheme.OFF -> stringResource(id = R.string.dark_theme_off)
                    DarkTheme.ON -> stringResource(id = R.string.dark_theme_on)
                    DarkTheme.FOLLOW_SYSTEM -> stringResource(id = R.string.dark_theme_follow_system)
                },
                textStyle = MaterialTheme.typography.labelLarge,
                selected = it == state.value.darkTheme
            )
        }
    ) {
        mainModel.onEvent(MainEvent.OnChangeDarkTheme(it.id))
    }
}