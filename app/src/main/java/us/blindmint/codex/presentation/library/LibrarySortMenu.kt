/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * LibrarySortMenu.kt - Library sorting and display options dialog
 */

package us.blindmint.codex.presentation.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.domain.library.sort.LibrarySortOrder
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.presentation.core.components.modal_drawer.DrawerSide
import us.blindmint.codex.ui.library.FilterState
import us.blindmint.codex.ui.library.LibraryEvent
import us.blindmint.codex.ui.library.LibraryModel
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.presentation.settings.library.display.components.LibraryGridSizeOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryLayoutOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryListSizeOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryShowProgressOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryShowReadButtonOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryTitlePositionOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibrarySortMenu(
    onDismiss: () -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    // Tabs
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.sort_settings),
        stringResource(R.string.display_settings),
        stringResource(R.string.filter_settings)
    )

    // Subpanel state and data for filters
    val libraryModel = hiltViewModel<LibraryModel>()
    val libraryState by libraryModel.state.collectAsStateWithLifecycle()

    // Load metadata from repository
    var availableTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableAuthors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableSeries by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableLanguages by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Load metadata on composition
    LaunchedEffect(Unit) {
        libraryModel.loadMetadata(
            onTagsLoaded = { tags -> availableTags = tags.toSet() },
            onAuthorsLoaded = { authors -> availableAuthors = authors.toSet() },
            onSeriesLoaded = { series -> availableSeries = series.toSet() },
            onLanguagesLoaded = { languages -> availableLanguages = languages.toSet() },
            onYearRangeLoaded = { _, _ -> /* Year range disabled */ }
        )
    }

    val sortedTags = availableTags.sorted()
    val sortedAuthors = availableAuthors.sorted()
    val sortedSeries = availableSeries.sorted()
    val sortedLanguages = availableLanguages.sorted()

    // Local state for UI
    var selectedStatuses: Set<String> by remember(libraryState.filterState.selectedStatuses) {
        mutableStateOf(libraryState.filterState.selectedStatuses)
    }
    var selectedTags: Set<String> by remember(libraryState.filterState.selectedTags) {
        mutableStateOf(libraryState.filterState.selectedTags)
    }
    var selectedAuthors: Set<String> by remember(libraryState.filterState.selectedAuthors) {
        mutableStateOf(libraryState.filterState.selectedAuthors)
    }
    var selectedSeries: Set<String> by remember(libraryState.filterState.selectedSeries) {
        mutableStateOf(libraryState.filterState.selectedSeries)
    }
    var selectedLanguages: Set<String> by remember(libraryState.filterState.selectedLanguages) {
        mutableStateOf(libraryState.filterState.selectedLanguages)
    }

    // Helper function to update filter state immediately when selections change
    fun updateFilterState() {
        val newFilterState = FilterState(
            selectedStatuses = selectedStatuses,
            selectedTags = selectedTags,
            selectedAuthors = selectedAuthors,
            selectedSeries = selectedSeries,
            publicationYearRange = 1900..2026, // Default range since year slider is disabled
            selectedLanguages = selectedLanguages
        )
        libraryModel.onEvent(LibraryEvent.OnUpdateFilterState(newFilterState))
    }

    // Subpanel state
    var showTagsSubpanel by remember { mutableStateOf(false) }
    var showAuthorsSubpanel by remember { mutableStateOf(false) }
    var showSeriesSubpanel by remember { mutableStateOf(false) }

    ModalDrawer(
        show = true,
        side = DrawerSide.LEFT,
        onDismissRequest = onDismiss,
        header = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledText(
                    text = stringResource(R.string.library_sort_and_display),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    ) {
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        val width by animateDpAsState(
                            targetValue = tabPositions[selectedTabIndex].contentWidth,
                            label = ""
                        )

                        TabRowDefaults.PrimaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            width = width
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab) }
                    )
                }
            }
        }

        // Content based on selected tab
        item {
            when (selectedTabIndex) {
                0 -> LibrarySortTabContent(state, mainModel)
                1 -> LibraryDisplayTabContent(state, mainModel)
                2 -> LibraryFilterTabContent(
                    selectedStatuses = selectedStatuses,
                    selectedTags = selectedTags,
                    selectedAuthors = selectedAuthors,
                    selectedSeries = selectedSeries,
                    selectedLanguages = selectedLanguages,
                    sortedTags = sortedTags,
                    sortedAuthors = sortedAuthors,
                    sortedSeries = sortedSeries,
                    sortedLanguages = sortedLanguages,
                    onStatusToggle = { status, selected ->
                        selectedStatuses = if (selected) selectedStatuses + status else selectedStatuses - status
                        updateFilterState()
                    },
                    onShowTagsSubpanel = { showTagsSubpanel = true },
                    onShowAuthorsSubpanel = { showAuthorsSubpanel = true },
                    onShowSeriesSubpanel = { showSeriesSubpanel = true },
                    onLanguageToggle = { language, selected ->
                        selectedLanguages = if (selected) selectedLanguages + language else selectedLanguages - language
                        updateFilterState()
                    }
                )
            }
        }
    }

    // Tags subpanel
    if (showTagsSubpanel) {
        LibraryFilterSubpanel(
            title = "Tags",
            show = showTagsSubpanel,
            items = sortedTags.toSet(),
            selectedItems = selectedTags,
            onItemToggle = { item, selected ->
                selectedTags = if (selected) selectedTags + item else selectedTags - item
                updateFilterState()
            },
            onSelectAll = { selectedTags = sortedTags.toSet() },
            onDeselectAll = { selectedTags = emptySet() },
            onReset = { selectedTags = libraryState.filterState.selectedTags },
            onDismiss = { showTagsSubpanel = false }
        )
    }

    // Authors subpanel
    if (showAuthorsSubpanel) {
        LibraryFilterSubpanel(
            title = "Authors",
            show = showAuthorsSubpanel,
            items = sortedAuthors.toSet(),
            selectedItems = selectedAuthors,
            onItemToggle = { item, selected ->
                selectedAuthors = if (selected) selectedAuthors + item else selectedAuthors - item
                updateFilterState()
            },
            onSelectAll = { selectedAuthors = sortedAuthors.toSet() },
            onDeselectAll = { selectedAuthors = emptySet() },
            onReset = { selectedAuthors = libraryState.filterState.selectedAuthors },
            onDismiss = { showAuthorsSubpanel = false }
        )
    }

    // Series subpanel
    if (showSeriesSubpanel) {
        LibraryFilterSubpanel(
            title = "Series",
            show = showSeriesSubpanel,
            items = sortedSeries.toSet(),
            selectedItems = selectedSeries,
            onItemToggle = { item, selected ->
                selectedSeries = if (selected) selectedSeries + item else selectedSeries - item
                updateFilterState()
            },
            onSelectAll = { selectedSeries = sortedSeries.toSet() },
            onDeselectAll = { selectedSeries = emptySet() },
            onReset = { selectedSeries = libraryState.filterState.selectedSeries },
            onDismiss = { showSeriesSubpanel = false }
        )
    }
}

@Composable
private fun LibrarySortTabContent(
    state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>,
    mainModel: MainModel
) {
    // Sort options grouped together
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        LibrarySortOrder.entries.forEach { sortOrder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (state.value.librarySortOrder == sortOrder) {
                            mainModel.onEvent(
                                MainEvent.OnChangeLibrarySortOrderDescending(
                                    !state.value.librarySortOrderDescending
                                )
                            )
                        } else {
                            mainModel.onEvent(MainEvent.OnChangeLibrarySortOrderDescending(true))
                            mainModel.onEvent(MainEvent.OnChangeLibrarySortOrder(sortOrder.name))
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.value.librarySortOrder == sortOrder) {
                        if (state.value.librarySortOrderDescending) Icons.Default.ArrowDownward
                        else Icons.Default.ArrowUpward
                    } else {
                        Icons.Default.ArrowUpward
                    },
                    contentDescription = stringResource(id = R.string.sort_order_content_desc),
                    modifier = Modifier.size(24.dp),
                    tint = if (state.value.librarySortOrder == sortOrder) MaterialTheme.colorScheme.secondary
                    else Color.Transparent
                )

                Spacer(modifier = Modifier.width(24.dp))

                StyledText(
                    text = stringResource(
                        when (sortOrder) {
                            LibrarySortOrder.NAME -> R.string.library_sort_order_name
                            LibrarySortOrder.LAST_READ -> R.string.library_sort_order_last_read
                            LibrarySortOrder.PROGRESS -> R.string.library_sort_order_progress
                            LibrarySortOrder.AUTHOR -> R.string.library_sort_order_author
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LibraryDisplayTabContent(
    state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>,
    mainModel: MainModel
) {
    // Display options
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        LibraryLayoutOption()

        // Size slider - content changes based on layout
        if (state.value.libraryLayout == LibraryLayout.GRID) {
            LibraryGridSizeOption()
        } else {
            LibraryListSizeOption()
        }

        // Title Position (only visible in Grid mode)
        if (state.value.libraryLayout == LibraryLayout.GRID) {
            LibraryTitlePositionOption()
        }

        LibraryShowReadButtonOption()

        LibraryShowProgressOption()
    }
}

@Composable
private fun LibraryFilterTabContent(
    selectedStatuses: Set<String>,
    selectedTags: Set<String>,
    selectedAuthors: Set<String>,
    selectedSeries: Set<String>,
    selectedLanguages: Set<String>,
    sortedTags: List<String>,
    sortedAuthors: List<String>,
    sortedSeries: List<String>,
    sortedLanguages: List<String>,
    onStatusToggle: (String, Boolean) -> Unit,
    onShowTagsSubpanel: () -> Unit,
    onShowAuthorsSubpanel: () -> Unit,
    onShowSeriesSubpanel: () -> Unit,
    onLanguageToggle: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Status Presets", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statuses = listOf("Reading", "Planning", "Already Read")
            statuses.forEach { status ->
                FilterChip(
                    selected = status in selectedStatuses,
                    onClick = { onStatusToggle(status, status !in selectedStatuses) },
                    label = { Text(status) }
                )
            }
        }

        Text("Tags", style = MaterialTheme.typography.titleSmall)
        if (sortedTags.isNotEmpty()) {
            OutlinedButton(
                onClick = onShowTagsSubpanel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${selectedTags.size} selected")
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Text("No tags available", style = MaterialTheme.typography.bodySmall)
        }

        Text("Authors", style = MaterialTheme.typography.titleSmall)
        if (sortedAuthors.isNotEmpty()) {
            OutlinedButton(
                onClick = onShowAuthorsSubpanel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${selectedAuthors.size} selected")
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Text("No authors available", style = MaterialTheme.typography.bodySmall)
        }

        Text("Series", style = MaterialTheme.typography.titleSmall)
        if (sortedSeries.isNotEmpty()) {
            OutlinedButton(
                onClick = onShowSeriesSubpanel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${selectedSeries.size} selected")
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Text("No series available", style = MaterialTheme.typography.bodySmall)
        }

        Text("Language", style = MaterialTheme.typography.titleSmall)
        if (sortedLanguages.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedLanguages.forEach { language ->
                    FilterChip(
                        selected = selectedLanguages.contains(language),
                        onClick = { onLanguageToggle(language, !selectedLanguages.contains(language)) },
                        label = { Text(language) }
                    )
                }
            }
        } else {
            Text("No languages available", style = MaterialTheme.typography.bodySmall)
        }
    }
}