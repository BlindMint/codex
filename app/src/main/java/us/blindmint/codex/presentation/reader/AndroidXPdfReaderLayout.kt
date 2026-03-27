/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import android.graphics.Color as AndroidColor
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
    isSearchVisible: Boolean = false,
    isInverseColorEnabled: Boolean = false
) {
    val context = LocalContext.current
    
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var pageLoaded by remember { mutableIntStateOf(0) }
    
    val pdfUri = remember(book.filePath) {
        Uri.parse(book.filePath)
    }

    val inverseColorFilter = remember(isInverseColorEnabled) {
        if (isInverseColorEnabled) {
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
        } else null
    }

    val effectiveBackgroundColor = if (isInverseColorEnabled) Color.Black else backgroundColor

    AndroidView(
        modifier = modifier
            .then(
                if (inverseColorFilter != null) {
                    Modifier.graphicsLayer(colorFilter = inverseColorFilter)
                } else Modifier
            )
            .then(
                Modifier.background(effectiveBackgroundColor)
            ),
        factory = { ctx ->
            androidx.pdf.viewer.PdfViewer(ctx).apply {
                setBackgroundColor(if (isInverseColorEnabled) AndroidColor.BLACK else AndroidColor.WHITE)
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
