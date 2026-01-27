/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.items
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.domain.navigator.Navigator
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.browse.opds.model.OpdsCatalogModel
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.browse.OpdsBookPreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsBooksGrid(
    lazyPagingItems: LazyPagingItems<OpdsEntry>,
    source: OpdsSourceEntity,
    onBookClick: (OpdsEntry) -> Unit,
    onBookLongClick: (OpdsEntry) -> Unit,
    isSelectionMode: Boolean,
    toggleBookSelection: (String) -> Unit,
    selectedBooks: Set<String>,
    username: String?,
    password: String?
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(lazyPagingItems) { item ->
            if (item != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OpdsBookPreview(
                        entry = item,
                        baseUrl = "",
                        onClick = {
                            if (isSelectionMode) {
                                toggleBookSelection(item.id)
                            } else {
                                onBookClick(item)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                onBookLongClick(item)
                            }
                        },
                        isSelected = selectedBooks.contains(item.id),
                        isSelectionMode = isSelectionMode,
                        username = username,
                        password = password
                    )
                }
            }
        }

        when (lazyPagingItems.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    val error = (lazyPagingItems.loadState.append as LoadState.Error).error
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error loading more items: ${error.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                }
            }
            }
            is LoadState.NotLoading -> {}
        }
    }
}
