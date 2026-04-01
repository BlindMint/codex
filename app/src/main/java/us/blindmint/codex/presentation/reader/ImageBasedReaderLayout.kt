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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.LinkedHashMap

private const val TAG = "ImageBasedReader"
private const val MAX_CACHED_PAGES = 50
private const val PREFETCH_PAGES = 5

/**
 * Non-blocking long-press detector.
 *
 * Observes pointer events via [PointerEventPass.Initial] without consuming them, so it never
 * interferes with concurrent gesture handlers (detectTransformGestures, detectTapGestures) or
 * parent scrollables (LazyColumn).  Fires [onLongPress] with the original down position when the
 * pointer has been held still for the system long-press timeout.
 */
private suspend fun PointerInputScope.detectLongPressNonBlocking(
    onLongPress: (position: Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPos = down.position

        val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis.toLong()) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: return@withTimeoutOrNull true
                if (!change.pressed) return@withTimeoutOrNull true
                if ((change.position - startPos).getDistance() > viewConfiguration.touchSlop) {
                    return@withTimeoutOrNull true
                }
            }
            @Suppress("UNREACHABLE_CODE") true
        }

        if (cancelled == null) {
            onLongPress(startPos)
        }
    }
}

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
    modifier: Modifier = Modifier,
    onLongPress: ((pageIndex: Int, bitmapX: Float, bitmapY: Float) -> Unit)? = null
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
                                .pointerInput(onLongPress, logicalPage) {
                                    if (onLongPress == null) return@pointerInput
                                    detectLongPressNonBlocking { startPos ->
                                        if (scale != 1f) return@detectLongPressNonBlocking
                                        val bitmapW = pageImage?.width?.toFloat() ?: return@detectLongPressNonBlocking
                                        val bitmapH = pageImage?.height?.toFloat() ?: return@detectLongPressNonBlocking
                                        val containerW = size.width.toFloat()
                                        val containerH = size.height.toFloat()
                                        val bitmapAspect = bitmapW / bitmapH
                                        val containerAspect = containerW / containerH
                                        val fitScale: Float
                                        val imgOffsetX: Float
                                        val imgOffsetY: Float
                                        if (bitmapAspect > containerAspect) {
                                            fitScale = containerW / bitmapW
                                            imgOffsetX = 0f
                                            imgOffsetY = (containerH - bitmapH * fitScale) / 2f
                                        } else {
                                            fitScale = containerH / bitmapH
                                            imgOffsetX = (containerW - bitmapW * fitScale) / 2f
                                            imgOffsetY = 0f
                                        }
                                        val bitmapX = (startPos.x - imgOffsetX) / fitScale
                                        val bitmapY = (startPos.y - imgOffsetY) / fitScale
                                        if (bitmapX in 0f..bitmapW && bitmapY in 0f..bitmapH) {
                                            onLongPress(logicalPage, bitmapX, bitmapY)
                                        }
                                    }
                                }
                                .pointerInput(showMenu, isRTL, totalPages, scale) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            if (showMenu) {
                                                onMenuToggle()
                                                return@detectTapGestures
                                            }
                                            if (scale > 1f) return@detectTapGestures
                                            val width = size.width.toFloat()
                                            val x = offset.x
                                            if (x < width * 0.2f) {
                                                scope.launch {
                                                    if (pagerState.currentPage > 0) {
                                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                    }
                                                }
                                            } else if (x > width * 0.8f) {
                                                scope.launch {
                                                    if (pagerState.currentPage < totalPages - 1) {
                                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                    }
                                                }
                                            } else {
                                                onMenuToggle()
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        if (newScale > 1f) {
                                            scale = newScale
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(onLongPress, logicalPage) {
                                            if (onLongPress == null) return@pointerInput
                                            detectLongPressNonBlocking { startPos ->
                                                if (verticalZoomScale != 1f) return@detectLongPressNonBlocking
                                                val bitmapW = pageImage?.width?.toFloat() ?: return@detectLongPressNonBlocking
                                                val bitmapH = pageImage?.height?.toFloat() ?: return@detectLongPressNonBlocking
                                                val containerW = size.width.toFloat()
                                                val fitScale = containerW / bitmapW
                                                val bitmapX = startPos.x / fitScale
                                                val bitmapY = startPos.y / fitScale
                                                if (bitmapX in 0f..bitmapW && bitmapY in 0f..bitmapH) {
                                                    onLongPress(logicalPage, bitmapX, bitmapY)
                                                }
                                            }
                                        }
                                        .pointerInput(showMenu, verticalZoomScale) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (showMenu) {
                                                        onMenuToggle()
                                                        return@detectTapGestures
                                                    }
                                                    if (verticalZoomScale > 1f) return@detectTapGestures
                                                    onMenuToggle()
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
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
                                        }
                                ) {
                                    Image(
                                        bitmap = pageImage!!,
                                        contentDescription = null,
                                        contentScale = webtoonContentScale,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer(
                                                scaleX = verticalZoomScale,
                                                scaleY = verticalZoomScale,
                                                translationX = verticalOffsetX,
                                                translationY = verticalOffsetY
                                            )
                                    )
                                }
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
