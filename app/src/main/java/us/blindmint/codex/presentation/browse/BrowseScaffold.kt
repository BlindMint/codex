/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import us.blindmint.codex.domain.browse.display.BrowseLayout
import us.blindmint.codex.domain.browse.file.SelectableFile
import us.blindmint.codex.presentation.browse.opds.OpdsCatalogPanel
import us.blindmint.codex.ui.browse.BrowseEvent
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.theme.DefaultTransition

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BrowseScaffold(
    files: List<SelectableFile>,
    refreshState: PullRefreshState,
    listState: LazyListState,
    gridState: LazyGridState,
    layout: BrowseLayout,
    gridSize: Int,
    autoGridSize: Boolean,
    includedFilterItems: List<String>,
    pinnedPaths: List<String>,
    canScrollBackList: Boolean,
    canScrollBackGrid: Boolean,
    hasSelectedItems: Boolean,
    selectedItemsCount: Int,
    isRefreshing: Boolean,
    isLoading: Boolean,
    dialogHidden: Boolean,
    filesEmpty: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    focusRequester: FocusRequester,
    searchVisibility: (BrowseEvent.OnSearchVisibility) -> Unit,
    searchQueryChange: (BrowseEvent.OnSearchQueryChange) -> Unit,
    search: (BrowseEvent.OnSearch) -> Unit,
    requestFocus: (BrowseEvent.OnRequestFocus) -> Unit,
    clearSelectedFiles: (BrowseEvent.OnClearSelectedFiles) -> Unit,
    selectFiles: (BrowseEvent.OnSelectFiles) -> Unit,
    selectFile: (BrowseEvent.OnSelectFile) -> Unit,
    showFilterBottomSheet: (BrowseEvent.OnShowFilterBottomSheet) -> Unit,
    showAddDialog: (BrowseEvent.OnShowAddDialog) -> Unit,
    changePinnedPaths: (MainEvent.OnChangeBrowsePinnedPaths) -> Unit,
    navigateToBrowseSettings: () -> Unit,
    onNavigateToOpdsCatalog: (us.blindmint.codex.ui.browse.OpdsRootScreen) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(refreshState),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            BrowseTopBar(
                files = files,
                layout = layout,
                includedFilterItems = includedFilterItems,
                canScrollBackList = canScrollBackList,
                canScrollBackGrid = canScrollBackGrid,
                hasSelectedItems = hasSelectedItems,
                selectedItemsCount = selectedItemsCount,
                showSearch = showSearch,
                searchQuery = searchQuery,
                focusRequester = focusRequester,
                searchVisibility = searchVisibility,
                searchQueryChange = searchQueryChange,
                search = search,
                requestFocus = requestFocus,
                clearSelectedFiles = clearSelectedFiles,
                selectFiles = selectFiles,
                showFilterBottomSheet = showFilterBottomSheet,
                showAddDialog = showAddDialog
            )
        }
        ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            DefaultTransition(visible = !isLoading) {
                OpdsCatalogPanel(
                    refreshState = refreshState,
                    listState = listState,
                    gridState = gridState,
                    layout = layout,
                    isRefreshing = isRefreshing,
                    showSearch = showSearch,
                    searchQuery = searchQuery,
                    focusRequester = focusRequester,
                    searchVisibility = searchVisibility,
                    searchQueryChange = searchQueryChange,
                    search = search,
                    requestFocus = requestFocus,
                    navigateToBrowseSettings = navigateToBrowseSettings,
                    onNavigateToOpdsCatalog = onNavigateToOpdsCatalog
                )
            }

            BrowseRefreshIndicator(
                isRefreshing = isRefreshing,
                refreshState = refreshState
            )
        }
    }
}