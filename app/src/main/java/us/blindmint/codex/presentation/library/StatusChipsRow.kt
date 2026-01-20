/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.library.category.Category

/**
 * Displays status filter chips (Reading, Planning, Already Read) with various modes.
 * Can be used in Filter tabs, bulk edit panels, and book details.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusChipsRow(
    selectedStatuses: Set<Category> = emptySet(),
    onStatusToggle: (Category, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = listOf(Category.READING, Category.PLANNING, Category.ALREADY_READ)

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEach { status ->
            FilterChip(
                selected = status in selectedStatuses,
                onClick = {
                    onStatusToggle(status, status !in selectedStatuses)
                },
                label = {
                    Text(
                        when (status) {
                            Category.READING -> "Reading"
                            Category.PLANNING -> "Planning"
                            Category.ALREADY_READ -> "Already Read"
                            else -> ""
                        }
                    )
                }
            )
        }
    }
}
