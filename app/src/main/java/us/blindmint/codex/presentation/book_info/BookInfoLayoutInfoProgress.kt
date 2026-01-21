/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.calculateProgress

@Composable
fun BookInfoLayoutInfoProgress(
    book: Book
) {
    val normalProgress = remember(book.progress) {
        "${book.progress.calculateProgress(1)}%"
    }

    // Calculate speed reader progress as word index / total words estimate
    // Since we don't have total words here, we'll use a placeholder
    // In a real implementation, you'd want to calculate or store total words
    val speedProgress = remember(book.speedReaderWordIndex) {
        if (book.speedReaderWordIndex > 0) {
            // Placeholder calculation - in practice you'd need total words
            // For now, just show the word index as a percentage representation
            "${(book.speedReaderWordIndex / 1000f * 100).toInt()}%" // Rough estimate
        } else {
            "0%"
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Normal reader progress
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { book.progress.coerceIn(0f, 1f) },
                trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.7f),
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Normal reader progress",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            StyledText(
                text = normalProgress,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
            )
        }

        // Speed reader progress (only show if book has been opened in speed reader)
        if (book.speedReaderHasBeenOpened) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (book.speedReaderWordIndex / 1000f).coerceIn(0f, 1f) }, // Placeholder calculation
                    trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.7f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                )
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = "Speed reader progress",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                StyledText(
                    text = speedProgress,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                )
            }
        }
    }
}