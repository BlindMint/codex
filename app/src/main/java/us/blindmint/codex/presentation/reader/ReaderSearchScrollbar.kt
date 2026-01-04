/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SearchResult
import kotlin.math.max
import kotlin.math.min

@Composable
fun ReaderSearchScrollbar(
    text: List<ReaderText>,
    listState: LazyListState,
    searchResults: List<SearchResult>,
    currentSearchResultIndex: Int,
    searchScrollbarOpacity: Float,
    searchHighlightColor: Color,
    showMenu: Boolean,
    onScrollToSearchResult: (Int) -> Unit
) {
    if (searchResults.isEmpty()) return

    var scrollbarSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Calculate viewport indicator position and size
    val viewportStartRatio = remember(listState.firstVisibleItemIndex, text.size) {
        if (text.isEmpty()) 0f else listState.firstVisibleItemIndex.toFloat() / text.size.toFloat()
    }

    val visibleItems = listState.layoutInfo.visibleItemsInfo.size
    val viewportHeightRatio = remember(visibleItems, text.size) {
        if (text.isEmpty()) 0f else min(visibleItems.toFloat() / text.size.toFloat(), 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(searchScrollbarOpacity)
    ) {
        // Scrollbar background with touch handling
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(64.dp) // ~25% of screen width
                .fillMaxHeight()
                .alpha(0.1f)
                .background(Color.Black)
                .onSizeChanged { scrollbarSize = it }
                .pointerInput(searchResults) {
                    detectTapGestures { offset ->
                        val scrollbarHeight = scrollbarSize.height.toFloat()
                        if (scrollbarHeight > 0) {
                            val tapY = offset.y
                            val relativePosition = tapY / scrollbarHeight

                            // Find the closest search result to this position
                            val targetIndex = (relativePosition * searchResults.size).toInt()
                                .coerceIn(0, searchResults.lastIndex)

                            onScrollToSearchResult(targetIndex)
                        }
                    }
                }
        )

        // Viewport indicator - shows current visible area
        if (viewportHeightRatio > 0f && viewportHeightRatio < 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(64.dp)
                    .offset(y = with(density) { (scrollbarSize.height * viewportStartRatio).toDp() })
                    .height(with(density) {
                        val calculatedHeight = (scrollbarSize.height * viewportHeightRatio).toDp()
                        if (calculatedHeight < 2.dp) 2.dp else calculatedHeight
                    })
                    .background(
                        color = Color.White,
                        shape = MaterialTheme.shapes.small
                    )
                    .alpha(0.4f)
            )
        }

        // Search result highlights - distribute evenly based on result index
        searchResults.forEachIndexed { index, result ->
            val isCurrent = index == currentSearchResultIndex

            // Position based on result index in search results (more accurate for scrolling)
            val positionRatio = if (searchResults.isNotEmpty()) {
                index.toFloat() / (searchResults.size - 1).toFloat()
            } else 0f

            // Visual highlight - no touch handling needed since scrollbar background handles it
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(64.dp)
                    .offset(y = with(density) { (scrollbarSize.height * positionRatio).toDp() })
                    .height(if (isCurrent) 8.dp else 4.dp)
                    .background(
                        color = searchHighlightColor,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}
