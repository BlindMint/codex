/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.os.Parcelable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.components.top_bar.collapsibleTopAppBarScrollBehavior
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.settings.SettingsContent
import us.blindmint.codex.ui.about.AboutScreen
import us.blindmint.codex.ui.help.HelpScreen
import us.blindmint.codex.ui.library.LibrarySettingsScreen
import us.blindmint.codex.ui.settings.ImportExportSettingsScreen

@Parcelize
object SettingsScreen : Screen, Parcelable {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        SettingsContent(
            listState = listState,
            scrollBehavior = scrollBehavior,
            navigateToAppearanceSettings = {
                navigator.push(AppearanceSettingsScreen)
            },
            navigateToReaderSettings = {
                navigator.push(ReaderSettingsScreen)
            },
            navigateToLibrarySettings = {
                navigator.push(LibrarySettingsScreen)
            },
            navigateToBrowseSettings = {
                navigator.push(BrowseSettingsScreen)
            },
            navigateToImportExportSettings = {
                navigator.push(ImportExportSettingsScreen)
            },
            navigateToAbout = {
                navigator.push(AboutScreen)
            },
            navigateToHelp = {
                navigator.push(HelpScreen(fromStart = false))
            },
            navigateBack = {
                navigator.pop()
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        )
    }
}