/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

private const val TAG = "ImageBasedReader"
private const val MAX_CACHED_PAGES = 50
private const val PREFETCH_PAGES = 5

@OptIn(FlowPreview::class)
@Composable
fun ImageBasedReaderLayout(
    bookTitle: String,
    currentPage: Int,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit,
    totalPages: Int,
    contentPadding: PaddingValues,
    backgroundColor: Color,
    readingDirection: String,
    readerMode: String,
    comicScaleType: Int,
    showMenu: Boolean,
    showPageIndicator: Boolean,
    onLoadingComplete: () -> Unit,
    onScrollRestorationComplete: () -> Unit,
    onMenuToggle: () -> Unit,
    onPageSelected: (Int) -> Unit,
    loadPage: suspend (Int) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loadedPages = remember {
        object : LinkedHashMap<Int, Pair<ImageBitmap, Bitmap>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Pair<ImageBitmap, Bitmap>>?): Boolean {
                if (size > MAX_CACHED_PAGES) {
                    eldest?.value?.second?.recycle()
                    return true
                }
                return false
            }
        }
    }

    val isRTL = readingDirection == "RTL"
    val isVertical = readingDirection == "VERTICAL" || readerMode == "WEBTOON"

    val contentScale = remember(comicScaleType) {
        when (comicScaleType) {
            1 -> ContentScale.Fit
            2 -> ContentScale.Crop
            3 -> ContentScale.FillWidth
            4 -> ContentScale.FillHeight
            5 -> ContentScale.None
            6 -> ContentScale.Fit
            else -> ContentScale.Fit
        }
    }

    val mapLogicalToPhysicalPage = { logicalPage: Int ->
        if (isRTL && !isVertical && totalPages > 0) totalPages - 1 - logicalPage else logicalPage
    }

    val mapPhysicalToLogicalPage = { physicalPage: Int ->
        if (isRTL && !isVertical && totalPages > 0) totalPages - 1 - physicalPage else physicalPage
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { maxOf(1, totalPages) }
    )

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = 0)

    // Global zoom state for vertical/webtoon mode - persists across pages
    var verticalZoomScale by remember { mutableFloatStateOf(1f) }
    var verticalOffsetX by remember { mutableFloatStateOf(0f) }
    var verticalOffsetY by remember { mutableFloatStateOf(0f) }

    var storedLogicalPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentPage) {
        storedLogicalPage = currentPage
    }

    LaunchedEffect(readingDirection, totalPages) {
        if (totalPages > 0 && storedLogicalPage >= 0) {
            val targetPhysicalPage = mapLogicalToPhysicalPage(storedLogicalPage)
            if (isVertical) {
                lazyListState.scrollToItem(targetPhysicalPage)
                try {
                    kotlinx.coroutines.flow.flow {
                        while (lazyListState.isScrollInProgress) {
                            emit(Unit)
                        }
                    }.first()
                } catch (e: Exception) {
                }
            } else {
                pagerState.scrollToPage(targetPhysicalPage)
                if (pagerState.isScrollInProgress) {
                    try {
                        kotlinx.coroutines.flow.flow {
                            while (pagerState.isScrollInProgress) {
                                emit(Unit)
                            }
                        }.first()
                    } catch (e: Exception) {
                    }
                }
            }
            onScrollRestorationComplete()
        }
    }

    suspend fun prefetchPages(currentPage: Int) {
        val physicalPage = mapLogicalToPhysicalPage(currentPage)
        for (offset in -PREFETCH_PAGES..PREFETCH_PAGES) {
            val targetPage = physicalPage + offset
            if (targetPage in 0 until totalPages && !loadedPages.containsKey(targetPage)) {
                withContext(Dispatchers.IO) {
                    loadPage(mapPhysicalToLogicalPage(targetPage))
                }
            }
        }
    }

    LaunchedEffect(pagerState, isRTL, isVertical) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .debounce(50)
            .collect { (physicalPage, _) ->
                if (!isVertical && totalPages > 0) {
                    val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                    onPageChanged(logicalPage)
                    prefetchPages(logicalPage)
                }
            }
    }

    LaunchedEffect(lazyListState, isRTL, isVertical) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .debounce(50)
            .collect { physicalIndex ->
                if (isVertical && totalPages > 0) {
                    val logicalPage = mapPhysicalToLogicalPage(physicalIndex)
                    onPageChanged(logicalPage)
                    prefetchPages(logicalPage)
                }
            }
    }

    DisposableEffect(bookTitle) {
        onDispose {
            loadedPages.values.forEach { it.second.recycle() }
            loadedPages.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (totalPages > 0) {
            LaunchedEffect(totalPages) {
                if (initialPage in 0 until totalPages) {
                    val targetPhysicalPage = mapLogicalToPhysicalPage(initialPage)
                    pagerState.scrollToPage(targetPhysicalPage)
                }
            }

            LaunchedEffect(currentPage, totalPages, isRTL) {
                if (currentPage in 0 until totalPages && totalPages > 0) {
                    val targetPhysicalPage = mapLogicalToPhysicalPage(currentPage)

                    if (pagerState.currentPage != targetPhysicalPage) {
                        pagerState.scrollToPage(targetPhysicalPage)
                    }
                    if (lazyListState.firstVisibleItemIndex != targetPhysicalPage) {
                        lazyListState.scrollToItem(targetPhysicalPage)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (!isVertical) {
                    // Paged mode (LTR or RTL)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = (WindowInsets.displayCutout.asPaddingValues()
                                .calculateTopPadding())
                                .coerceAtLeast(18.dp),
                            bottom = (WindowInsets.displayCutout.asPaddingValues()
                                .calculateBottomPadding())
                                .coerceAtLeast(18.dp),
                        )
                    ) { physicalPage ->
                        val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                        var pageImage by remember(logicalPage) { mutableStateOf<ImageBitmap?>(null) }

                        var scale by remember { mutableFloatStateOf(1f) }
                        var offsetX by remember { mutableFloatStateOf(0f) }
                        var offsetY by remember { mutableFloatStateOf(0f) }

                        LaunchedEffect(logicalPage) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }

                        LaunchedEffect(logicalPage) {
                            if (pageImage == null) {
                                withContext(Dispatchers.IO) {
                                    pageImage = loadPage(logicalPage)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(showMenu, isRTL) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        
                                        if (newScale > 1f) {
                                            scale = newScale
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                            
                                            val width = size.width.toFloat()
                                            val x = centroid.x
                                            var handledNavigation = false

                                            if (!showMenu) {
                                                if (x < width * 0.2f) {
                                                    scope.launch {
                                                        if (pagerState.currentPage > 0) {
                                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                        }
                                                    }
                                                    handledNavigation = true
                                                } else if (x > width * 0.8f) {
                                                    scope.launch {
                                                        if (pagerState.currentPage < totalPages - 1) {
                                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                        }
                                                    }
                                                    handledNavigation = true
                                                }
                                            }

                                            if (!handledNavigation) {
                                                onMenuToggle()
                                            }
                                        }
                                    }
                                }
                        ) {
                            if (pageImage != null) {
                                Image(
                                    bitmap = pageImage!!,
                                    contentDescription = null,
                                    contentScale = contentScale,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                }
                            }
                        }
                    }
                } else {
                    // Vertical scrolling mode with global zoom
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = (WindowInsets.displayCutout.asPaddingValues()
                                .calculateTopPadding())
                                .coerceAtLeast(18.dp),
                            bottom = (WindowInsets.displayCutout.asPaddingValues()
                                .calculateBottomPadding())
                                .coerceAtLeast(18.dp),
                        )
                    ) {
                        itemsIndexed(
                            (0 until totalPages).toList(),
                            key = { _, page -> page }
                        ) { _, physicalPage ->
                            val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                            var pageImage by remember(logicalPage) { mutableStateOf<ImageBitmap?>(null) }

                            LaunchedEffect(logicalPage) {
                                if (pageImage == null) {
                                    withContext(Dispatchers.IO) {
                                        pageImage = loadPage(logicalPage)
                                    }
                                }
                            }

                            if (pageImage != null) {
                                val webtoonContentScale = if (contentScale == ContentScale.Fit) ContentScale.FillWidth else contentScale
                                Image(
                                    bitmap = pageImage!!,
                                    contentDescription = null,
                                    contentScale = webtoonContentScale,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(showMenu, verticalZoomScale) {
                                            if (verticalZoomScale > 1f) {
                                                // When zoomed, handle pan gestures
                                                detectTransformGestures { _, pan, zoom, _ ->
                                                    val newScale = (verticalZoomScale * zoom).coerceIn(1f, 5f)
                                                    
                                                    if (newScale > 1f) {
                                                        verticalZoomScale = newScale
                                                        verticalOffsetY += pan.y
                                                        verticalOffsetX = (verticalOffsetX + pan.x).coerceIn(-500f, 500f)
                                                    } else {
                                                        verticalZoomScale = 1f
                                                        verticalOffsetX = 0f
                                                        verticalOffsetY = 0f
                                                    }
                                                }
                                            } else {
                                                // Normal tap to toggle menu
                                                detectTapGestures {
                                                    if (!showMenu) {
                                                        onMenuToggle()
                                                    }
                                                }
                                            }
                                        }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                }
                            }
                        }
                    }
                }
            }

            // Global zoom overlay for vertical mode
            if (isVertical && verticalZoomScale > 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = verticalZoomScale,
                            scaleY = verticalZoomScale,
                            translationX = verticalOffsetX,
                            translationY = verticalOffsetY
                        )
                )
            }

            // Zoom gesture handler for vertical mode (on top of everything)
            if (isVertical) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(showMenu, isVertical) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (verticalZoomScale * zoom).coerceIn(1f, 5f)
                                
                                if (newScale > 1f) {
                                    verticalZoomScale = newScale
                                    // Lock horizontal when zoomed - only allow vertical pan
                                    verticalOffsetY += pan.y
                                    verticalOffsetX = (verticalOffsetX + pan.x).coerceIn(-500f, 500f)
                                } else {
                                    verticalZoomScale = 1f
                                    verticalOffsetX = 0f
                                    verticalOffsetY = 0f
                                }
                            }
                        }
                )
            }

            // Page indicator
            if (showPageIndicator && totalPages > 0) {
                ComicPageIndicator(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
