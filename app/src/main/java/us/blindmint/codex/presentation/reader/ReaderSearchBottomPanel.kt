/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CornerSize
import us.blindmint.codex.domain.reader.ReaderProgressCount
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SearchResult
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.components.common.HighlightedText
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.theme.readerBarsColor
import kotlin.math.roundToInt

@Composable
fun ReaderSearchBottomPanel(
    show: Boolean,
    searchResults: List<SearchResult>,
    currentSearchResultIndex: Int,
    text: List<ReaderText>,
    progressCount: ReaderProgressCount,
    bottomBarHeight: Int,
    topBarHeight: Int,
    onScrollToSearchResult: (ReaderEvent.OnScrollToSearchResult) -> Unit
) {
    val density = LocalDensity.current
    val screenHeight = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    // Calculate panel height: from top of bottom bar up to middle of available content space
    val panelHeight by animateDpAsState(
        targetValue = if (show) {
            val screenHeightDp = screenHeight / density.density
            val topBarHeightDp = with(density) { topBarHeight.toDp().toPx() / density.density }
            val bottomBarHeightDp = with(density) { bottomBarHeight.toDp().toPx() / density.density }
            // Panel goes from bottom bar top to the middle of the content area
            val contentAreaMiddle = (screenHeightDp - topBarHeightDp - bottomBarHeightDp) * 0.5f + topBarHeightDp
            val panelBottom = screenHeightDp - bottomBarHeightDp
            (panelBottom - contentAreaMiddle).coerceAtLeast(0f).dp
        } else 0.dp
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = show,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
                    .align(Alignment.BottomCenter)
                    .offset(y = with(density) { -bottomBarHeight.toDp() })
                    .background(
                        MaterialTheme.colorScheme.readerBarsColor
                    )
                    .clip(
                        MaterialTheme.shapes.large.copy(
                            topStart = MaterialTheme.shapes.large.topStart,
                            topEnd = MaterialTheme.shapes.large.topEnd,
                            bottomStart = CornerSize(0.dp),
                            bottomEnd = CornerSize(0.dp)
                        )
                    )
            ) {
                // Results list (no header)
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(searchResults) { index, result ->
                        val isCurrent = index == currentSearchResultIndex

                        // Calculate progress for this result
                        val progressText = calculateProgressForSearchResult(
                            result = result,
                            text = text,
                            progressCount = progressCount
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onScrollToSearchResult(ReaderEvent.OnScrollToSearchResult(index))
                                }
                                .background(
                                    if (isCurrent)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Progress indicator (moved further left)
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(50.dp),
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Search result content with highlighting
                            SearchResultText(
                                beforeContext = result.beforeContext,
                                matchedText = result.matchedText,
                                afterContext = result.afterContext,
                                isCurrent = isCurrent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultText(
    beforeContext: String,
    matchedText: String,
    afterContext: String,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        // Before context (dimmed)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))) {
            append(beforeContext)
        }

        // Matched text (highlighted)
        withStyle(style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )) {
            append(matchedText)
        }

        // After context (dimmed)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))) {
            append(afterContext)
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
}

private fun calculateProgressForSearchResult(
    result: SearchResult,
    text: List<ReaderText>,
    progressCount: ReaderProgressCount
): String {
    if (text.isEmpty()) return ""

    val progress = result.textIndex.toFloat() / text.lastIndex.toFloat()

    return when (progressCount) {
        ReaderProgressCount.PERCENTAGE -> {
            "${(progress * 100).roundToInt()}%"
        }
        ReaderProgressCount.QUANTITY -> {
            "${result.textIndex + 1} / ${text.size}"
        }
        ReaderProgressCount.PAGE -> {
            // Simplified page calculation - in a real implementation,
            // you'd need the same CHARACTERS_PER_PAGE logic as in ReaderScreen
            val totalChars = text.sumOf { readerText ->
                when (readerText) {
                    is ReaderText.Text -> readerText.line.text.length
                    else -> 0
                }
            }
            val currentChars = text.take(result.textIndex + 1).sumOf { readerText ->
                when (readerText) {
                    is ReaderText.Text -> readerText.line.text.length
                    else -> 0
                }
            }
            val currentPage = (currentChars / 2000) + 1 // Approximate characters per page
            val totalPages = (totalChars / 2000) + 1
            "$currentPage / $totalPages"
        }
    }
}