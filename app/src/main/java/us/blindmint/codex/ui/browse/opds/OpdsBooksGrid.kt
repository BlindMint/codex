/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.ui.browse.opds.OpdsBookPreview

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
        items(
            count = lazyPagingItems.itemCount,
            key = { index -> lazyPagingItems[index]?.id ?: "" }
        ) { index ->
            val item = lazyPagingItems[index]
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
