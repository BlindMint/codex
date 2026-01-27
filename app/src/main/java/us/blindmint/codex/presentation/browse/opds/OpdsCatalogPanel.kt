/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse.opds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import us.blindmint.codex.R
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.browse.display.BrowseLayout
import us.blindmint.codex.domain.browse.file.SelectableFile
import us.blindmint.codex.presentation.core.util.noRippleClickable

import us.blindmint.codex.ui.browse.BrowseEvent
import us.blindmint.codex.ui.browse.OpdsAddSourceDialog
import us.blindmint.codex.ui.browse.OpdsRootScreen
import us.blindmint.codex.ui.browse.opds.model.OpdsCatalogModel
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.settings.BrowseSettingsScreen
import us.blindmint.codex.ui.settings.opds.OpdsSourcesModel
import us.blindmint.codex.ui.settings.opds.OpdsSourcesState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpdsCatalogPanel(
    refreshState: PullRefreshState,
    listState: LazyListState,
    gridState: LazyGridState,
    layout: BrowseLayout,
    isRefreshing: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    focusRequester: FocusRequester,
    searchVisibility: (BrowseEvent.OnSearchVisibility) -> Unit,
    searchQueryChange: (BrowseEvent.OnSearchQueryChange) -> Unit,
    search: (BrowseEvent.OnSearch) -> Unit,
    requestFocus: (BrowseEvent.OnRequestFocus) -> Unit,
    navigateToBrowseSettings: () -> Unit,
    onNavigateToOpdsCatalog: (OpdsRootScreen) -> Unit,
) {
    val sourcesModel = hiltViewModel<OpdsSourcesModel>()
    val sourcesState by sourcesModel.state.collectAsStateWithLifecycle()
    var showAddSourceDialog by remember { mutableStateOf(false) }

    OpdsAddSourceDialog(
        showDialog = showAddSourceDialog,
        onDismiss = { showAddSourceDialog = false }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(refreshState),
        contentAlignment = Alignment.TopCenter
    ) {
        if (sourcesState.sources.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No OPDS sources configured",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Add an OPDS source to start browsing and downloading books from online catalogs.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                OutlinedButton(
                    onClick = {
                        showAddSourceDialog = true
                        // Note: The actual validation happens in OpdsAddSourceDialog
                    }
                ) {
                    Text(stringResource(R.string.add_opds_source))
                }
            }
        } else {
            // Show list of OPDS sources
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item {
                    Text(
                        text = "OPDS Sources",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(sourcesState.sources) { source ->
                    OpdsSourceItem(
                        source = source,
                        onSourceClick = {
                            onNavigateToOpdsCatalog(OpdsRootScreen(source))
                        }
                    )
                }

                item {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OpdsSourceItem(
    source: OpdsSourceEntity,
    onSourceClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable { onSourceClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = source.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!source.usernameEncrypted.isNullOrBlank()) {
                Text(
                    text = "Authenticated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}