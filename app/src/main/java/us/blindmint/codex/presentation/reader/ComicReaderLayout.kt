/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.parser.comic.ArchiveReader
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.reader.ReaderEvent
import kotlin.math.min

private const val MAX_CACHED_PAGES = 50
private const val PREFETCH_PAGES = 5

@OptIn(FlowPreview::class)
@Composable
fun ComicReaderLayout(
    book: Book,
    currentPage: Int,
    initialPage: Int = 0,
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
    modifier: Modifier = Modifier,
    showMenu: Boolean = false,
    showPageIndicator: Boolean = true,
    onLoadingComplete: () -> Unit = {},
    onScrollRestorationComplete: () -> Unit = {},
    onMenuToggle: () -> Unit = {},
    onTotalPagesLoaded: (Int) -> Unit = {},
    onPageSelected: (Int) -> Unit = {}
) {
    Log.d("ComicReaderLayout", "ComicReaderLayout called for: ${book.title}")
    Log.d("ComicReaderLayout", "  initialPage: $initialPage")
    Log.d("ComicReaderLayout", "  currentPage: $currentPage")

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

    // Lazy loading cache - stores Pair of (ImageBitmap for display, Bitmap for cleanup)
    // Using LinkedHashMap for LRU eviction order
    val loadedPages = remember {
        object : LinkedHashMap<Int, Pair<ImageBitmap, android.graphics.Bitmap>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Pair<ImageBitmap, android.graphics.Bitmap>>?): Boolean {
                if (size > MAX_CACHED_PAGES) {
                    eldest?.value?.second?.recycle() // Free native memory
                    return true
                }
                return false
            }
        }
    }

    // Function to load a specific page with high quality
    suspend fun loadPage(pageIndex: Int): ImageBitmap? {
        // Check if already loaded
        loadedPages[pageIndex]?.first?.let { return it }

        // Load the page with high quality options
        try {
            archiveHandle?.let { archive ->
                // archive.entries already only contains image files (filtered by ArchiveHandle)
                if (pageIndex < archive.entries.size) {
                    val entry = archive.entries[pageIndex]
                    Log.d("CodexComic", "Lazy loading page ${pageIndex + 1}")

                    archive.getInputStream(entry).use { input ->
                        // High quality bitmap options
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inSampleSize = 1  // Full resolution, no downsampling
                            inMutable = false
                        }
                        val bitmap = BitmapFactory.decodeStream(input, null, options)
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            loadedPages[pageIndex] = imageBitmap to bitmap
                            Log.d("CodexComic", "Loaded page ${pageIndex + 1} at full quality")
                            return imageBitmap
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("CodexComic", "Failed to load page $pageIndex: ${e.message}", e)
        }

        return null
    }

    // Load comic archive structure
    // Use book.id as key so we only reload when switching to a different comic,
    // not when the book object is updated (e.g., when progress is saved)
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
                Log.d("CodexComic", "Starting to load comic: ${book.title}")
                Log.d("CodexComic", "Book filePath: ${book.filePath}")

                val cachedFile = CachedFileFactory.fromBook(context, book)
                if (cachedFile == null) {
                    Log.e("CodexComic", "Failed to create CachedFile for comic")
                    errorMessage = "Failed to access comic file"
                    return@withContext
                }
                Log.d("CodexComic", "CachedFile created: ${cachedFile.name}, path: ${cachedFile.path}, size: ${cachedFile.size}")

                val archiveReader = ArchiveReader()
                Log.d("CodexComic", "ArchiveReader created")

                val archive = archiveReader.openArchive(cachedFile)
                Log.d("CodexComic", "Archive opened, entries: ${archive.entries.size}")

                // archive.entries already only contains image files (filtered by ArchiveHandle)
                Log.d("CodexComic", "Found ${archive.entries.size} image pages")
                archiveHandle = archive
                totalPages = archive.entries.size
                comicLoaded = true
                onTotalPagesLoaded(archive.entries.size)

                // Pre-load the first few pages for smooth UX
                for (i in 0 until min(3, archive.entries.size)) {
                    loadPage(i)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            Log.e("CodexComic", "Failed to load comic archive", e)
            errorMessage = "Failed to load comic: ${e.message}"
        } finally {
            Log.d("CodexComic", "Comic archive loading finished")
            isLoading = false
            initialLoadComplete = true
            onLoadingComplete()
        }
    }

    DisposableEffect(book.id) {
        onDispose {
            archiveHandle?.close()
            loadedPages.values.forEach { it.second.recycle() }
            loadedPages.clear()
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = fontColor,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (totalPages > 0) {
            ImageBasedReaderLayout(
                bookTitle = book.title,
                currentPage = currentPage,
                initialPage = initialPage,
                onPageChanged = onPageChanged,
                totalPages = totalPages,
                contentPadding = contentPadding,
                backgroundColor = backgroundColor,
                readingDirection = comicReadingDirection,
                readerMode = comicReaderMode,
                comicScaleType = comicScaleType,
                showMenu = showMenu,
                showPageIndicator = showPageIndicator,
                onLoadingComplete = onLoadingComplete,
                onScrollRestorationComplete = onScrollRestorationComplete,
                onMenuToggle = onMenuToggle,
                onPageSelected = onPageSelected,
                loadPage = { pageIndex ->
                    loadPage(pageIndex)
                }
            )
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

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset zoom when changing pages
    LaunchedEffect(imageBitmap) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Apply tap inversion logic
    val shouldInvertHorizontal = comicInvertTaps == "HORIZONTAL" || comicInvertTaps == "BOTH"

    // Create adjusted callbacks based on inversion setting
    val adjustedOnPreviousPage: () -> Unit = if (shouldInvertHorizontal) onNextPage else onPreviousPage
    val adjustedOnNextPage: () -> Unit = if (shouldInvertHorizontal) onPreviousPage else onNextPage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(comicTapZone, isRTL, showMenu) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Apply zoom with limits
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    
                    if (newScale > 1f) {
                        // When zoomed, allow panning
                        scale = newScale
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        // When not zoomed, handle tap navigation
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        val x = centroid.x
                        val y = centroid.y

                        var handledNavigation = false

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
                            }
                        }

                        if (!handledNavigation) {
                            onMenuToggle()
                        }
                    }
                }
            }
    ) {
        Image(
            bitmap = imageBitmap,
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
    }
}
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