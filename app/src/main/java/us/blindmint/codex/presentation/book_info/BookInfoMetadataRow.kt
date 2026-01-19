/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Displays a row of metadata items as chips (Tags, Authors, Series, Languages)
 * with a title label above them. Entire row is clickable to edit metadata.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookInfoMetadataRow(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    onItemTap: (String) -> Unit = {}
) {
    if (items.isEmpty()) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    AssistChip(
                        onClick = {
                            onItemTap(item)
                        },
                        label = { Text(item) },
                        enabled = true
                    )
                }
            }
        }
    }
}
