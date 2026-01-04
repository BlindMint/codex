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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.SearchResult
import kotlin.math.max

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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(searchScrollbarOpacity)
            .onSizeChanged { scrollbarSize = it }
            .pointerInput(searchResults) {
                detectTapGestures { offset ->
                    // Calculate which search result was tapped based on Y position
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
    ) {
        // Scrollbar background
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(48.dp) // 25% of typical screen width approximation
                .fillMaxHeight()
                .alpha(0.1f)
                .background(Color.Black)
        )
        
        // Search result highlights
        searchResults.forEachIndexed { index, result ->
            val isCurrent = index == currentSearchResultIndex
            
            // Calculate position based on text index
            val totalItems = text.size.toFloat()
            val positionRatio = if (totalItems > 0) result.textIndex / totalItems else 0f
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(48.dp)
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
