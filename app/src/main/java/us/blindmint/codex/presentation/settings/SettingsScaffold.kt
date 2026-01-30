/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    listState: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    navigateToAppearanceSettings: () -> Unit,
    navigateToReaderSettings: () -> Unit,
    navigateToLibrarySettings: () -> Unit,
    navigateToBrowseSettings: () -> Unit,
    navigateToImportExportSettings: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToHelp: () -> Unit,
    navigateBack: () -> Unit,
    showSearch: Boolean = false,
    searchQuery: String = "",
    focusRequester: FocusRequester,
    onSearchVisibilityChange: (Boolean) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: () -> Unit = {}
) {
    Scaffold(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsTopBar(
                scrollBehavior = scrollBehavior,
                navigateBack = navigateBack,
                showSearch = showSearch,
                searchQuery = searchQuery,
                focusRequester = focusRequester,
                onSearchVisibilityChange = onSearchVisibilityChange,
                onSearchQueryChange = onSearchQueryChange,
                onSearch = onSearch
            )
        }
    ) { paddingValues ->
        SettingsLayout(
            listState = listState,
            paddingValues = paddingValues,
            navigateToAppearanceSettings = navigateToAppearanceSettings,
            navigateToReaderSettings = navigateToReaderSettings,
            navigateToLibrarySettings = navigateToLibrarySettings,
            navigateToBrowseSettings = navigateToBrowseSettings,
            navigateToImportExportSettings = navigateToImportExportSettings,
            navigateToAbout = navigateToAbout,
            navigateToHelp = navigateToHelp,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange
        )
    }
}