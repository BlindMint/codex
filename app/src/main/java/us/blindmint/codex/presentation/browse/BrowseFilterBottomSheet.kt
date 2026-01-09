/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.constants.provideExtensions
import us.blindmint.codex.presentation.settings.browse.display.components.BrowseGridSizeOption
import us.blindmint.codex.presentation.settings.browse.display.components.BrowseLayoutOption
import us.blindmint.codex.ui.browse.BrowseEvent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseFilterBottomSheet(
    dismissBottomSheet: (BrowseEvent.OnDismissBottomSheet) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { dismissBottomSheet(BrowseEvent.OnDismissBottomSheet) },
        modifier = Modifier.fillMaxWidth(),
        sheetState = rememberModalBottomSheetState(),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
    ) {
        BrowseFilterDialogContent(
            dismissBottomSheet = dismissBottomSheet
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseFilterDialogContent(
    dismissBottomSheet: (BrowseEvent.OnDismissBottomSheet) -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val mainState = mainModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.display_and_filter),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        // Content - single scrollable area with grouped sections
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .height(500.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        BrowseLayoutOption()
                        BrowseGridSizeOption()
                    }
                }
            }

            // Filter section - grouped in a card with multi-select chips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.filter_browse_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            provideExtensions().forEach { extension ->
                                androidx.compose.material3.FilterChip(
                                    selected = mainState.value.browseIncludedFilterItems.any { it == extension },
                                    onClick = {
                                        mainModel.onEvent(
                                            MainEvent.OnChangeBrowseIncludedFilterItem(extension)
                                        )
                                    },
                                    label = { androidx.compose.material3.Text(extension.uppercase()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

