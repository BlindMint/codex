/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.import_progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.import_progress.ImportOperation
import us.blindmint.codex.domain.import_progress.ImportStatus

/**
 * Reusable progress bar component for displaying import operations.
 * Shows progress, current file, and allows cancellation.
 */
@Composable
fun ImportProgressBar(
    operation: ImportOperation,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = if (operation.totalBooks > 0) {
        operation.currentProgress.toFloat() / operation.totalBooks.toFloat()
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header with title and cancel button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Importing: ${operation.folderName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${operation.currentProgress}/${operation.totalBooks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (operation.status == ImportStatus.IN_PROGRESS) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel import"
                    )
                }
            }
        }

        // Current file name (limited to 1-2 lines, progress bar always visible)
        if (operation.currentFile.isNotEmpty()) {
            Text(
                text = "Processing: ${operation.currentFile}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Progress bar (always visible at bottom)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        // Status message
        when (operation.status) {
            ImportStatus.STARTING -> {
                Text(
                    text = "Preparing import...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ImportStatus.SCANNING -> {
                Text(
                    text = "Scanning folder...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ImportStatus.COMPLETED -> {
                Text(
                    text = "Import completed successfully",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            ImportStatus.FAILED -> {
                Text(
                    text = "Import failed: ${operation.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            ImportStatus.CANCELLED -> {
                Text(
                    text = "Import cancelled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {}
        }
    }
}
