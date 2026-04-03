/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import us.blindmint.codex.domain.reader.ReaderPageState
import us.blindmint.codex.domain.reader.ReaderPage
import us.blindmint.codex.domain.reader.ChapterTransition
import us.blindmint.codex.presentation.reader.viewer.ContentScaleMode
import us.blindmint.codex.presentation.reader.viewer.ZoomableImageView
import us.blindmint.codex.presentation.reader.viewer.webtoon.WebtoonRecyclerView
import us.blindmint.codex.presentation.reader.viewer.webtoon.WebtoonFrame
import us.blindmint.codex.presentation.reader.viewer.webtoon.WebtoonLayoutManager
import us.blindmint.codex.presentation.reader.viewer.webtoon.WebtoonImageAdapter

private const val MAX_CACHED_PAGES = 50
private const val PREFETCH_PAGES = 5
private const val EXTRA_LAYOUT_SPACE = 500

private fun mapContentScale(comicScaleType: Int, vertical: Boolean): ContentScaleMode {
    return when (comicScaleType) {
        2 -> ContentScaleMode.CROP
        3 -> ContentScaleMode.FILL_WIDTH
        4 -> ContentScaleMode.FILL_HEIGHT
        5 -> ContentScaleMode.NONE
        else -> if (vertical) ContentScaleMode.FILL_WIDTH else ContentScaleMode.FIT
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
    comicTapZone: Int = 0,
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
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { maxOf(1, totalPages) })
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val zoomedPages = remember { mutableStateMapOf<Int, Boolean>() }
    var storedLogicalPage by remember { mutableIntStateOf(0) }

    val pageStates = remember { mutableStateMapOf<Int, ReaderPageState>() }

    val pageProgress = remember { mutableStateMapOf<Int, Int>() }

    val mapLogicalToPhysicalPage = remember(isRTL, isVertical, totalPages) {
        { logicalPage: Int -> if (isRTL && !isVertical && totalPages > 0) totalPages - 1 - logicalPage else logicalPage }
    }
    val mapPhysicalToLogicalPage = remember(isRTL, isVertical, totalPages) {
        { physicalPage: Int -> if (isRTL && !isVertical && totalPages > 0) totalPages - 1 - physicalPage else physicalPage }
    }

    suspend fun prefetchPages(currentPage: Int) {
        val physicalPage = mapLogicalToPhysicalPage(currentPage)
        for (offset in -PREFETCH_PAGES..PREFETCH_PAGES) {
            val targetPage = physicalPage + offset
            if (targetPage in 0 until totalPages && !loadedPages.containsKey(targetPage)) {
                val logicalPage = mapPhysicalToLogicalPage(targetPage)
                if (pageStates[targetPage] != ReaderPageState.Ready) {
                    pageStates[targetPage] = ReaderPageState.Loading
                }
                withContext(Dispatchers.IO) {
                    loadPage(logicalPage)?.let { bitmap ->
                        loadedPages[targetPage] = bitmap to bitmap.asAndroidBitmap()
                        pageStates[targetPage] = ReaderPageState.Ready
                    } ?: run {
                        if (pageStates[targetPage] != ReaderPageState.Ready) {
                            pageStates[targetPage] = ReaderPageState.Error("Failed to load page")
                        }
                    }
                }
            }
        }
    }

    suspend fun preloadAround(currentPage: Int) {
        val physicalPage = mapLogicalToPhysicalPage(currentPage)
        val preloadRange = PREFETCH_PAGES
        val startIndex = maxOf(0, physicalPage - preloadRange)
        val endIndex = minOf(totalPages - 1, physicalPage + preloadRange)

        for (i in startIndex..endIndex) {
            if (i == physicalPage) continue
            if (!loadedPages.containsKey(i)) {
                val logicalPage = mapPhysicalToLogicalPage(i)
                if (pageStates[i] != ReaderPageState.Ready) {
                    pageStates[i] = ReaderPageState.Loading
                }
                scope.launch(Dispatchers.IO) {
                    loadPage(logicalPage)?.let { bitmap ->
                        loadedPages[i] = bitmap to bitmap.asAndroidBitmap()
                        pageStates[i] = ReaderPageState.Ready
                    }
                }
            }
        }
    }

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
                        while (lazyListState.isScrollInProgress) emit(Unit)
                    }.first()
                } catch (_: Exception) {
                }
            } else {
                pagerState.scrollToPage(targetPhysicalPage)
                if (pagerState.isScrollInProgress) {
                    try {
                        kotlinx.coroutines.flow.flow {
                            while (pagerState.isScrollInProgress) emit(Unit)
                        }.first()
                    } catch (_: Exception) {
                    }
                }
            }
            onScrollRestorationComplete()
        }
    }

    suspend fun preloadVisibleRange(firstVisible: Int, lastVisible: Int) {
        val preloadBehind = PREFETCH_PAGES
        val preloadAhead = PREFETCH_PAGES

        for (targetPage in (firstVisible - preloadBehind)..(lastVisible + preloadAhead)) {
            if (targetPage in 0 until totalPages && !loadedPages.containsKey(targetPage)) {
                val logicalPage = mapPhysicalToLogicalPage(targetPage)
                if (pageStates[targetPage] != ReaderPageState.Ready) {
                    pageStates[targetPage] = ReaderPageState.Loading
                }
                scope.launch(Dispatchers.IO) {
                    loadPage(logicalPage)?.let { bitmap ->
                        loadedPages[targetPage] = bitmap to bitmap.asAndroidBitmap()
                        pageStates[targetPage] = ReaderPageState.Ready
                    } ?: run {
                        if (pageStates[targetPage] != ReaderPageState.Ready) {
                            pageStates[targetPage] = ReaderPageState.Error("Failed to load page")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(pagerState, totalPages, isVertical) {
        snapshotFlow { pagerState.currentPage }
            .debounce(50)
            .collect { physicalPage ->
                if (!isVertical && totalPages > 0) {
                    val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                    onPageChanged(logicalPage)
                    onPageSelected(logicalPage)
                    prefetchPages(logicalPage)
                }
            }
    }

    LaunchedEffect(lazyListState, totalPages, isVertical) {
        snapshotFlow {
            val first = lazyListState.firstVisibleItemIndex
            val last = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: first
            first to last
        }
            .debounce(50)
            .collect { visibleRange ->
                val firstVisible = visibleRange.first
                val lastVisible = visibleRange.second
                if (isVertical && totalPages > 0) {
                    val logicalPage = mapPhysicalToLogicalPage(firstVisible)
                    onPageChanged(logicalPage)
                    onPageSelected(logicalPage)
                    preloadVisibleRange(firstVisible, lastVisible)
                }
            }
    }

    DisposableEffect(bookTitle) {
        onDispose {
            loadedPages.values.forEach { it.second.recycle() }
            loadedPages.clear()
            pageStates.clear()
            pageProgress.clear()
            ZoomableImageView.resetSharedZoom()
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
                    pagerState.scrollToPage(mapLogicalToPhysicalPage(initialPage))
                }
            }

            LaunchedEffect(currentPage, totalPages, isRTL) {
                if (currentPage in 0 until totalPages && totalPages > 0) {
                    val targetPhysicalPage = mapLogicalToPhysicalPage(currentPage)
                    if (!isVertical && pagerState.currentPage != targetPhysicalPage) {
                        pagerState.scrollToPage(targetPhysicalPage)
                    }
                }
            }

            if (!isVertical) {
                val activePhysicalPage = pagerState.currentPage
                val pagerScrollEnabled = !(zoomedPages[activePhysicalPage] ?: false)

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = pagerScrollEnabled,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = (WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()).coerceAtLeast(18.dp),
                        bottom = (WindowInsets.displayCutout.asPaddingValues().calculateBottomPadding()).coerceAtLeast(18.dp)
                    )
                ) { physicalPage ->
                    val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                    var pageImage by remember(logicalPage) { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(logicalPage) {
                        zoomedPages[physicalPage] = false
                        pageStates[physicalPage] = ReaderPageState.Loading
                        if (pageImage == null) {
                            withContext(Dispatchers.IO) {
                                try {
                                    pageImage = loadPage(logicalPage)
                                    if (pageImage != null) {
                                        loadedPages[physicalPage] = pageImage!! to pageImage!!.asAndroidBitmap()
                                        pageStates[physicalPage] = ReaderPageState.Ready
                                    } else {
                                        pageStates[physicalPage] = ReaderPageState.Error("Failed to load page")
                                    }
                                } catch (e: Exception) {
                                    pageStates[physicalPage] = ReaderPageState.Error(e.message ?: "Unknown error", e)
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                ZoomableImageView(context).apply {
                                    setMaxScale(5f)
                                    setContentScaleMode(mapContentScale(comicScaleType, vertical = false))
                                    setInvertColors(invertColors)
                                    callbacks = ZoomableImageView.GestureCallbacks(
                                        onTap = { x, y ->
                                            val width = width.toFloat()
                                            val height = height.toFloat()
                                            var handledNavigation = false
                                            if (!showMenu && !isZoomed()) {
                                                when (comicTapZone) {
                                                    0 -> {
                                                        if (x < width * 0.2f) {
                                                            scope.launch { if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                            handledNavigation = true
                                                        } else if (x > width * 0.8f) {
                                                            scope.launch { if (pagerState.currentPage < totalPages - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                            handledNavigation = true
                                                        }
                                                    }
                                                    1 -> {
                                                        if (x < width * 0.3f || (x < width * 0.5f && y > height * 0.7f)) {
                                                            scope.launch { if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                            handledNavigation = true
                                                        } else if (x > width * 0.7f || (x > width * 0.5f && y > height * 0.7f)) {
                                                            scope.launch { if (pagerState.currentPage < totalPages - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                            handledNavigation = true
                                                        }
                                                    }
                                                    2 -> {
                                                        if (x < width * 0.2f) {
                                                            scope.launch { if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                            handledNavigation = true
                                                        } else if (x > width * 0.8f) {
                                                            scope.launch { if (pagerState.currentPage < totalPages - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                            handledNavigation = true
                                                        }
                                                    }
                                                    3 -> {
                                                        if (x < width * 0.04f) {
                                                            scope.launch { if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                            handledNavigation = true
                                                        } else if (x > width * 0.96f) {
                                                            scope.launch { if (pagerState.currentPage < totalPages - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                            handledNavigation = true
                                                        }
                                                    }
                                                    4 -> {
                                                        if (x < width * 0.5f) {
                                                            scope.launch { if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                        } else {
                                                            scope.launch { if (pagerState.currentPage < totalPages - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                        }
                                                        handledNavigation = true
                                                    }
                                                }
                                            }
                                            if (!handledNavigation) {
                                                onMenuToggle()
                                            }
                                        },
                                        onLongPress = { bitmapX, bitmapY ->
                                            onLongPress?.invoke(logicalPage, bitmapX, bitmapY)
                                        },
                                        onScaleChanged = { scale ->
                                            zoomedPages[physicalPage] = scale > 1.01f
                                        }
                                    )
                                }
                            },
                            update = { view ->
                                view.setBitmap(pageImage?.asAndroidBitmap())
                                view.setContentScaleMode(mapContentScale(comicScaleType, vertical = false))
                                view.setInvertColors(invertColors)
                                if (physicalPage != pagerState.currentPage) {
                                    view.resetZoom()
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = EXTRA_LAYOUT_SPACE.dp,
                        bottom = (WindowInsets.displayCutout.asPaddingValues().calculateBottomPadding()).coerceAtLeast(18.dp) + EXTRA_LAYOUT_SPACE.dp
                    )
                ) {
                    itemsIndexed((0 until totalPages).toList(), key = { _, page -> page }) { _, physicalPage ->
                        val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                        var pageImage by remember(logicalPage) { mutableStateOf<ImageBitmap?>(null) }

                        LaunchedEffect(logicalPage) {
                            pageStates[physicalPage] = ReaderPageState.Loading
                            if (pageImage == null) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        pageImage = loadPage(logicalPage)
                                        if (pageImage != null) {
                                            loadedPages[physicalPage] = pageImage!! to pageImage!!.asAndroidBitmap()
                                            pageStates[physicalPage] = ReaderPageState.Ready
                                        } else {
                                            pageStates[physicalPage] = ReaderPageState.Error("Failed to load page")
                                        }
                                    } catch (e: Exception) {
                                        pageStates[physicalPage] = ReaderPageState.Error(e.message ?: "Unknown error", e)
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = with(density) { (pageImage?.height ?: 1).toDp() }),
                                factory = { context ->
                                    ZoomableImageView(context).apply {
                                        setMaxScale(5f)
                                        setContentScaleMode(mapContentScale(comicScaleType, vertical = true))
                                        setVerticalMode(true)
                                        setUseSharedZoom(true)
                                        setInvertColors(invertColors)
                                        callbacks = ZoomableImageView.GestureCallbacks(
                                            onTap = { _, _ -> onMenuToggle() },
                                            onLongPress = { bitmapX, bitmapY ->
                                                onLongPress?.invoke(logicalPage, bitmapX, bitmapY)
                                            },
                                            onScaleChanged = { scale ->
                                                zoomedPages[physicalPage] = scale > 1.01f
                                            },
                                            onInteractionStateChanged = { isInteracting ->
                                                zoomedPages[physicalPage] = isInteracting
                                            }
                                        )
                                    }
                                },
                                update = { view ->
                                    view.setBitmap(pageImage?.asAndroidBitmap())
                                    view.setContentScaleMode(mapContentScale(comicScaleType, vertical = true))
                                    view.setVerticalMode(true)
                                    view.setUseSharedZoom(true)
                                    view.setInvertColors(invertColors)
                                }
                            )
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

    LaunchedEffect(bookTitle) {
        onLoadingComplete()
    }
}
