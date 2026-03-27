/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.net.Uri
import android.os.Bundle
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
import com.artifex.mupdf.viewer.DocumentView
import us.blindmint.codex.domain.library.book.Book

@Composable
fun MuPdfReaderLayout(
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
    var documentViewRef by remember { mutableStateOf<DocumentView?>(null) }

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
            DocumentView(ctx).apply {
                addPageChangeListener(object : DocumentView.PageChangeListener {
                    override fun onPageChanged(page: Int, pageCount: Int) {
                        onPageChanged(page)
                    }

                    override fun onPageCountChanged(pageCount: Int) {
                        totalPages = pageCount
                        onTotalPagesLoaded(pageCount)
                        isLoading = false
                        onLoadingComplete()
                    }

                    override fun onZoomChanged(zoom: Float) {}
                })
                documentViewRef = this
            }
        },
        update = { docView ->
            try {
                val options = Bundle().apply {
                    putParcelable(DocumentView.EXTRA_FILE_URI, pdfUri)
                    putInt(DocumentView.EXTRA_PAGE_NUMBER, initialPage)
                    putBoolean(DocumentView.EXTRA_SHOW_SEARCH, isSearchVisible)
                }
                docView.loadDocument(context, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            documentViewRef?.release()
        }
    }
}
