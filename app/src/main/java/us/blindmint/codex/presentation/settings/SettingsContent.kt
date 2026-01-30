/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
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
    SettingsScaffold(
        listState = listState,
        scrollBehavior = scrollBehavior,
        navigateToAppearanceSettings = navigateToAppearanceSettings,
        navigateToReaderSettings = navigateToReaderSettings,
        navigateToLibrarySettings = navigateToLibrarySettings,
        navigateToBrowseSettings = navigateToBrowseSettings,
        navigateToImportExportSettings = navigateToImportExportSettings,
        navigateToAbout = navigateToAbout,
        navigateToHelp = navigateToHelp,
        navigateBack = navigateBack,
        showSearch = showSearch,
        searchQuery = searchQuery,
        focusRequester = focusRequester,
        onSearchVisibilityChange = onSearchVisibilityChange,
        onSearchQueryChange = onSearchQueryChange,
        onSearch = onSearch
    )
}