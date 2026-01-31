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
import us.blindmint.codex.presentation.settings.general.GeneralSettingsScaffold
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Parcelize
object GeneralSettingsScreen : Screen, Parcelable, SearchableSettings {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        GeneralSettingsScaffold(
            listState = listState,
            scrollBehavior = scrollBehavior,
            navigateBack = {
                navigator.pop()
            }
        )
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(id = R.string.general_settings)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val mainModel = hiltViewModel<MainModel>()
        val state = mainModel.state.collectAsStateWithLifecycle()

        return listOf(
            getAppPreferencesGroup(mainModel, state),
        )
    }

    @Composable
    private fun getAppPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "App Settings",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = "Show start screen",
                    checked = state.value.showStartScreen,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeShowStartScreen(it))
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    title = "Double press exit",
                    checked = state.value.doublePressExit,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeDoublePressExit(it))
                    },
                ),
            ),
        )
    }
}
