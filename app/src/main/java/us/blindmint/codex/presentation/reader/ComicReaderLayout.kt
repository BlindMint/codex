/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.parser.comic.ArchiveReader
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.util.LocalActivity
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.ui.main.MainModel
import kotlin.math.min

private const val MAX_CACHED_PAGES = 50
private const val PREFETCH_PAGES = 5

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
    onPageSelected: (Int) -> Unit = {},
    invertColors: Boolean = false
) {
    val context = LocalContext.current
    val mainModel = hiltViewModel<MainModel>()
    val scope = rememberCoroutineScope()

    var archiveHandle by remember { mutableStateOf<ArchiveReader.ArchiveHandle?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loadedPages = remember {
        object : LinkedHashMap<Int, Pair<ImageBitmap, Bitmap>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Pair<ImageBitmap, Bitmap>>?): Boolean {
                return false
            }
        }
    }

    val loadMutex = remember { Mutex() }

    suspend fun loadPage(pageIndex: Int): ImageBitmap? {
        loadedPages[pageIndex]?.first?.let { return it }

        return loadMutex.withLock {
            loadedPages[pageIndex]?.first?.let { return@withLock it }

            try {
                archiveHandle?.let { archive ->
                    if (pageIndex < archive.entries.size) {
                        val entry = archive.entries[pageIndex]

                        archive.getInputStream(entry)?.use { input ->
                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val bitmap = BitmapFactory.decodeStream(input, null, options)
                            if (bitmap != null) {
                                val imageBitmap = bitmap.asImageBitmap()
                                loadedPages[pageIndex] = imageBitmap to bitmap
                                return@withLock imageBitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("CodexComic", "Failed to load page $pageIndex: ${e.message}", e)
            }

            null
        }
    }

    LaunchedEffect(book.id) {
        isLoading = true
        errorMessage = null
        loadedPages.values.forEach { it.second.recycle() }
        loadedPages.clear()
        archiveHandle?.close()
        archiveHandle = null
        totalPages = 0

        scope.launch {
            kotlinx.coroutines.delay(10000)
            if (isLoading) {
                errorMessage = "Loading timed out after 10 seconds"
                isLoading = false
            }
        }

        var archive: ArchiveReader.ArchiveHandle? = null
        try {
            withContext(Dispatchers.IO) {
                val cachedFile = CachedFileFactory.fromBook(context, book)
                if (cachedFile == null) {
                    errorMessage = "Failed to access comic file"
                    return@withContext
                }

                val archiveReader = ArchiveReader()
                archive = archiveReader.openArchive(cachedFile)

                archiveHandle = archive
                totalPages = archive.entries.size
                onTotalPagesLoaded(archive.entries.size)

                for (i in 0 until min(3, archive.entries.size)) {
                    loadPage(i)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            archive?.close()
            throw e
        } catch (e: Exception) {
            errorMessage = "Failed to load comic: ${e.message}"
            archive?.close()
        } finally {
            isLoading = false
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
            Box(modifier = Modifier.fillMaxSize())
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = fontColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (totalPages > 0) {
            ComicReaderDisplay(
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
                loadPage = { pageIndex -> loadPage(pageIndex) },
                invertColors = invertColors
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