/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.browse.scan.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.import_progress.ImportOperation
import us.blindmint.codex.domain.import_progress.ImportStatus

@Composable
fun FolderImportProgress(
    operation: ImportOperation,
    modifier: Modifier = Modifier
) {
    val progressColor = when (operation.status) {
        ImportStatus.STARTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        ImportStatus.SCANNING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        ImportStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        ImportStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        ImportStatus.FAILED -> MaterialTheme.colorScheme.error
        ImportStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }

    AnimatedContent(
        targetState = operation.status to (operation.totalBooks > 0),
        label = "import-progress",
        transitionSpec = {
            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                slideOutVertically { height -> -height } + fadeOut()
            ).using(
                SizeTransform(clip = false)
            )
        },
        modifier = modifier
    ) { (status, hasProgress) ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (status) {
                ImportStatus.STARTING -> {
                    ProgressText(
                        text = "Starting import...",
                        color = progressColor
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.12f),
                    )
                }

                ImportStatus.SCANNING -> {
                    ProgressText(
                        text = "Scanning folder...",
                        color = progressColor
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.12f),
                    )
                }

                ImportStatus.IN_PROGRESS -> {
                    if (hasProgress) {
                        val progress = operation.currentProgress.toFloat() / operation.totalBooks.toFloat()
                        val percentage = (progress * 100).toInt()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ProgressText(
                                text = "$percentage%",
                                color = progressColor,
                                isMonospace = true
                            )

                            ProgressText(
                                text = "${operation.currentProgress}/${operation.totalBooks} files",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (operation.currentFile.isNotEmpty()) {
                            ProgressText(
                                text = operation.currentFile,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.12f),
                        )
                    }
                }

                ImportStatus.COMPLETED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = progressColor,
                                modifier = Modifier.size(16.dp)
                            )
                            ProgressText(
                                text = "Import complete • ${operation.totalBooks} files",
                                color = progressColor
                            )
                        }
                }

                ImportStatus.FAILED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = progressColor,
                                modifier = Modifier.size(16.dp)
                            )
                            ProgressText(
                                text = "Import failed${operation.errorMessage?.let { ": $it" } ?: ""}",
                                color = progressColor
                            )
                        }
                }

                ImportStatus.CANCELLED -> {
                    ProgressText(
                        text = "Import cancelled",
                        color = progressColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    isMonospace: Boolean = false
) {
    val textStyle = if (isMonospace) {
        style.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    } else {
        style
    }

    Text(
        text = text,
        modifier = modifier,
        style = textStyle.copy(
            color = color
        ),
        maxLines = maxLines,
        overflow = overflow
    )
}
