/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.net.toUri
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
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import kotlin.math.min

private const val TAG = "CodexPdf"

@OptIn(FlowPreview::class)
@Composable
fun PdfReaderLayout(
    book: Book,
    currentPage: Int,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit,
    backgroundColor: Color,
    readingDirection: String = "LTR",
    modifier: Modifier = Modifier,
    showMenu: Boolean = false,
    showPageIndicator: Boolean = true,
    onLoadingComplete: () -> Unit = {},
    onScrollRestorationComplete: () -> Unit = {},
    onMenuToggle: () -> Unit = {},
    onTotalPagesLoaded: (Int) -> Unit = {},
    onPageSelected: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var initialLoadComplete by remember { mutableStateOf(false) }

    val loadedPages = remember { mutableMapOf<Int, Pair<ImageBitmap, android.graphics.Bitmap>>() }
    val renderMutex = remember { Mutex() }

    // Reading direction support
    val isRTL = readingDirection == "RTL"
    val isVertical = readingDirection == "VERTICAL"

    // For RTL, reverse the page order so page 0 becomes the last physical page
    val mapLogicalToPhysicalPage = { logicalPage: Int ->
        if (isRTL && totalPages > 0) totalPages - 1 - logicalPage else logicalPage
    }

    val mapPhysicalToLogicalPage = { physicalPage: Int ->
        if (isRTL && totalPages > 0) totalPages - 1 - physicalPage else physicalPage
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
                // Wait for scroll to complete
                kotlinx.coroutines.flow.flow {
                    while (lazyListState.isScrollInProgress) {
                        emit(Unit)
                    }
                }.first()
            } else {
                pagerState.scrollToPage(targetPhysicalPage)
                // Wait for scroll to complete
                kotlinx.coroutines.flow.flow {
                    while (pagerState.isScrollInProgress) {
                        emit(Unit)
                    }
                }.first()
            }
            // Signal that scroll restoration is complete - loading can now be hidden
            onScrollRestorationComplete()
        }
    }

    // Render a single PDF page to bitmap
    suspend fun renderPage(pageIndex: Int, renderer: PdfRenderer): ImageBitmap? {
        return renderMutex.withLock {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) return@withLock null
            
            loadedPages[pageIndex]?.first?.let { return@withLock it }

            try {
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null

                val page = renderer.openPage(pageIndex)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

val imageBitmap = bitmap.asImageBitmap()
loadedPages[pageIndex] = imageBitmap to bitmap
Log.d(TAG, "Rendered page ${pageIndex + 1}")
imageBitmap
            } catch (e: Exception) {
                if (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    Log.w(TAG, "Failed to render page $pageIndex: ${e.message}", e)
                }
                null
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
                }
            }
    }

    // Load the PDF
LaunchedEffect(book.id) {
    isLoading = true
    errorMessage = null

    scope.launch {
        kotlinx.coroutines.delay(10000)
        if (isLoading) {
            errorMessage = "Loading timed out after 10 seconds"
            isLoading = false
        }
    }

    try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Loading PDF: ${book.title}")
                val cachedFile = CachedFileFactory.fromBook(context, book)
                if (cachedFile == null) {
                    Log.e(TAG, "Failed to create CachedFile for PDF")
                    errorMessage = "Failed to access PDF file"
                    return@withContext
                }
                val rawFile = cachedFile.rawFile

                if (rawFile == null) {
                    errorMessage = "Failed to cache PDF file"
                    return@withContext
                }

                val fd = ParcelFileDescriptor.open(rawFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)

                fileDescriptor = fd
                pdfRenderer = renderer
                totalPages = renderer.pageCount
                onTotalPagesLoaded(renderer.pageCount)
                Log.d(TAG, "PDF loaded, pages: ${renderer.pageCount}")

                // Pre-load first few pages
                for (i in 0 until min(3, renderer.pageCount)) {
                    renderPage(i, renderer)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            Log.e(TAG, "Failed to load PDF", e)
            errorMessage = "Failed to load PDF: ${e.message}"
        } finally {
            isLoading = false
            initialLoadComplete = true
            onLoadingComplete()
        }
    }

    // Cleanup
    DisposableEffect(book.id) {
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing PDF resources", e)
            }
            loadedPages.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize())
    } else if (errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = errorMessage!!,
                color = backgroundColor,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else if (totalPages > 0) {
            // Restore initial page position
            LaunchedEffect(totalPages) {
                if (initialPage in 0 until totalPages) {
                    val targetPhysicalPage = mapLogicalToPhysicalPage(initialPage)
                    pagerState.scrollToPage(targetPhysicalPage)
                }
            }

            // Keep both scroll states in sync with currentPage
            LaunchedEffect(currentPage, totalPages, isRTL, initialLoadComplete) {
                if (!initialLoadComplete) return@LaunchedEffect
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
                        var pageImage by remember(logicalPage) { mutableStateOf(loadedPages[logicalPage]?.first) }

                        LaunchedEffect(logicalPage) {
                            if (pageImage == null) {
                                pdfRenderer?.let { renderer ->
                                    withContext(Dispatchers.IO) {
                                        pageImage = renderPage(logicalPage, renderer)
                                    }
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
                                    contentScale = ContentScale.Fit,
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
var pageImage by remember(logicalPage) { mutableStateOf(loadedPages[logicalPage]?.first) }

                            LaunchedEffect(logicalPage) {
                                if (pageImage == null) {
                                    pdfRenderer?.let { renderer ->
                                        withContext(Dispatchers.IO) {
                                            pageImage = renderPage(logicalPage, renderer)
                                        }
                                    }
                                }
                            }

                            if (pageImage != null) {
                                Image(
                                    bitmap = pageImage!!,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
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
