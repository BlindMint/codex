/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.domain.library.sort.LibrarySortOrder
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
private fun LibraryDisplayCard() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.display_settings),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            us.blindmint.codex.presentation.settings.library.display.components.LibraryLayoutOption()

            // Size slider - content changes based on layout
            if (state.value.libraryLayout == LibraryLayout.GRID) {
                us.blindmint.codex.presentation.settings.library.display.components.LibraryGridSizeOption()
            } else {
                us.blindmint.codex.presentation.settings.library.display.components.LibraryListSizeOption()
            }

            // Title Position (only visible in Grid mode)
            if (state.value.libraryLayout == LibraryLayout.GRID) {
                us.blindmint.codex.presentation.settings.library.display.components.LibraryTitlePositionOption()
            }

            us.blindmint.codex.presentation.settings.library.display.components.LibraryShowReadButtonOption()
            us.blindmint.codex.presentation.settings.library.display.components.LibraryShowProgressOption()
        }
    }
}

@Composable
private fun LibraryTabsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.tabs_settings),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            us.blindmint.codex.presentation.settings.library.tabs.components.LibraryShowCategoryTabsOption()
            us.blindmint.codex.presentation.settings.library.tabs.components.LibraryShowBookCountOption()
            us.blindmint.codex.presentation.settings.library.tabs.components.LibraryAlwaysShowDefaultTabOption()
        }
    }
}

@Composable
private fun LibrarySortCard() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.sort_settings),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LibrarySortOptions(state, mainModel)
        }
    }
}

@Composable
private fun LibrarySortOptions(state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>, mainModel: MainModel) {
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

fun LazyListScope.LibrarySettingsCategory() {

    // Display section - grouped in a card
    item {
        LibraryDisplayCard()
    }

    // Tabs section - grouped in a card
    item {
        LibraryTabsCard()
    }

    // Sort section - grouped in a card
    item {
        LibrarySortCard()
    }
}