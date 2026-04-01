/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.ui.reader.ReaderEvent
import kotlin.math.abs
import kotlin.math.min

private const val TAG = "CodexPdf"

/** MuPDF rendering scale. 2× gives clear, high-DPI output on most screens. */
private const val RENDER_SCALE = 2.0f

private const val MAX_CACHED_PAGES = 50

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
    onPageSelected: (Int) -> Unit = {},
    onTextSelected: (ReaderEvent.OnTextSelected) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var document by remember { mutableStateOf<Document?>(null) }
    // MuPDF Document is not thread-safe; serialize all access
    val documentMutex = remember { Mutex() }

    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // LRU cache: ImageBitmap for display + Bitmap for recycling
    val loadedPages = remember {
        object : LinkedHashMap<Int, Pair<ImageBitmap, Bitmap>>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<Int, Pair<ImageBitmap, Bitmap>>?
            ): Boolean {
                if (size > MAX_CACHED_PAGES) {
                    eldest?.value?.second?.recycle()
                    return true
                }
                return false
            }
        }
    }

    suspend fun renderPage(pageIndex: Int): ImageBitmap? {
        loadedPages[pageIndex]?.first?.let { return it }
        return documentMutex.withLock {
            try {
                val doc = document ?: return@withLock null
                val page = doc.loadPage(pageIndex)
                val bitmap = AndroidDrawDevice.drawPage(page, Matrix(RENDER_SCALE, RENDER_SCALE))
                page.destroy()
                bitmap?.let {
                    val imageBitmap = it.asImageBitmap()
                    loadedPages[pageIndex] = imageBitmap to it
                    imageBitmap
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to render page $pageIndex", e)
                null
            }
        }
    }

    LaunchedEffect(book.id) {
        isLoading = true
        errorMessage = null

        try {
            withContext(Dispatchers.IO) {
                val cachedFile = CachedFileFactory.fromBook(context, book) ?: run {
                    errorMessage = "Failed to access PDF file"
                    return@withContext
                }
                val rawFile = cachedFile.rawFile ?: run {
                    errorMessage = "Failed to cache PDF file"
                    return@withContext
                }

                val doc = Document.openDocument(rawFile.absolutePath)
                document = doc
                val pageCount = doc.countPages()
                totalPages = pageCount
                onTotalPagesLoaded(pageCount)
                Log.d(TAG, "Opened PDF '${book.title}' — $pageCount pages")

                for (i in 0 until min(3, pageCount)) {
                    renderPage(i)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to open PDF", e)
            errorMessage = "Failed to load PDF: ${e.message}"
        } finally {
            isLoading = false
            onLoadingComplete()
        }
    }

    DisposableEffect(book.id) {
        onDispose {
            document?.destroy()
            document = null
            loadedPages.values.forEach { it.second.recycle() }
            loadedPages.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize())

            errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            totalPages > 0 -> ImageBasedReaderLayout(
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
                loadPage = { renderPage(it) },
                onLongPress = { pageIndex, bitmapX, bitmapY ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            val result = documentMutex.withLock {
                                val doc = document ?: return@withLock null
                                val page = doc.loadPage(pageIndex)
                                val structText = page.toStructuredText()
                                val pageX = bitmapX / RENDER_SCALE
                                val pageY = bitmapY / RENDER_SCALE
                                val wordAndLine = findWordAtPoint(structText, pageX, pageY)
                                structText.destroy()
                                page.destroy()
                                wordAndLine
                            }
                            result?.let { (word, lineText) ->
                                withContext(Dispatchers.Main) {
                                    onTextSelected(
                                        ReaderEvent.OnTextSelected(
                                            selectedText = word,
                                            paragraphText = lineText
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Text extraction failed at ($bitmapX, $bitmapY)", e)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Finds the word at the given page-coordinate point using MuPDF StructuredText.
 * Returns a (word, fullLineText) pair, or null if no text is at that position.
 */
private fun findWordAtPoint(
    structuredText: StructuredText,
    pageX: Float,
    pageY: Float
): Pair<String, String>? {
    for (block in structuredText.getBlocks()) {
        for (line in block.lines) {
            val bbox = line.bbox
            // Check if tap Y falls within this line's vertical bounds
            if (pageY < bbox.y0 || pageY > bbox.y1) continue

            val chars = line.chars
            if (chars.isEmpty()) continue

            val lineText = chars.map { it.c.toChar() }.joinToString("")

            // Find the character directly under the tap X
            var tapIndex = chars.indexOfFirst { ch ->
                !ch.isWhitespace() && pageX >= ch.quad.ul_x && pageX <= ch.quad.ur_x
            }

            // Fall back to the closest non-whitespace character
            if (tapIndex == -1) {
                var minDist = Float.MAX_VALUE
                chars.forEachIndexed { i, ch ->
                    if (!ch.isWhitespace()) {
                        val midX = (ch.quad.ul_x + ch.quad.ur_x) / 2f
                        val dist = abs(midX - pageX)
                        if (dist < minDist) {
                            minDist = dist
                            tapIndex = i
                        }
                    }
                }
            }

            if (tapIndex == -1) continue

            // Expand left and right to word boundaries (whitespace = boundary)
            var start = tapIndex
            var end = tapIndex
            while (start > 0 && !chars[start - 1].isWhitespace()) start--
            while (end < chars.size - 1 && !chars[end + 1].isWhitespace()) end++

            val word = lineText.substring(start, end + 1).trim()
            if (word.isNotBlank()) return word to lineText
        }
    }
    return null
}
