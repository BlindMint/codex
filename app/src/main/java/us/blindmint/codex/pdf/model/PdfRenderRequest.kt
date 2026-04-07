/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class PdfRenderRequest(
    val pageIndex: Int,
    val viewport: PdfViewport,
    val zoomScale: Float,
    val invertColors: Boolean = false
)

@Immutable
data class PdfRenderResult(
    val pageIndex: Int,
    val bitmap: Bitmap,
    val request: PdfRenderRequest
)

@Immutable
data class PdfTileBitmap(
    val request: PdfTileRequest,
    val bitmap: Bitmap
)
