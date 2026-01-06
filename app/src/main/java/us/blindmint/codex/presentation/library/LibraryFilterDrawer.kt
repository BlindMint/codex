/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawerTitleItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryFilterDrawer(
    show: Boolean,
    onDismiss: () -> Unit
) {
    val statuses = listOf("Reading", "Planning", "Already Read", "Favorites")
    var selectedStatuses by remember { mutableStateOf(setOf<String>()) }
    var yearRange by remember { mutableStateOf(1900f..2026f) }

    ModalDrawer(
        show = show,
        side = us.blindmint.codex.presentation.core.components.modal_drawer.DrawerSide.LEFT,
        onDismissRequest = onDismiss,
        header = {
            TopAppBar(
                title = { Text(stringResource(R.string.filter_title)) },
                actions = {
                    Button(onClick = {
                        selectedStatuses = emptySet()
                        yearRange = 1900f..2026f
                        // TODO: Clear other filters
                    }) {
                        Text("Clear All")
                    }
                }
            )
        },
        footer = {
            Button(onClick = onDismiss) {
                Text("Apply")
            }
        }
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Status Presets", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statuses.forEach { status ->
                        FilterChip(
                            selected = status in selectedStatuses,
                            onClick = {
                                selectedStatuses = if (status in selectedStatuses) {
                                    selectedStatuses - status
                                } else {
                                    selectedStatuses + status
                                }
                            },
                            label = { Text(status) }
                        )
                    }
                }

                Text("Tags", style = MaterialTheme.typography.titleSmall)
                Text("No tags available", style = MaterialTheme.typography.bodySmall)

                Text("Authors", style = MaterialTheme.typography.titleSmall)
                Text("No authors available", style = MaterialTheme.typography.bodySmall)

                Text("Series", style = MaterialTheme.typography.titleSmall)
                Text("No series available", style = MaterialTheme.typography.bodySmall)

                Text("Publication Year", style = MaterialTheme.typography.titleSmall)
                RangeSlider(
                    value = yearRange,
                    onValueChange = { yearRange = it },
                    valueRange = 1900f..2026f,
                    steps = 126
                )
                Text("${yearRange.start.toInt()} - ${yearRange.endInclusive.toInt()}")

                Text("Language", style = MaterialTheme.typography.titleSmall)
                Text("No languages available", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}