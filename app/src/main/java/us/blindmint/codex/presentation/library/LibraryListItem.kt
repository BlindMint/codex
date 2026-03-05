/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.SelectableBook
import us.blindmint.codex.presentation.core.components.common.AsyncCoverImage
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.calculateProgress

@Composable
fun LibraryListItem(
    book: SelectableBook,
    hasSelectedItems: Boolean,
    selectBook: (select: Boolean?) -> Unit,
    navigateToBookInfo: () -> Unit,
    navigateToReader: () -> Unit,
    modifier: Modifier = Modifier,
    navigateToSpeedReading: () -> Unit = {},
    listSize: Int = 1, // 0=Small, 1=Medium, 2=Large
    showNormalProgress: Boolean = true,
    showSpeedProgress: Boolean = true
) {
    val backgroundColor = if (book.selected) MaterialTheme.colorScheme.secondary
    else Color.Transparent
    val fontColor = if (book.selected) MaterialTheme.colorScheme.onSecondary
    else MaterialTheme.colorScheme.onSurface

    val progress = rememberSaveable(book.data.displayProgress) {
        "${book.data.displayProgress.calculateProgress(1)}%"
    }

    // Size configurations based on listSize
    val coverSize = when (listSize) {
        0 -> 60.dp // Small
        1 -> 80.dp // Medium
        2 -> 100.dp // Large
        else -> 80.dp
    }

    val padding = when (listSize) {
        0 -> 6.dp // Small
        1 -> 8.dp // Medium
        2 -> 10.dp // Large
        else -> 8.dp
    }

    val innerPadding = when (listSize) {
        0 -> 8.dp // Small
        1 -> 12.dp // Medium
        2 -> 16.dp // Large
        else -> 12.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .clip(RoundedCornerShape(12.dp))
            .background(
                backgroundColor,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = {
                    if (hasSelectedItems) selectBook(null)
                    else navigateToBookInfo()
                },
                onLongClick = {
                    if (!hasSelectedItems) selectBook(true)
                }
            )
            .padding(innerPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover image on the left
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.medium
                )
        ) {
            if (book.data.coverImage != null) {
                AsyncCoverImage(
                    uri = book.data.coverImage,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = stringResource(
                        id = R.string.cover_image_not_found_content_desc
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f),
                    tint = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            // Progress buttons
            if (showNormalProgress) {
                val showSpeedButton = !book.data.isComic &&
                        book.data.speedReaderHasBeenOpened &&
                        showSpeedProgress

                val speedProgress = remember(book.data.speedReaderWordIndex, book.data.speedReaderTotalWords) {
                    if (book.data.speedReaderTotalWords > 0) {
                        val raw = if (book.data.speedReaderWordIndex >= book.data.speedReaderTotalWords - 1) 1f
                            else book.data.speedReaderWordIndex.toFloat() / book.data.speedReaderTotalWords
                        "${raw.calculateProgress(1)}%"
                    } else {
                        "0%"
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 3.dp, end = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Normal reader button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                            .clickable { navigateToReader() }
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                            Text(
                                text = progress,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.8f
                                ),
                                color = MaterialTheme.colorScheme.onSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Speed reader button or empty spacer
                    if (showSpeedButton) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                                .clickable { navigateToSpeedReading() }
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                                Text(
                                    text = speedProgress,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.8f
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title and info on the right
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            StyledText(
                text = book.data.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = fontColor
                ),
                maxLines = 2
            )
        }
    }
}