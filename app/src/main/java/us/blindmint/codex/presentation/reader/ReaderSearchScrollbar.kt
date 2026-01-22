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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SearchResult
import us.blindmint.codex.ui.theme.readerBarsColor
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
    isSearchVisible: Boolean,
    topBarHeight: Int,
    bottomBarHeight: Int,
    onScrollToPosition: (Int) -> Unit,
    onScrollToSearchResult: (Int) -> Unit
) {

    var scrollbarSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Get system bar insets for when menu is hidden
    val statusBarInsets = WindowInsets.statusBars
    val navBarInsets = WindowInsets.navigationBars

    // Calculate top offset: topBarHeight already includes status bar padding from TopAppBar
    // when menu is visible, so we don't add status bar insets again to avoid double-counting
    val topOffset = with(density) {
        if (showMenu && topBarHeight > 0) {
            // topBarHeight already includes status bar padding from Material 3 TopAppBar
            topBarHeight.toDp()
        } else {
            // When menu is hidden, use just the status bar insets
            statusBarInsets.getTop(density).toDp()
        }
    }

    // Calculate bottom offset: bottomBarHeight already includes navigationBarsPadding
    // when menu is visible, so we don't add nav bar insets again to avoid double-counting
    val bottomOffset = with(density) {
        if (showMenu && bottomBarHeight > 0) {
            // bottomBarHeight already includes nav bar padding from ReaderBottomBar's navigationBarsPadding()
            bottomBarHeight.toDp()
        } else {
            // When menu is hidden, use just the nav bar insets
            navBarInsets.getBottom(density).toDp()
        }
    }

    // Calculate viewport indicator position and size
    val firstVisibleItemIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    val viewportStartRatio = remember(firstVisibleItemIndex, text.size) {
        if (text.isEmpty()) 0f else firstVisibleItemIndex.toFloat() / text.size.toFloat()
    }

    val visibleItemsCount by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size }
    }
    val viewportHeightRatio = remember(visibleItemsCount, text.size) {
        if (text.isEmpty()) 0f else min(visibleItemsCount.toFloat() / text.size.toFloat(),1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topOffset, bottom = bottomOffset)
    ) {
        // Scrollbar background with touch handling - always allows scrolling
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(64.dp) // ~25% of screen width
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.readerBarsColor.copy(alpha = searchScrollbarOpacity))
                .onSizeChanged { scrollbarSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val scrollbarHeight = scrollbarSize.height.toFloat()
                        if (scrollbarHeight > 0 && text.isNotEmpty()) {
                            val tapY = offset.y
                            val relativePosition = tapY / scrollbarHeight

                            // Scroll to position in document based on tap location
                            val targetIndex = (relativePosition * text.size).toInt()
                                .coerceIn(0, text.lastIndex)

                            onScrollToPosition(targetIndex)
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
                        color = Color.White.copy(alpha = searchScrollbarOpacity * 0.6f),
                        shape = MaterialTheme.shapes.small
                    )
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
                        color = searchHighlightColor.copy(alpha = searchScrollbarOpacity),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}
