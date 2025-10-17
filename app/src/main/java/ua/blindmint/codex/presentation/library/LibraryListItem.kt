/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.library.book.SelectableBook
import ua.blindmint.codex.presentation.core.components.common.AsyncCoverImage
import ua.blindmint.codex.presentation.core.components.common.StyledText
import ua.blindmint.codex.presentation.core.util.calculateProgress
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LibraryListItem(
    book: SelectableBook,
    hasSelectedItems: Boolean,
    selectBook: (select: Boolean?) -> Unit,
    navigateToBookInfo: () -> Unit,
    navigateToReader: () -> Unit,
    modifier: Modifier = Modifier,
    listSize: Int = 1 // 0=Small, 1=Medium, 2=Large
) {
    val backgroundColor = if (book.selected) MaterialTheme.colorScheme.secondary
    else Color.Transparent
    val fontColor = if (book.selected) MaterialTheme.colorScheme.onSecondary
    else MaterialTheme.colorScheme.onSurface

    val progress = rememberSaveable(book.data.progress) {
        "${book.data.progress.calculateProgress(1)}%"
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

            // Progress indicator
            val mainModel = androidx.hilt.navigation.compose.hiltViewModel<ua.blindmint.codex.ui.main.MainModel>()
            val mainState = mainModel.state.collectAsStateWithLifecycle()
            if (mainState.value.libraryShowProgress) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StyledText(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.8f
                        )
                    )
                }
            }

            // Play button
            if (mainState.value.libraryShowReadButton) {
                FilledIconButton(
                    onClick = { navigateToReader() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(24.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(id = R.string.continue_reading_content_desc),
                        Modifier.size(16.dp)
                    )
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