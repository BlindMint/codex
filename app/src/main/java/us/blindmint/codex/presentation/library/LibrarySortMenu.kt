/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * LibrarySortMenu.kt - Library sorting and display options dialog
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.domain.library.sort.LibrarySortOrder
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.presentation.settings.library.display.components.LibraryGridSizeOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryLayoutOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryListSizeOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryShowProgressOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryShowReadButtonOption
import us.blindmint.codex.presentation.settings.library.display.components.LibraryTitlePositionOption
import us.blindmint.codex.presentation.settings.library.sort.components.LibrarySortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortMenu(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        sheetState = rememberModalBottomSheetState(),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
    ) {
        LibrarySortMenuContent(
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun LibrarySortMenuContent(
    onDismiss: () -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

        // Content - single scrollable area with grouped sections
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sort section - grouped in a card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.sort_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Sort options
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
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.value.librarySortOrderDescending) Icons.Default.ArrowDownward
                                    else Icons.Default.ArrowUpward,
                                    contentDescription = stringResource(id = R.string.sort_order_content_desc),
                                    modifier = Modifier.size(28.dp),
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
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Display section - grouped in a card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.display_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
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
            }
        }
    }
}