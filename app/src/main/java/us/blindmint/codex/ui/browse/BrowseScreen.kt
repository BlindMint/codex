/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import us.blindmint.codex.R
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.settings.browse.scan.components.BrowseScanOption
import us.blindmint.codex.presentation.settings.browse.scan.components.BrowseScanOptionNote
import us.blindmint.codex.presentation.settings.browse.opds.BrowseOpdsContent
import us.blindmint.codex.presentation.core.components.common.SearchTextField
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBar
import us.blindmint.codex.presentation.core.components.top_bar.TopAppBarData
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel
import us.blindmint.codex.ui.browse.BrowseModel
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.browse.BrowseEvent
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.settings.BrowseSettingsScreen
import us.blindmint.codex.presentation.browse.BrowseContent

@Parcelize
data class BrowseScreen(val id: Int = 0) : Screen, Parcelable {

    companion object {
        const val ADD_DIALOG = "add_dialog"
        const val FILTER_BOTTOM_SHEET = "filter_bottom_sheet"
        val refreshListChannel: Channel<Unit> = Channel(Channel.CONFLATED)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val activity = LocalActivity.current

        val screenModel = hiltViewModel<OpdsSourcesModel>()
        val browseModel = hiltViewModel<BrowseModel>()

        val sources = screenModel.state.collectAsStateWithLifecycle()
        val browseState = browseModel.state.collectAsStateWithLifecycle()

        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf(stringResource(R.string.local), stringResource(R.string.opds))

        val topBarFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

        // Tab-specific state for search and filter
        var localTabShowSearch by remember { mutableStateOf(false) }
        var localTabSearchQuery by remember { mutableStateOf("") }
        var localTabShowFilter by remember { mutableStateOf(false) }
        var opdsTabShowSearch by remember { mutableStateOf(false) }
        var opdsTabSearchQuery by remember { mutableStateOf("") }

        // Get current tab's state
        val currentTabShowSearch = if (selectedTabIndex == 0) localTabShowSearch else opdsTabShowSearch
        val currentTabSearchQuery = if (selectedTabIndex == 0) localTabSearchQuery else opdsTabSearchQuery



        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    scrollBehavior = null,
                    isTopBarScrolled = false,
                    shownTopBar = when {
                    selectedTabIndex == 0 && browseState.value.hasSelectedItems -> 2
                    selectedTabIndex == 0 && localTabShowSearch -> 1
                    else -> 0
                },
                    topBars = listOf(
                        TopAppBarData(
                            contentID = 0,
                            contentNavigationIcon = {},
                            contentTitle = {
                                Text(stringResource(R.string.browse_screen))
                            },
                            contentActions = {
                                // Search icon for Local tab only (OPDS has its own search in sub-screens)
                                if (selectedTabIndex == 0) {
                                    IconButton(onClick = {
                                        localTabShowSearch = true
                                    }) {
                                        Icon(Icons.Default.Search, "Search")
                                    }
                                }
                                // Filter icon for Local tab
                                if (selectedTabIndex == 0) {
                                    IconButton(onClick = {
                                        localTabShowFilter = true
                                    }) {
                                        Icon(Icons.Default.FilterList, "Filter")
                                    }
                                }
                            }
                        ),
                        TopAppBarData(
                            contentID = 1,
                            contentNavigationIcon = {
                                IconButton(onClick = {
                                    localTabShowSearch = false
                                    localTabSearchQuery = ""
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit search")
                                }
                            },
                            contentTitle = {
                                SearchTextField(
                                    modifier = Modifier,
                                    initialQuery = localTabSearchQuery,
                                    onQueryChange = { query ->
                                        localTabSearchQuery = query
                                    },
                                    onSearch = {
                                        // Search is handled by LocalTabContent
                                    }
                                )
                            },
                            contentActions = {}
                        ),
                        TopAppBarData(
                            contentID = 2,
                            contentNavigationIcon = {
                                IconButton(onClick = {
                                    browseModel.onEvent(BrowseEvent.OnClearSelectedFiles)
                                }) {
                                    Icon(Icons.Default.Clear, "Clear selection")
                                }
                            },
                            contentTitle = {
                                Text("${browseState.value.selectedItemsCount} selected")
                            },
                            contentActions = {
                                IconButton(onClick = {
                                    browseModel.onEvent(BrowseEvent.OnSelectFiles(
                                        includedFileFormats = emptyList(),
                                        files = browseState.value.files
                                    ))
                                }) {
                                    Icon(Icons.Default.SelectAll, "Select all")
                                }
                                IconButton(onClick = {
                                    browseModel.onEvent(BrowseEvent.OnShowAddDialog)
                                }) {
                                    Icon(Icons.Default.CheckCircle, "Add selected")
                                }
                            }
                        )
                    )
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                // Reset search state when switching tabs
                                if (index == 0) {
                                    localTabShowSearch = false
                                    localTabSearchQuery = ""
                                } else {
                                    opdsTabShowSearch = false
                                    opdsTabSearchQuery = ""
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 ->                     LocalTabContent(
                        showSearch = localTabShowSearch,
                        searchQuery = localTabSearchQuery,
                        onSearchVisibility = { show ->
                            localTabShowSearch = show
                            if (!show) localTabSearchQuery = ""
                        },
                        onSearchQueryChange = { query ->
                            localTabSearchQuery = query
                        },
                        showFilter = localTabShowFilter,
                        onFilterShown = { localTabShowFilter = false }
                    )
                    1 -> OpdsTabContent(sources.value.sources)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun LocalTabContent(
    showSearch: Boolean,
    searchQuery: String,
    onSearchVisibility: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    showFilter: Boolean = false,
    onFilterShown: () -> Unit = {}
) {
    val navigator = LocalNavigator.current
    val activity = LocalActivity.current
    val mainModel = hiltViewModel<MainModel>()
    val browseModel = hiltViewModel<BrowseModel>()

    val browseState by browseModel.state.collectAsStateWithLifecycle()
    val mainState by mainModel.state.collectAsStateWithLifecycle()

    // Filter files based on included filter items
    val filteredFiles = remember(browseState.files, mainState.browseIncludedFilterItems) {
        if (mainState.browseIncludedFilterItems.isEmpty()) {
            browseState.files
        } else {
            browseState.files.filter { file ->
                mainState.browseIncludedFilterItems.any { extension ->
                    file.data.path.endsWith(extension, ignoreCase = true)
                }
            }
        }
    }

    // Sync local search state with browse model
    if (showSearch != browseState.showSearch) {
        browseModel.onEvent(BrowseEvent.OnSearchVisibility(showSearch))
    }
    if (searchQuery != browseState.searchQuery) {
        browseModel.onEvent(BrowseEvent.OnSearchQueryChange(searchQuery))
    }

    // Trigger filter bottom sheet when requested
    if (showFilter) {
        browseModel.onEvent(BrowseEvent.OnShowFilterBottomSheet)
        onFilterShown()
    }

    val focusRequester = remember { FocusRequester() }
    val refreshState = rememberPullRefreshState(
        refreshing = browseState.isRefreshing,
        onRefresh = { browseModel.onEvent(BrowseEvent.OnRefreshList(loading = false, hideSearch = false)) }
    )
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    BrowseContent(
        files = filteredFiles,
        selectedBooksAddDialog = browseState.selectedBooksAddDialog,
        refreshState = refreshState,
        loadingAddDialog = browseState.loadingAddDialog,
        dialog = browseState.dialog,
        bottomSheet = browseState.bottomSheet,
        listState = listState,
        gridState = gridState,
        layout = mainState.browseLayout,
        gridSize = mainState.browseGridSize,
        autoGridSize = mainState.browseAutoGridSize,
        includedFilterItems = mainState.browseIncludedFilterItems,
        pinnedPaths = mainState.browsePinnedPaths,
        canScrollBackList = listState.canScrollBackward,
        canScrollBackGrid = gridState.canScrollBackward,
        hasSelectedItems = browseState.hasSelectedItems,
        selectedItemsCount = browseState.selectedItemsCount,
        isRefreshing = browseState.isRefreshing,
        isLoading = browseState.isLoading,
        dialogHidden = browseState.dialog == null,
        filesEmpty = filteredFiles.isEmpty(),
        showSearch = showSearch,
        searchQuery = searchQuery,
        focusRequester = focusRequester,
        searchVisibility = { event -> onSearchVisibility(event.show) },
        searchQueryChange = { event -> onSearchQueryChange(event.query) },
        search = { browseModel.onEvent(it) },
        requestFocus = { browseModel.onEvent(it) },
        clearSelectedFiles = { browseModel.onEvent(it) },
        selectFiles = { browseModel.onEvent(it) },
        selectFile = { browseModel.onEvent(it) },
        dismissBottomSheet = { browseModel.onEvent(it) },
        showFilterBottomSheet = { browseModel.onEvent(it) },
        showAddDialog = { browseModel.onEvent(it) },
        dismissAddDialog = { browseModel.onEvent(it) },
        actionAddDialog = { event ->
            browseModel.onEvent(BrowseEvent.OnActionAddDialog(
                context = activity,
                navigateToLibrary = { navigator.push(LibraryScreen(), saveInBackStack = false) }
            ))
        },
        selectAddDialog = { browseModel.onEvent(it) },
        changePinnedPaths = mainModel::onEvent,
        navigateToLibrary = { navigator.push(LibraryScreen(), saveInBackStack = false) },
        navigateToBrowseSettings = { navigator.push(BrowseSettingsScreen()) },
        onRescan = { BrowseScreen.refreshListChannel.trySend(Unit) }
    )
}

@Composable
private fun OpdsTabContent(sources: List<us.blindmint.codex.data.local.dto.OpdsSourceEntity>) {
    val navigator = LocalNavigator.current

    LazyColumn {
        item {
            us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle(title = "OPDS Catalogs")
        }
        items(sources) { source ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = { navigator.push(OpdsRootScreen(source)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = source.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
        item {
            BrowseOpdsContent(onNavigateToSettings = { navigator.push(BrowseSettingsScreen()) })
        }
    }
}