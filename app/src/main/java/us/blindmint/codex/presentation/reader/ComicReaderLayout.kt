/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.min
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.parser.comic.ArchiveReader
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.reader.ReaderEvent

@Composable
fun ComicReaderLayout(
    book: Book,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    contentPadding: PaddingValues,
    backgroundColor: Color,
    fontColor: Color,
    comicReadingDirection: String,
    comicReaderMode: String,
    comicTapZone: Int,
    comicInvertTaps: String,
    comicScaleType: Int,
    comicProgressBar: Boolean,
    comicProgressBarPadding: Int,
    comicProgressBarAlignment: us.blindmint.codex.domain.util.HorizontalAlignment,
    comicProgressBarFontSize: Int,
    comicProgressCount: us.blindmint.codex.domain.reader.ReaderProgressCount,
    showMenu: Boolean = false,
    showPageIndicator: Boolean = true,
    onLoadingComplete: () -> Unit = {},
    onMenuToggle: () -> Unit = {},
    onTotalPagesLoaded: (Int) -> Unit = {},
    onPageSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val mainModel = hiltViewModel<MainModel>()
    val scope = rememberCoroutineScope()

    // Archive state
    var archiveHandle by remember { mutableStateOf<ArchiveReader.ArchiveHandle?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var comicLoaded by remember { mutableStateOf(false) }
    var initialLoadComplete by remember { mutableStateOf(false) }

    // Lazy loading cache
    val loadedPages = remember { mutableMapOf<Int, ImageBitmap>() }

    // Function to load a specific page
    fun loadPage(pageIndex: Int): ImageBitmap? {
        // Check if already loaded
        loadedPages[pageIndex]?.let { return it }

        // Load the page
        try {
            archiveHandle?.let { archive ->
                val imageEntries = archive.entries.filter { entry ->
                    val entryPath = entry.getPath()
                    entryPath != null && ArchiveReader.isImageFile(entryPath)
                }

                if (pageIndex < imageEntries.size) {
                    val entry = imageEntries[pageIndex]
                    android.util.Log.d("CodexComic", "Lazy loading page ${pageIndex + 1}")

                    archive.getInputStream(entry).use { input ->
                        val bitmap = BitmapFactory.decodeStream(input)
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            loadedPages[pageIndex] = imageBitmap
                            android.util.Log.d("CodexComic", "Loaded page ${pageIndex + 1}")
                            return imageBitmap
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CodexComic", "Failed to load page $pageIndex: ${e.message}", e)
        }

        return null
    }

    // Page index mapping for RTL support
    val isRTL = comicReadingDirection == "RTL"

    // For RTL, we reverse the page order so page 0 becomes the last physical page
    val mapLogicalToPhysicalPage = { logicalPage: Int ->
        if (isRTL && totalPages > 0) totalPages - 1 - logicalPage else logicalPage
    }

    val mapPhysicalToLogicalPage = { physicalPage: Int ->
        if (isRTL && totalPages > 0) totalPages - 1 - physicalPage else physicalPage
    }

    // Calculate initial positions based on reading direction
    val pagerInitialPage = remember(comicReadingDirection, totalPages) {
        if (totalPages > 0 && comicReadingDirection == "RTL") {
            totalPages - 1  // Start at last physical page for RTL (shows first logical page)
        } else {
            0  // Start at first physical page for LTR
        }
    }

    val lazyInitialIndex = remember(comicReadingDirection, totalPages) {
        if (totalPages > 0 && comicReadingDirection == "RTL") {
            totalPages - 1  // Start at last physical index for RTL
        } else {
            0  // Start at first physical index for LTR
        }
    }

    // Pager state for paged mode - uses calculated initial page
    val pagerState = rememberPagerState(
        initialPage = pagerInitialPage,
        pageCount = { maxOf(1, totalPages) }
    )

    // Lazy list state for webtoon mode - uses calculated initial index
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = lazyInitialIndex)

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

    // Update current page when pager changes
    LaunchedEffect(pagerState, isRTL) {
        snapshotFlow { pagerState.currentPage }.collect { physicalPage ->
            if (comicReaderMode == "PAGED" && totalPages > 0) {
                val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                onPageChanged(logicalPage)
            }
        }
    }

    // Update current page when lazy list changes (webtoon mode)
    LaunchedEffect(lazyListState, isRTL) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { physicalIndex ->
            if (comicReaderMode == "WEBTOON" && totalPages > 0) {
                val logicalPage = mapPhysicalToLogicalPage(physicalIndex)
                onPageChanged(logicalPage)
            }
        }
    }



    // Load comic archive structure
    LaunchedEffect(book) {
        isLoading = true
        errorMessage = null

        try {
            withContext(Dispatchers.IO) {
                android.util.Log.d("CodexComic", "Starting to load comic: ${book.title}")
                android.util.Log.d("CodexComic", "Book filePath: ${book.filePath}")

                val uri = Uri.parse(book.filePath)
                android.util.Log.d("CodexComic", "Parsed URI: $uri")

                val cachedFile = CachedFile(context, uri)
                android.util.Log.d("CodexComic", "CachedFile created: ${cachedFile.name}, path: ${cachedFile.path}, size: ${cachedFile.size}")

                val archiveReader = ArchiveReader()
                android.util.Log.d("CodexComic", "ArchiveReader created")

                val archive = archiveReader.openArchive(cachedFile)
                android.util.Log.d("CodexComic", "Archive opened, entries: ${archive.entries.size}")

                // Count image entries for total pages
                val imageEntries = archive.entries.filter { entry ->
                    val entryPath = entry.getPath()
                    entryPath != null && ArchiveReader.isImageFile(entryPath)
                }

                android.util.Log.d("CodexComic", "Found ${imageEntries.size} image pages")
                archiveHandle = archive
                totalPages = imageEntries.size
                comicLoaded = true
                onTotalPagesLoaded(imageEntries.size)

                // Pre-load the first few pages for smooth UX
                for (i in 0 until min(3, imageEntries.size)) {
                    loadPage(i)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CodexComic", "Failed to load comic archive", e)
            errorMessage = "Failed to load comic: ${e.message}"
        } finally {
            android.util.Log.d("CodexComic", "Comic archive loading finished")
            isLoading = false
            initialLoadComplete = true
            onLoadingComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoading) {
            // Loading state - could add a loading indicator here
            Box(modifier = Modifier.fillMaxSize())
        } else if (errorMessage != null) {
            // Error state - could add error UI here
            Box(modifier = Modifier.fillMaxSize())
                } else if (totalPages > 0) {
            // Track pager state changes and notify parent
            LaunchedEffect(pagerState.currentPage) {
                onPageChanged(pagerState.currentPage)
            }

            // When parent requests a specific page, scroll to it (only after initial load)
            LaunchedEffect(currentPage, initialLoadComplete) {
                if (initialLoadComplete && currentPage >= 0 && currentPage < totalPages) {
                    val targetPhysicalPage = mapLogicalToPhysicalPage(currentPage)
                    if (targetPhysicalPage != pagerState.currentPage) {
                        pagerState.animateScrollToPage(targetPhysicalPage)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (comicReaderMode == "PAGED") {
                    // Paged mode - horizontal pager
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
                        // For RTL, we need to load the logical page corresponding to this physical position
                        val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                        val pageImage = loadPage(logicalPage)
                        if (pageImage != null) {
                            ComicPage(
                                imageBitmap = pageImage,
                                comicReadingDirection = comicReadingDirection,
                                comicTapZone = comicTapZone,
                                showMenu = showMenu,
                                onPreviousPage = {
                                    scope.launch {
                                        if (pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                onNextPage = {
                                    scope.launch {
                                        if (pagerState.currentPage < totalPages - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                onMenuToggle = onMenuToggle,
                                comicInvertTaps = comicInvertTaps,
                                contentScale = contentScale
                            )
                        } else {
                            // Show loading placeholder for pages that haven't loaded yet
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Could add a loading indicator here if needed
                            }
                        }
                    }
                } else {
                    // Webtoon mode - vertical scrolling
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
                            key = { _, physicalPage -> physicalPage }
                        ) { _, physicalPage ->
                            // For RTL, we need to load the logical page corresponding to this physical position
                            val logicalPage = mapPhysicalToLogicalPage(physicalPage)
                            val pageImage = loadPage(logicalPage)
                            if (pageImage != null) {
                                // For webtoon, prefer FillWidth but respect user's contentScale choice
                                val webtoonContentScale = if (contentScale == ContentScale.Fit) ContentScale.FillWidth else contentScale
                                Image(
                                    bitmap = pageImage,
                                    contentDescription = null,
                                    contentScale = webtoonContentScale,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(comicTapZone, showMenu) {
                                            detectTapGestures { offset ->
                                                // For webtoon, just handle menu toggle on tap
                                                if (!showMenu) {
                                                    onMenuToggle()
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

@Composable
fun ComicProgressBar(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    if (totalPages > 0) {
        val percentage = ((currentPage + 1).toFloat() / totalPages * 100).toInt()
        val progressText = "$percentage% (${currentPage + 1}/$totalPages)"

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            StyledText(
                text = progressText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ComicPage(
    imageBitmap: ImageBitmap,
    comicReadingDirection: String,
    comicTapZone: Int,
    showMenu: Boolean = false,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onMenuToggle: () -> Unit = {},
    comicInvertTaps: String = "NONE",
    contentScale: ContentScale = ContentScale.Fit
) {
    val isRTL = comicReadingDirection == "RTL"

    // Apply tap inversion logic
    val shouldInvertHorizontal = comicInvertTaps == "HORIZONTAL" || comicInvertTaps == "BOTH"

    // Create adjusted callbacks based on inversion setting
    val adjustedOnPreviousPage: () -> Unit = if (shouldInvertHorizontal) onNextPage else onPreviousPage
    val adjustedOnNextPage: () -> Unit = if (shouldInvertHorizontal) onPreviousPage else onNextPage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(comicTapZone, isRTL, showMenu) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val x = offset.x
                    val y = offset.y

                    var handledNavigation = false

                    // Only allow navigation if menu is not open
                    if (!showMenu) {
                        when (comicTapZone) {
                            0 -> { // Default
                                if (x < width * 0.2f) {
                                    if (isRTL) adjustedOnNextPage() else adjustedOnPreviousPage()
                                    handledNavigation = true
                                } else if (x > width * 0.8f) {
                                    if (isRTL) adjustedOnPreviousPage() else adjustedOnNextPage()
                                    handledNavigation = true
                                }
                            }
                            1 -> { // L-shaped navigation
                                if (x < width * 0.3f || (x < width * 0.5f && y > height * 0.7f)) {
                                    if (isRTL) adjustedOnNextPage() else adjustedOnPreviousPage()
                                    handledNavigation = true
                                } else if (x > width * 0.7f || (x > width * 0.5f && y > height * 0.7f)) {
                                    if (isRTL) adjustedOnPreviousPage() else adjustedOnNextPage()
                                    handledNavigation = true
                                }
                            }
                            2 -> { // Kindle-ish
                                if (x < width * 0.2f) {
                                    if (isRTL) adjustedOnNextPage() else adjustedOnPreviousPage()
                                    handledNavigation = true
                                } else if (x > width * 0.8f) {
                                    if (isRTL) adjustedOnPreviousPage() else adjustedOnNextPage()
                                    handledNavigation = true
                                }
                            }
                            3 -> { // Edge
                                if (x < width * 0.1f) {
                                    if (isRTL) adjustedOnNextPage() else adjustedOnPreviousPage()
                                    handledNavigation = true
                                } else if (x > width * 0.9f) {
                                    if (isRTL) adjustedOnPreviousPage() else adjustedOnNextPage()
                                    handledNavigation = true
                                }
                            }
                            4 -> { // Right and left
                                if (x < width * 0.5f) {
                                    if (isRTL) adjustedOnNextPage() else adjustedOnPreviousPage()
                                } else {
                                    if (isRTL) adjustedOnPreviousPage() else adjustedOnNextPage()
                                }
                                handledNavigation = true
                            }
                            // 5 = Disabled, no navigation
                        }
                    }

                    // If tap wasn't handled by navigation zones, toggle menu
                    if (!handledNavigation) {
                        onMenuToggle()
                    }
                }
            }
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}