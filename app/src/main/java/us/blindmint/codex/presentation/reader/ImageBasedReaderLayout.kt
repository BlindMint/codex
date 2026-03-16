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
import kotlin.math.min
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

    // Lazy loading cache - stores Pair of (ImageBitmap for display, Bitmap for cleanup)
    // Using LinkedHashMap for LRU eviction order
    val loadedPages = remember {
        object : LinkedHashMap<Int, Pair<ImageBitmap, Bitmap>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Pair<ImageBitmap, Bitmap>>?): Boolean {
                if (size > MAX_CACHED_PAGES) {
                    eldest?.value?.second?.recycle() // Free native memory
                    return true
                }
                return false
            }
        }
    }

    // Reading direction support
    val isRTL = readingDirection == "RTL"
    val isVertical = readingDirection == "VERTICAL" || readerMode == "WEBTOON"

    // Map comic scale type to ContentScale
    val contentScale = remember(comicScaleType) {
        when (comicScaleType) {
            1 -> ContentScale.Fit  // FIT_SCREEN
            2 -> ContentScale.Crop  // STRETCH (crop to fit)
            3 -> ContentScale.FillWidth  // FIT_WIDTH
            4 -> ContentScale.FillHeight  // FIT_HEIGHT
            5 -> ContentScale.None  // ORIGINAL (no scaling)
            6 -> ContentScale.Fit  // SMART_FIT (same as fit for now)
            else -> ContentScale.Fit
        }
    }

    // For RTL, reverse the page order so page 0 becomes the last physical page
    // Don't reverse in vertical mode
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

    // Store the current logical page for positioning when direction changes
    var storedLogicalPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentPage) {
        storedLogicalPage = currentPage
    }

    // Position to the same logical page when reading direction changes
    // Wait for scroll animation to complete before hiding loading
    LaunchedEffect(readingDirection, totalPages) {
        if (totalPages > 0 && storedLogicalPage >= 0) {
            val targetPhysicalPage = mapLogicalToPhysicalPage(storedLogicalPage)
            if (isVertical) {
                lazyListState.scrollToItem(targetPhysicalPage)
                // Wait for scroll to complete (with timeout to avoid exception)
                try {
                    kotlinx.coroutines.flow.flow {
                        while (lazyListState.isScrollInProgress) {
                            emit(Unit)
                        }
                    }.first()
                } catch (e: Exception) {
                    // Ignore any exceptions from scroll completion check
                }
            } else {
                pagerState.scrollToPage(targetPhysicalPage)
                // Wait for scroll to complete (only if scroll is in progress)
                if (pagerState.isScrollInProgress) {
                    try {
                        kotlinx.coroutines.flow.flow {
                            while (pagerState.isScrollInProgress) {
                                emit(Unit)
                            }
                        }.first()
                    } catch (e: Exception) {
                        // Ignore any exceptions from scroll completion check
                    }
                }
            }
            // Signal that scroll restoration is complete - loading can now be hidden
            onScrollRestorationComplete()
        }
    }

    // Prefetch pages around current page
    suspend fun prefetchPages(currentPage: Int) {
        val physicalPage = mapLogicalToPhysicalPage(currentPage)
        for (offset in -PREFETCH_PAGES..PREFETCH_PAGES) {
            val targetPage = physicalPage + offset
            if (targetPage in 0 until totalPages && !loadedPages.containsKey(targetPage)) {
                withContext(Dispatchers.IO) {
                    loadPage(mapPhysicalToLogicalPage(targetPage))?.let { imageBitmap ->
                        // Get the Android Bitmap from ImageBitmap for recycling
                        // This is a workaround - in real implementation, loadPage should return both
                        // For now, we'll just cache the ImageBitmap without recycling support
                        // TODO: Improve bitmap management
                    }
                }
            }
        }
    }

    // Track page changes in paged mode (with debounce)
    LaunchedEffect(pagerState, isRTL, isVertical) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .debounce(50)
            .collect { (physicalPage, _) ->
                if (!isVertical && totalPages > 0) {
                    val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                    onPageChanged(logicalPage)
                    // Prefetch pages around current position
                    prefetchPages(logicalPage)
                }
            }
    }

    // Track page changes in vertical mode (with debounce)
    LaunchedEffect(lazyListState, isRTL, isVertical) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .debounce(50)
            .collect { physicalIndex ->
                if (isVertical && totalPages > 0) {
                    val logicalPage = mapPhysicalToLogicalPage(physicalIndex)
                    onPageChanged(logicalPage)
                    // Prefetch pages around current position
                    prefetchPages(logicalPage)
                }
            }
    }

    // Cleanup
    DisposableEffect(bookTitle) {
        onDispose {
            // Recycle all bitmaps to free native memory
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
            // Restore initial page position
            LaunchedEffect(totalPages) {
                if (initialPage in 0 until totalPages) {
                    val targetPhysicalPage = mapLogicalToPhysicalPage(initialPage)
                    pagerState.scrollToPage(targetPhysicalPage)
                }
            }

            // Keep both scroll states in sync with currentPage
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
                                    detectTapGestures { offset ->
                                        val width = size.width.toFloat()
                                        val x = offset.x
                                        var handledNavigation = false

                                        if (!showMenu) {
                                            // Left edge tap
                                            if (x < width * 0.2f) {
                                                scope.launch {
                                                    if (pagerState.currentPage > 0) {
                                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                    }
                                                }
                                                handledNavigation = true
                                            }
                                            // Right edge tap
                                            else if (x > width * 0.8f) {
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
                        ) {
                            if (pageImage != null) {
                                Image(
                                    bitmap = pageImage!!,
                                    contentDescription = null,
                                    contentScale = contentScale,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Loading placeholder
                                }
                            }
                        }
                    }
                } else {
                    // Vertical scrolling mode
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
                                // For webtoon, prefer FillWidth but respect user's contentScale choice
                                val webtoonContentScale = if (contentScale == ContentScale.Fit) ContentScale.FillWidth else contentScale
                                Image(
                                    bitmap = pageImage!!,
                                    contentDescription = null,
                                    contentScale = webtoonContentScale,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(showMenu) {
                                            detectTapGestures {
                                                onMenuToggle()
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
                                    // Loading placeholder
                                }
                            }
                        }
                    }
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
}