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
import us.blindmint.codex.presentation.settings.import_export.ImportExportSettingsScaffold
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Parcelize
object ImportExportSettingsScreen : Screen, Parcelable, SearchableSettings {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        ImportExportSettingsScaffold(
            scrollBehavior = scrollBehavior,
            listState = listState,
            navigateBack = {
                navigator.pop()
            }
        )
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(id = R.string.import_export_settings)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        return listOf(
            Preference.PreferenceGroup(
                title = "Backup & Restore",
                preferenceItems = listOf(
                    Preference.PreferenceItem.InfoPreference(
                        title = "Backup Settings",
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = "Restore Settings",
                    ),
                ),
            ),
        )
    }
}
