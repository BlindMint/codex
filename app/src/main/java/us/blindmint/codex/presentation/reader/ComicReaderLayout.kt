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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val mainModel = hiltViewModel<MainModel>()
    val scope = rememberCoroutineScope()

    // Comic pages state
    var comicPages by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pager state
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (comicPages.size - 1).coerceAtLeast(0)),
        pageCount = { comicPages.size }
    )

    // Update current page when pager changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
        }
    }

    // Load comic pages
    LaunchedEffect(book) {
        isLoading = true
        errorMessage = null

        try {
            withContext(Dispatchers.IO) {
                val cachedFile = CachedFile(context, Uri.parse(book.filePath))
                val archiveReader = ArchiveReader()

                archiveReader.openArchive(cachedFile).use { archive ->
                    val pages = mutableListOf<ImageBitmap>()

                    for (entry in archive.entries) {
                        if (ArchiveReader.isImageFile(entry.getPath())) {
                            try {
                                archive.getInputStream(entry).use { input ->
                                    val bitmap = BitmapFactory.decodeStream(input)
                                    bitmap?.let {
                                        pages.add(it.asImageBitmap())
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip corrupted images
                                continue
                            }
                        }
                    }

                    comicPages = pages
                }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load comic: ${e.message}"
        } finally {
            isLoading = false
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
        } else if (comicPages.isNotEmpty()) {
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
                val pageImage = comicPages.getOrNull(page)
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
                                if (pagerState.currentPage < comicPages.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
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
    onNextPage: () -> Unit
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

                    when (comicTapZone) {
                        0 -> { // Default
                            if (x < width * 0.2f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                            } else if (x > width * 0.8f) {
                                if (isRTL) onPreviousPage() else onNextPage()
                            }
                        }
                        1 -> { // L-shaped navigation
                            if (x < width * 0.3f || (x < width * 0.5f && y > height * 0.7f)) {
                                if (isRTL) onNextPage() else onPreviousPage()
                            } else if (x > width * 0.7f || (x > width * 0.5f && y > height * 0.7f)) {
                                if (isRTL) onPreviousPage() else onNextPage()
                            }
                        }
                        2 -> { // Kindle-ish
                            if (x < width * 0.2f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                            } else if (x > width * 0.8f) {
                                if (isRTL) onPreviousPage() else onNextPage()
                            }
                        }
                        3 -> { // Edge
                            if (x < width * 0.1f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                            } else if (x > width * 0.9f) {
                                if (isRTL) onPreviousPage() else onNextPage()
                            }
                        }
                        4 -> { // Right and left
                            if (x < width * 0.5f) {
                                if (isRTL) onNextPage() else onPreviousPage()
                            } else {
                                if (isRTL) onPreviousPage() else onNextPage()
                            }
                        }
                        // 5 = Disabled, no action
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