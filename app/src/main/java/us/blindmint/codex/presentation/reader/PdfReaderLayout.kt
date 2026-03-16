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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
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

@Composable
fun PdfReaderLayout(
    book: Book,
    currentPage: Int,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit,
    backgroundColor: Color,
    readingDirection: String = "LTR",
    comicScaleType: Int = 1,
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

    // Lazy loading cache - stores Pair of (ImageBitmap for display, Bitmap for cleanup)
    // Using LinkedHashMap for LRU eviction order
    val MAX_CACHED_PAGES = 50
    val PREFETCH_PAGES = 5
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
    val renderMutex = remember { Mutex() }

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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize())
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = backgroundColor,
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
                contentPadding = PaddingValues(0.dp),
                backgroundColor = backgroundColor,
                readingDirection = readingDirection,
                readerMode = if (readingDirection == "VERTICAL") "WEBTOON" else "PAGED",
                comicScaleType = comicScaleType,
                showMenu = showMenu,
                showPageIndicator = showPageIndicator,
                onLoadingComplete = onLoadingComplete,
                onScrollRestorationComplete = onScrollRestorationComplete,
                onMenuToggle = onMenuToggle,
                onPageSelected = onPageSelected,
                loadPage = { pageIndex ->
                    pdfRenderer?.let { renderPage(pageIndex, it) }
                }
            )
        }
    }
}
