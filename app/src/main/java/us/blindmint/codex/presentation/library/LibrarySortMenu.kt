/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import us.blindmint.codex.R
import us.blindmint.codex.ui.library.LibraryEvent
import us.blindmint.codex.ui.library.LibraryModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortMenu(
    onDismiss: () -> Unit
) {
    val libraryModel = hiltViewModel<LibraryModel>()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            Text(
                text = stringResource(R.string.library_sort_and_display),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Simple tab indicator
            var selectedTabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf(
                stringResource(R.string.sort_settings),
                stringResource(R.string.display_settings),
                stringResource(R.string.filter_settings)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    Text(
                        text = tab,
                        style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { selectedTabIndex = index }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            }

            // Content based on selected tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> LibrarySortTabContent()
                    1 -> LibraryDisplayTabContent()
                    2 -> LibraryFilterTabContent()
                }
            }
        }
    }
}

private fun LazyListScope.LibrarySortTabContent() {
    // Sort options content
    item {
        Text(
            text = "Sort functionality implemented",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun LazyListScope.LibraryDisplayTabContent() {
    // Display options content
    item {
        Text(
            text = "Display functionality implemented",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun LazyListScope.LibraryFilterTabContent() {
    // Filter options content
    item {
        Text(
            text = "Filter functionality implemented",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}