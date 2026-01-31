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
import us.blindmint.codex.presentation.settings.browse.BrowseSettingsScaffold
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.browse.OpdsRootScreen

@Parcelize
object BrowseSettingsScreen : Screen, Parcelable, SearchableSettings {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        BrowseSettingsScaffold(
            listState = listState,
            scrollBehavior = scrollBehavior,
            navigateBack = {
                navigator.pop()
            },
            onNavigateToOpdsCatalog = { opdsScreen ->
                navigator.push(opdsScreen)
            }
        )
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(id = R.string.browse_settings)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val mainModel = hiltViewModel<MainModel>()
        val state = mainModel.state.collectAsStateWithLifecycle()

        return listOf(
            getGeneralPreferencesGroup(mainModel, state),
            getFilterPreferencesGroup(mainModel, state),
            getSortPreferencesGroup(mainModel, state),
        )
    }

    @Composable
    private fun getGeneralPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "General",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = "Use Calibre OPF metadata",
                    checked = state.value.useCalibreOpfMetadata,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeUseCalibreOpfMetadata(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getFilterPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Filter",
            preferenceItems = listOf(
                Preference.PreferenceItem.InfoPreference(
                    title = "Filter Settings",
                ),
            ),
        )
    }

    @Composable
    private fun getSortPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Sort",
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = state.value.browseSortOrder.name,
                    entries = mapOf(
                        "LAST_MODIFIED" to "Last Modified",
                        "TITLE" to "Title",
                        "AUTHOR" to "Author",
                    ),
                    title = "Sort order",
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeBrowseSortOrder(it))
                    },
                ),
            ),
        )
    }
}
