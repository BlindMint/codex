/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.SelectableBook
import us.blindmint.codex.presentation.core.components.common.AsyncCoverImage
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.calculateProgress

@Composable
fun LibraryItem(
    book: SelectableBook,
    hasSelectedItems: Boolean,
    selectBook: (select: Boolean?) -> Unit,
    navigateToBookInfo: () -> Unit,
    navigateToReader: () -> Unit,
    navigateToSpeedReading: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (book.selected) MaterialTheme.colorScheme.secondary
    else Color.Transparent
    val fontColor = if (book.selected) MaterialTheme.colorScheme.onSecondary
    else MaterialTheme.colorScheme.onSurface

    val progress = rememberSaveable(book.data.progress) {
        "${book.data.progress.calculateProgress(1)}%"
    }

    // Get main state for settings
    val mainModel = androidx.hilt.navigation.compose.hiltViewModel<us.blindmint.codex.ui.main.MainModel>()
    val mainState = mainModel.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .padding(3.dp)
            .background(
                backgroundColor,
                MaterialTheme.shapes.large
            )
            .padding(3.dp)
            .padding(bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f / 1.5f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.medium
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

            if (mainState.value.libraryShowProgress) {
                // Normal reader progress (top-left)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StyledText(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    )
                }

                // Speed reader progress (top-right, only if opened)
                if (book.data.speedReaderHasBeenOpened) {
                    val speedProgress = remember(book.data.speedReaderWordIndex) {
                        // Placeholder calculation - in practice you'd calculate based on total words
                        "${(book.data.speedReaderWordIndex / 1000f * 100).toInt()}%"
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Speed reader progress",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        StyledText(
                            text = speedProgress,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onTertiary,
                            )
                        )
                    }
                }
            }

            if (mainState.value.libraryShowReadButton && !book.data.isComic) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    // Main button - Normal Reading
                    FilledIconButton(
                        onClick = { navigateToReader() },
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(id = R.string.continue_reading_content_desc),
                            Modifier.size(20.dp)
                        )
                    }

                    // Separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp * 0.6f)
                            .align(Alignment.CenterVertically)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                    )

                    // Trailing button - Speed Reading
                    FilledIconButton(
                        onClick = { navigateToSpeedReading() },
                        modifier = Modifier.size(24.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Speed Read",
                            Modifier.size(16.dp)
                        )
                    }
                }
            } else if (mainState.value.libraryShowReadButton && book.data.isComic) {
                // Keep existing single button for comics
                FilledIconButton(
                    onClick = { navigateToReader() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(id = R.string.continue_reading_content_desc),
                        Modifier.size(20.dp)
                    )
                }
            }
        }
        // Title display based on title position setting

        when (mainState.value.libraryTitlePosition) {
            us.blindmint.codex.domain.library.display.LibraryTitlePosition.BELOW -> {
                Spacer(modifier = Modifier.height(6.dp))
                StyledText(
                    text = book.data.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = fontColor
                    ),
                    minLines = 2,
                    maxLines = 2
                )
            }
            us.blindmint.codex.domain.library.display.LibraryTitlePosition.HIDDEN -> {
                // No title displayed, no space taken
            }
        }
    }
}
