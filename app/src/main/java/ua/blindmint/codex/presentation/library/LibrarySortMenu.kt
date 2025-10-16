/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.library.display.LibraryLayout
import ua.blindmint.codex.domain.library.sort.LibrarySortOrder
import ua.blindmint.codex.domain.ui.ButtonItem
import ua.blindmint.codex.presentation.core.components.common.IconButton
import ua.blindmint.codex.presentation.core.components.common.StyledText
import ua.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import ua.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import ua.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import ua.blindmint.codex.ui.main.MainEvent
import ua.blindmint.codex.ui.main.MainModel
import ua.blindmint.codex.ui.theme.ExpandingTransition

@Composable
fun LibrarySortMenu(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 8.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                LibrarySortMenuContent(
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun LibrarySortMenuContent(
    onDismiss: () -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.sort_settings),
        stringResource(R.string.display_settings)
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledText(
                text = stringResource(R.string.library_sort_and_display),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            indicator = { tabPositions ->
                TabRowDefaults.PrimaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Content based on selected tab
        Column(
            Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            when (selectedTabIndex) {
                0 -> LibrarySortTabContent()
                1 -> LibraryDisplayTabContent()
            }
        }
    }
}

@Composable
private fun LibrarySortTabContent() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    LazyColumn {
        items(LibrarySortOrder.entries.size) { index ->
            val sortOrder = LibrarySortOrder.entries[index]
            val isSelected = state.value.librarySortOrder == sortOrder

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable {
                        mainModel.onEvent(MainEvent.OnChangeLibrarySortOrder(sortOrder.name))
                        if (isSelected) {
                            mainModel.onEvent(
                                MainEvent.OnChangeLibrarySortOrderDescending(
                                    !state.value.librarySortOrderDescending
                                )
                            )
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledText(
                    text = when (sortOrder) {
                        LibrarySortOrder.NAME -> stringResource(R.string.library_sort_order_name)
                        LibrarySortOrder.LAST_READ -> stringResource(R.string.library_sort_order_last_read)
                        LibrarySortOrder.PROGRESS -> stringResource(R.string.library_sort_order_progress)
                        LibrarySortOrder.AUTHOR -> stringResource(R.string.library_sort_order_author)
                    },
                    modifier = Modifier.weight(1f)
                )

                if (isSelected) {
                    androidx.compose.material3.Icon(
                        imageVector = if (state.value.librarySortOrderDescending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.sort_order_content_desc)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDisplayTabContent() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    LazyColumn {
        // Layout Selection
        item {
            SegmentedButtonWithTitle(
                title = stringResource(id = R.string.layout_option),
                buttons = LibraryLayout.entries.map {
                    ua.blindmint.codex.domain.ui.ButtonItem(
                        id = it.name,
                        title = stringResource(
                            when (it) {
                                LibraryLayout.LIST -> R.string.layout_list
                                LibraryLayout.GRID -> R.string.layout_grid
                            }
                        ),
                        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        selected = state.value.libraryLayout == it
                    )
                },
                onClick = {
                    mainModel.onEvent(
                        MainEvent.OnChangeLibraryLayout(
                            LibraryLayout.entries[LibraryLayout.entries.indexOfFirst { layout ->
                                layout.name == it.id
                            }]
                        )
                    )
                }
            )
        }

        // Grid Size Slider (only show when Grid layout is selected)
        item {
            ExpandingTransition(visible = state.value.libraryLayout == LibraryLayout.GRID) {
                SliderWithTitle(
                    value = state.value.libraryGridSize
                            to " ${stringResource(R.string.library_grid_size_per_row)}",
                    valuePlaceholder = stringResource(id = R.string.library_grid_size_auto),
                    showPlaceholder = state.value.libraryAutoGridSize,
                    fromValue = 0,
                    toValue = 8,
                    title = stringResource(id = R.string.library_grid_size_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeLibraryAutoGridSize(it == 0))
                        mainModel.onEvent(
                            MainEvent.OnChangeLibraryGridSize(it)
                        )
                    }
                )
            }
        }

        // Tab Behavior Settings
        item {
            Spacer(Modifier.height(16.dp))
            StyledText(
                text = stringResource(R.string.tabs_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            SwitchWithTitle(
                selected = state.value.libraryShowCategoryTabs,
                title = stringResource(id = R.string.show_category_tabs_option),
                onClick = {
                    mainModel.onEvent(MainEvent.OnChangeLibraryShowCategoryTabs(!state.value.libraryShowCategoryTabs))
                }
            )
        }


        item {
            SwitchWithTitle(
                selected = state.value.libraryShowBookCount,
                title = stringResource(id = R.string.show_book_count_option),
                onClick = {
                    mainModel.onEvent(MainEvent.OnChangeLibraryShowBookCount(!state.value.libraryShowBookCount))
                }
            )
        }

        // Add spacer to match height of Sort tab
        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}