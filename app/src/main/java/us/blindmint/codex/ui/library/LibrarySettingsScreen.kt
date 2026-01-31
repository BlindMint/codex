/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.library

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
import us.blindmint.codex.presentation.settings.library.LibrarySettingsScaffold
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Parcelize
object LibrarySettingsScreen : Screen, Parcelable, SearchableSettings {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        LibrarySettingsScaffold(
            listState = listState,
            scrollBehavior = scrollBehavior,
            navigateBack = {
                navigator.pop()
            }
        )
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(id = R.string.library_settings)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val mainModel = hiltViewModel<MainModel>()
        val state = mainModel.state.collectAsStateWithLifecycle()

        return listOf(
            getDisplayPreferencesGroup(mainModel, state),
            getSortPreferencesGroup(mainModel, state),
            getTabsPreferencesGroup(mainModel, state),
        )
    }

    @Composable
    private fun getDisplayPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Display",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = "Show progress",
                    checked = state.value.libraryShowProgress,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeLibraryShowProgress(it))
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    title = "Show read button",
                    checked = state.value.libraryShowReadButton,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeLibraryShowReadButton(it))
                    },
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
                    value = state.value.librarySortOrder.name,
                    entries = mapOf(
                        "TITLE" to "Title",
                        "AUTHOR" to "Author",
                        "DATE_ADDED" to "Date Added",
                        "LAST_READ" to "Last Read",
                    ),
                    title = "Sort order",
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeLibrarySortOrder(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getTabsPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Tabs",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = "Show category tabs",
                    checked = state.value.libraryShowCategoryTabs,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeLibraryShowCategoryTabs(it))
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    title = "Show book count",
                    checked = state.value.libraryShowBookCount,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeLibraryShowBookCount(it))
                    },
                ),
            ),
        )
    }
}
