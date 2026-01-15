/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

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
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.domain.library.display.LibraryTitlePosition
import us.blindmint.codex.domain.library.sort.LibrarySortOrder
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.library.LibraryModel
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.theme.ExpandingTransition

@Composable
fun LibrarySortMenu(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
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
        stringResource(R.string.display_settings),
        stringResource(R.string.filter_settings)
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
                2 -> LibraryFilterTabContent()
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
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
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
                androidx.compose.material3.Icon(
                    imageVector = if (state.value.librarySortOrderDescending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.sort_order_content_desc),
                    tint = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
                )

                Spacer(modifier = Modifier.width(24.dp))

                StyledText(
                    text = when (sortOrder) {
                        LibrarySortOrder.NAME -> stringResource(R.string.library_sort_order_name)
                        LibrarySortOrder.LAST_READ -> stringResource(R.string.library_sort_order_last_read)
                        LibrarySortOrder.PROGRESS -> stringResource(R.string.library_sort_order_progress)
                        LibrarySortOrder.AUTHOR -> stringResource(R.string.library_sort_order_author)
                    },
                    modifier = Modifier.weight(1f)
                )
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
                    us.blindmint.codex.domain.ui.ButtonItem(
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

        // Size Slider (always visible, content changes based on layout)
        item {
            if (state.value.libraryLayout == LibraryLayout.GRID) {
                SliderWithTitle(
                    value = state.value.libraryGridSize to " ${stringResource(R.string.library_grid_size_per_row)}",
                    valuePlaceholder = stringResource(id = R.string.library_grid_size_auto),
                    showPlaceholder = state.value.libraryAutoGridSize,
                    fromValue = 0,
                    toValue = 8,
                    title = stringResource(id = R.string.library_grid_size_option),
                    onValueChange = { value ->
                        mainModel.onEvent(MainEvent.OnChangeLibraryAutoGridSize(value == 0))
                        mainModel.onEvent(
                            MainEvent.OnChangeLibraryGridSize(value)
                        )
                    }
                )
            } else {
                SliderWithTitle(
                    value = state.value.libraryListSize to "",
                    valuePlaceholder = when (state.value.libraryListSize) {
                        0 -> stringResource(R.string.library_list_size_small)
                        1 -> stringResource(R.string.library_list_size_medium)
                        2 -> stringResource(R.string.library_list_size_large)
                        else -> stringResource(R.string.library_list_size_medium)
                    },
                    showPlaceholder = true,
                    fromValue = 0,
                    toValue = 2,
                    title = stringResource(id = R.string.library_list_size_option),
                    onValueChange = { value ->
                        mainModel.onEvent(MainEvent.OnChangeLibraryListSize(value))
                    }
                )
            }
        }

        // Title Position (only visible in Grid mode)
        item {
            ExpandingTransition(visible = state.value.libraryLayout == LibraryLayout.GRID) {
                SegmentedButtonWithTitle(
                    title = stringResource(id = R.string.library_title_position_option),
                    buttons = LibraryTitlePosition.entries.map {
                        us.blindmint.codex.domain.ui.ButtonItem(
                            id = it.name,
                            title = stringResource(
                                when (it) {
                                    LibraryTitlePosition.BELOW -> R.string.library_title_position_below
                                    LibraryTitlePosition.HIDDEN -> R.string.library_title_position_hidden
                                }
                            ),
                            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            selected = state.value.libraryTitlePosition == it
                        )
                    },
                    onClick = {
                        mainModel.onEvent(
                            MainEvent.OnChangeLibraryTitlePosition(
                                LibraryTitlePosition.valueOf(it.id)
                            )
                        )
                    }
                )
            }
        }

        item {
            SwitchWithTitle(
                selected = state.value.libraryShowReadButton,
                title = stringResource(id = R.string.library_show_read_button_option),
                onClick = {
                    mainModel.onEvent(MainEvent.OnChangeLibraryShowReadButton(!state.value.libraryShowReadButton))
                }
            )
        }

        item {
            SwitchWithTitle(
                selected = state.value.libraryShowProgress,
                title = stringResource(id = R.string.library_show_progress_option),
                onClick = {
                    mainModel.onEvent(MainEvent.OnChangeLibraryShowProgress(!state.value.libraryShowProgress))
                }
            )
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

@Composable
private fun LibraryFilterTabContent() {
    val libraryModel = hiltViewModel<LibraryModel>()
    val libraryState = libraryModel.state.collectAsStateWithLifecycle()

    LazyColumn {
        // Tags section
        item {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        // Authors section
        item {
            Text(
                text = "Authors",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        // Series section
        item {
            Text(
                text = "Series",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        // Publication Year section
        item {
            Text(
                text = "Publication Year",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        item {
            Text(
                text = "Filter functionality will be implemented with metadata chips and year slider",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
