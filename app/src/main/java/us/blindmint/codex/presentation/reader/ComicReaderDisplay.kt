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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
private const val SLIDER_TAG = "CodexComicSlider"
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
fun ComicReaderDisplay(
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
    onLongPress: ((pageIndex: Int, bitmapX: Float, bitmapY: Float) -> Unit)? = null,
    invertColors: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loadedPages = remember {
        object : LinkedHashMap<Int, Pair<ImageBitmap, Bitmap>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Pair<ImageBitmap, Bitmap>>?): Boolean {
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

    val imageColorFilter: ColorFilter? = if (invertColors) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else {
        null
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
    var programmaticScrollTarget by remember { mutableIntStateOf(-1) }

    LaunchedEffect(currentPage) {
        storedLogicalPage = currentPage
    }

    LaunchedEffect(readingDirection, totalPages) {
        if (totalPages > 0 && storedLogicalPage >= 0) {
            val targetPhysicalPage = mapLogicalToPhysicalPage(storedLogicalPage)
            if (isVertical) {
                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                val firstVisibleIndex = visibleItems.firstOrNull()?.index
                val isAlreadyAtTop = firstVisibleIndex == targetPhysicalPage
                if (!isAlreadyAtTop) {
                    programmaticScrollTarget = targetPhysicalPage
                    lazyListState.scrollToItem(targetPhysicalPage)
                    launch {
                        kotlinx.coroutines.delay(100)
                        programmaticScrollTarget = -1
                    }
                }
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
            if (targetPage in 0 until totalPages) {
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
        snapshotFlow { 
            lazyListState.firstVisibleItemIndex to programmaticScrollTarget
        }
            .debounce(500)
            .collect { (physicalIndex, scrollTarget) ->
                if (isVertical && totalPages > 0) {
                    if (scrollTarget == -1 || scrollTarget == physicalIndex) {
                        val logicalPage = mapPhysicalToLogicalPage(physicalIndex)
                        Log.d(SLIDER_TAG, "[\u2190 snapshotFlow] emitting onPageChanged($logicalPage) physicalIndex=$physicalIndex scrollTarget=$scrollTarget")
                        onPageChanged(logicalPage)
                        prefetchPages(logicalPage)
                    } else {
                        Log.d(SLIDER_TAG, "[\u2190 snapshotFlow] SKIPPED: physicalIndex=$physicalIndex scrollTarget=$scrollTarget")
                    }
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
                Log.d(SLIDER_TAG, "[\u2B06 LaunchedEffect] fired: currentPage=$currentPage, totalPages=$totalPages, isRTL=$isRTL, isVertical=$isVertical")
                if (currentPage in 0 until totalPages && totalPages > 0) {
                    val targetPhysicalPage = mapLogicalToPhysicalPage(currentPage)

                    if (!isVertical) {
                        if (pagerState.currentPage != targetPhysicalPage) {
                            Log.d(SLIDER_TAG, "[\u2B06 LaunchedEffect] PAGER scroll to $targetPhysicalPage")
                            pagerState.scrollToPage(targetPhysicalPage)
                        }
                    } else {
                        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                        val firstVisibleIndex = visibleItems.firstOrNull()?.index
                        val lastMeasuredIndex = visibleItems.lastOrNull()?.index ?: -1
                        val isAlreadyAtTop = firstVisibleIndex == targetPhysicalPage
                        Log.d(SLIDER_TAG, "[\u2B06 LaunchedEffect] VERTICAL: targetPhysicalPage=$targetPhysicalPage, firstVisibleIndex=$firstVisibleIndex, lastMeasuredIndex=$lastMeasuredIndex, isAlreadyAtTop=$isAlreadyAtTop")
                        if (!isAlreadyAtTop) {
                            programmaticScrollTarget = targetPhysicalPage
                            if (lastMeasuredIndex < targetPhysicalPage) {
                                Log.d(SLIDER_TAG, "[\u2B06 LaunchedEffect] VERTICAL: target not measured yet, pre-scrolling to $lastMeasuredIndex first")
                                lazyListState.scrollToItem(lastMeasuredIndex)
                            }
                            kotlinx.coroutines.delay(200)
                            val currentLastMeasured = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            Log.d(SLIDER_TAG, "[\u2B06 LaunchedEffect] VERTICAL: scrollToItem($targetPhysicalPage) (lastMeasuredNow=$currentLastMeasured)")
                            lazyListState.scrollToItem(targetPhysicalPage)
                            launch {
                                kotlinx.coroutines.delay(200)
                                programmaticScrollTarget = -1
                            }
                        } else {
                            Log.d(SLIDER_TAG, "[\u2B06 LaunchedEffect] VERTICAL: skipping scroll (already at top)")
                        }
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
                                .pointerInput(showMenu) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val downTime = down.uptimeMillis
                                        val startPos = down.position
                                        var upPos = startPos
                                        var lastChange: androidx.compose.ui.input.pointer.PointerInputChange? = null

                                        do {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            lastChange = change
                                            if (!change.pressed) {
                                                upPos = change.position
                                            }
                                        } while (event.changes.any { it.pressed })

                                        val distance = (upPos - startPos).getDistance()
                                        val elapsed = (lastChange?.uptimeMillis ?: 0) - downTime

                                                    if (distance < viewConfiguration.touchSlop && elapsed > 80) {
                                            if (showMenu) {
                                                onMenuToggle()
                                            } else {
                                                val width = size.width.toFloat()
                                                when {
                                                    upPos.x < width * 0.2f -> {
                                                        scope.launch {
                                                            if (pagerState.currentPage > 0) {
                                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                            }
                                                        }
                                                    }

                                                    upPos.x > width * 0.8f -> {
                                                        scope.launch {
                                                            if (pagerState.currentPage < totalPages - 1) {
                                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                            }
                                                        }
                                                    }

                                                    else -> onMenuToggle()
                                                }
                                            }
                                        }
                                    }
                                }
                                .pointerInput(onLongPress, logicalPage, pageImage) {
                                    if (onLongPress == null) return@pointerInput
                                    detectLongPressNonBlocking { startPos ->
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
                        ) {
                            if (pageImage != null) {
                                Image(
                                    bitmap = pageImage!!,
                                    contentDescription = null,
                                    contentScale = contentScale,
                                    colorFilter = imageColorFilter,
                                    modifier = Modifier.fillMaxSize()
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 0.dp,
                                bottom = if (isVertical) 0.dp else (WindowInsets.displayCutout.asPaddingValues()
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
                                    withContext(Dispatchers.IO) {
                                        pageImage = loadPage(logicalPage)
                                    }
                                }

                                if (pageImage != null) {
                                    val webtoonContentScale = if (contentScale == ContentScale.Fit) ContentScale.FillWidth else contentScale
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(showMenu) {
                                                awaitEachGesture {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    val downTime = down.uptimeMillis
                                                    val startPos = down.position
                                                    var upPos = startPos
                                                    var lastChange: androidx.compose.ui.input.pointer.PointerInputChange? = null

                                                    do {
                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                        val change = event.changes.firstOrNull() ?: break
                                                        lastChange = change
                                                        if (!change.pressed) {
                                                            upPos = change.position
                                                        }
                                                    } while (event.changes.any { it.pressed })

                                                    val distance = (upPos - startPos).getDistance()
                                                    val elapsed = (lastChange?.uptimeMillis ?: 0) - downTime

                                        if (distance < viewConfiguration.touchSlop && elapsed > 80) {
                                                        val width = size.width.toFloat()
                                                        if (!showMenu && (upPos.x < width * 0.2f || upPos.x > width * 0.8f)) {
                                                            return@awaitEachGesture
                                                        }
                                                        onMenuToggle()
                                                    }
                                                }
                                            }
                                            .pointerInput(onLongPress, logicalPage, pageImage) {
                                                if (onLongPress == null) return@pointerInput
                                                detectLongPressNonBlocking { startPos ->
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
                                    ) {
                                        Image(
                                            bitmap = pageImage!!,
                                            contentDescription = null,
                                            contentScale = webtoonContentScale,
                                            colorFilter = imageColorFilter,
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
                                            .height(1200.dp)
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showPageIndicator && totalPages > 0) {
                PageIndicator(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
