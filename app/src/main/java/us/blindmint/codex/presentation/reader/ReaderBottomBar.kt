/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.reader.Checkpoint
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.util.Direction
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.theme.HorizontalExpandingTransition
import us.blindmint.codex.ui.theme.readerBarsColor

@Composable
fun ReaderBottomBar(
    book: Book,
    progress: String,
    text: List<ReaderText>,
    listState: LazyListState,
    lockMenu: Boolean,
    checkpoint: Checkpoint,
    bottomBarPadding: Dp,
    restoreCheckpoint: (ReaderEvent.OnRestoreCheckpoint) -> Unit,
    scroll: (ReaderEvent.OnScroll) -> Unit,
    changeProgress: (ReaderEvent.OnChangeProgress) -> Unit,
    currentComicPage: Int = 0,
    totalComicPages: Int = 0,
    onComicPageSelected: (Int) -> Unit = {},
    comicProgressBar: Boolean = true,
    comicProgressCount: us.blindmint.codex.domain.reader.ReaderProgressCount = us.blindmint.codex.domain.reader.ReaderProgressCount.PAGE,
    comicProgressBarPadding: Dp = 4.dp,
    comicProgressBarAlignment: us.blindmint.codex.domain.util.HorizontalAlignment = us.blindmint.codex.domain.util.HorizontalAlignment.CENTER,
    comicProgressBarFontSize: androidx.compose.ui.unit.TextUnit = 8.sp,
    comicReadingDirection: String = "LTR"
) {
    val firstVisibleItemIndex = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }
    val arrowDirection = remember(checkpoint.index, firstVisibleItemIndex) {
        derivedStateOf {
            val checkpointIndex = checkpoint.index
            val index = firstVisibleItemIndex.value

            when {
                checkpointIndex > index -> Direction.END
                checkpointIndex < index -> Direction.START
                else -> Direction.NEUTRAL
            }
        }
    }
    val checkpointProgress = remember(checkpoint.index, text.lastIndex, book.isPageBased) {
        derivedStateOf {
            if (book.isPageBased || text.isEmpty()) {
                0f // Page-based formats don't use text-based checkpoints
            } else {
                (checkpoint.index / text.lastIndex.toFloat()) * 0.987f
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .noRippleClickable(onClick = {})
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))

        // For books, show the progress text. For page-based formats, progress is handled by ReaderBottomBarComicSlider
        if (!book.isPageBased) {
            StyledText(
                text = progress,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(Modifier.height(6.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalExpandingTransition(
                visible = arrowDirection.value == Direction.START,
                startDirection = true
            ) {
                IconButton(
                    icon = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = R.string.checkpoint_back_content_desc,
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    disableOnClick = false
                ) {
                    restoreCheckpoint(ReaderEvent.OnRestoreCheckpoint)
                }
            }

            // Only show the progress slider/controls if:
            // - For books: always show
            // - For page-based: only show if comicProgressBar is enabled
            if (!book.isPageBased || comicProgressBar) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (book.isPageBased) {
                        ReaderBottomBarComicSlider(
                            currentPage = currentComicPage,
                            totalPages = totalComicPages,
                            lockMenu = lockMenu,
                            onPageSelected = onComicPageSelected,
                            showProgressBar = comicProgressBar,
                            progressCount = comicProgressCount,
                            progressBarPadding = comicProgressBarPadding,
                            progressBarAlignment = comicProgressBarAlignment,
                            progressBarFontSize = comicProgressBarFontSize,
                            comicReadingDirection = comicReadingDirection
                        )
                    } else {
                        ReaderBottomBarSlider(
                            book = book,
                            lockMenu = lockMenu,
                            listState = listState,
                            scroll = scroll,
                            changeProgress = changeProgress
                        )

                        if (arrowDirection.value != Direction.NEUTRAL) {
                            ReaderBottomBarSliderIndicator(progress = checkpointProgress.value)
                        }
                    }
                }
            }

            HorizontalExpandingTransition(
                visible = arrowDirection.value == Direction.END,
                startDirection = false
            ) {
                IconButton(
                    icon = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = R.string.checkpoint_forward_content_desc,
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    disableOnClick = false
                ) {
                    restoreCheckpoint(ReaderEvent.OnRestoreCheckpoint)
                }
            }
        }

        Spacer(Modifier.height(8.dp + bottomBarPadding))
    }
}