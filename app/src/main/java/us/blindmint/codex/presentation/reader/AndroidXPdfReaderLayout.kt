/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Color as AndroidColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import us.blindmint.codex.domain.library.book.Book

@Composable
fun AndroidXPdfReaderLayout(
    book: Book,
    currentPage: Int,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit,
    backgroundColor: androidx.compose.ui.graphics.Color,
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
    isSearchVisible: Boolean = false
) {
    val context = LocalContext.current
    
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var pageLoaded by remember { mutableIntStateOf(0) }
    
    val pdfUri = remember(book.filePath) {
        Uri.parse(book.filePath)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            androidx.pdf.viewer.PdfViewer(ctx).apply {
                setBackgroundColor(AndroidColor.WHITE)
                isScrollable = true
                shouldShowScrollbar = true
                
                addOnCurrentPageChangedListener { page ->
                    pageLoaded = page
                    onPageChanged(page)
                }
                
                addOnDocumentLoadSuccessListener { document ->
                    totalPages = document.pageCount
                    onTotalPagesLoaded(document.pageCount)
                    isLoading = false
                    onLoadingComplete()
                }
                
                addOnDocumentLoadErrorListener { error ->
                    isLoading = false
                }
            }
        },
        update = { pdfViewer ->
            pdfViewer.load(pdfUri)
        }
    )
}
