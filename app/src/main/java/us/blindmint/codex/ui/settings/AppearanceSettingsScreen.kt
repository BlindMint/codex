/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings

import android.os.Parcelable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.R
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.components.top_bar.collapsibleTopAppBarScrollBehavior
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.settings.Preference
import us.blindmint.codex.presentation.settings.SearchableSettings
import us.blindmint.codex.presentation.settings.appearance.AppearanceSettingsScaffold
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Parcelize
object AppearanceSettingsScreen : Screen, Parcelable, SearchableSettings {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        AppearanceSettingsScaffold(
            scrollBehavior = scrollBehavior,
            listState = listState,
            navigateBack = {
                navigator.pop()
            }
        )
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(id = R.string.appearance_settings)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val mainModel = hiltViewModel<MainModel>()
        val state = mainModel.state.collectAsStateWithLifecycle()

        return listOf(
            getThemePreferencesGroup(mainModel, state),
            getColorsPreferencesGroup(mainModel, state),
        )
    }

    @Composable
    private fun getThemePreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.theme_appearance_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = state.value.darkTheme.name,
                    entries = mapOf(
                        "OFF" to "Light",
                        "ON" to "Dark",
                        "FOLLOW_SYSTEM" to "System",
                    ),
                    title = stringResource(id = R.string.dark_theme_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeDarkTheme(it))
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    title = stringResource(id = R.string.pure_dark_option),
                    checked = state.value.pureDark.name == "ON",
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangePureDark(if (it) "ON" else "OFF"))
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    value = state.value.theme.name,
                    entries = mapOf(
                        "CATPPUCCIN" to "Catppuccin",
                        "ROSE_PINE" to "Rose Pine",
                        "ROSE_PINE_MOON" to "Rose Pine Moon",
                        "DRACULA" to "Dracula",
                        "NORD" to "Nord",
                        "GRUVBOX" to "Gruvbox",
                        "TOKYO_NIGHT" to "Tokyo Night",
                        "MONOKAI" to "Monokai",
                        "SOLARIZED" to "Solarized",
                    ),
                    title = stringResource(id = R.string.app_theme_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeTheme(it))
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    value = state.value.themeContrast.name,
                    entries = mapOf(
                        "STANDARD" to "Standard",
                        "MEDIUM" to "Medium",
                        "HIGH" to "High",
                    ),
                    title = stringResource(id = R.string.theme_contrast_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeThemeContrast(it))
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    title = stringResource(id = R.string.absolute_dark_option),
                    checked = state.value.absoluteDark,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeAbsoluteDark(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getColorsPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.colors_appearance_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = stringResource(id = R.string.fast_color_preset_change_option),
                    checked = state.value.fastColorPresetChange,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeFastColorPresetChange(it))
                    },
                ),
            ),
        )
    }
}
