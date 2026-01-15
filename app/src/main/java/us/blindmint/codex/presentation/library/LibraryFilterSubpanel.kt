/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.presentation.core.components.modal_drawer.DrawerSide

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryFilterSubpanel(
    title: String,
    show: Boolean,
    items: Set<String>,
    selectedItems: Set<String>,
    onItemToggle: (String, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalDrawer(
        show = show,
        side = DrawerSide.LEFT,
        onDismissRequest = onDismiss,
        header = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                actions = {
                    TextButton(onClick = onReset) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        }
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select All", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = onDeselectAll,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deselect All", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Chips
                if (items.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items.sorted().forEach { item ->
                            FilterChip(
                                selected = item in selectedItems,
                                onClick = {
                                    onItemToggle(item, item !in selectedItems)
                                },
                                label = { Text(item) }
                            )
                        }
                    }
                } else {
                    Text(
                        "No items available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}