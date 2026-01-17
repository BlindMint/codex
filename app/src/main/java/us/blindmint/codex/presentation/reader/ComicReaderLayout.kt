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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.reader.ReaderEvent

@Composable
fun ComicReaderLayout(
    book: Book,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    contentPadding: PaddingValues,
    backgroundColor: Color,
    comicReadingDirection: String,
    comicTapZone: Int,
    showPageIndicator: Boolean = true,
    onLoadingComplete: () -> Unit = {},
    onMenuToggle: () -> Unit = {},
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

    // Pager state
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages }
    )

    // Update current page when pager changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                ) { page ->
                    val pageImage = loadPage(page)
                    if (pageImage != null) {
                        ComicPage(
                            imageBitmap = pageImage,
                            comicReadingDirection = comicReadingDirection,
                            comicTapZone = comicTapZone,
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
                            onMenuToggle = onMenuToggle
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
private fun ComicPage(
    imageBitmap: ImageBitmap,
    comicReadingDirection: String,
    comicTapZone: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onMenuToggle: () -> Unit = {}
) {
    val isRTL = comicReadingDirection == "RTL"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(comicTapZone, isRTL) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val x = offset.x
                    val y = offset.y

                    var handledNavigation = false

                    when (comicTapZone) {
                        0 -> { // Default
                            if (x < width * 0.2f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                                handledNavigation = true
                            } else if (x > width * 0.8f) {
                                if (isRTL) onPreviousPage() else onNextPage()
                                handledNavigation = true
                            }
                        }
                        1 -> { // L-shaped navigation
                            if (x < width * 0.3f || (x < width * 0.5f && y > height * 0.7f)) {
                                if (isRTL) onNextPage() else onPreviousPage()
                                handledNavigation = true
                            } else if (x > width * 0.7f || (x > width * 0.5f && y > height * 0.7f)) {
                                if (isRTL) onPreviousPage() else onNextPage()
                                handledNavigation = true
                            }
                        }
                        2 -> { // Kindle-ish
                            if (x < width * 0.2f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                                handledNavigation = true
                            } else if (x > width * 0.8f) {
                                if (isRTL) onPreviousPage() else onNextPage()
                                handledNavigation = true
                            }
                        }
                        3 -> { // Edge
                            if (x < width * 0.1f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                                handledNavigation = true
                            } else if (x > width * 0.9f) {
                                if (isRTL) onPreviousPage() else onNextPage()
                                handledNavigation = true
                            }
                        }
                        4 -> { // Right and left
                            if (x < width * 0.5f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                            } else {
                                if (isRTL) onPreviousPage() else onNextPage()
                            }
                            handledNavigation = true
                        }
                        // 5 = Disabled, no navigation
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
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}