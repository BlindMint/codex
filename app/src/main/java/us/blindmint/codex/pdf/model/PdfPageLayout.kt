/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.model

import androidx.compose.runtime.Immutable
import kotlin.math.max

@Immutable
data class PdfPageLayout(
    val pageIndex: Int,
    val displayWidthPx: Float,
    val displayHeightPx: Float,
    val contentTopPx: Float = 0f,
    val contentLeftPx: Float = 0f,
    val cropLeftRatio: Float = 0f,
    val cropTopRatio: Float = 0f,
    val cropRightRatio: Float = 1f,
    val cropBottomRatio: Float = 1f
)

fun PdfPageGeometry.createPageLayout(
    viewport: PdfViewport,
    zoomScale: Float,
    fitWidth: Boolean = true,
    cropLeftRatio: Float = 0f,
    cropTopRatio: Float = 0f,
    cropRightRatio: Float = 1f,
    cropBottomRatio: Float = 1f
): PdfPageLayout {
    val safeWidth = max(width, 1f)
    val safeHeight = max(height, 1f)
    val croppedWidth = max(safeWidth * (cropRightRatio - cropLeftRatio), 1f)
    val croppedHeight = max(safeHeight * (cropBottomRatio - cropTopRatio), 1f)

    val fitScale = if (fitWidth) {
        viewport.widthPx / croppedWidth
    } else {
        minOf(viewport.widthPx / croppedWidth, viewport.heightPx / croppedHeight)
    }

    val scaledWidth = croppedWidth * fitScale * zoomScale
    val scaledHeight = croppedHeight * fitScale * zoomScale

    return PdfPageLayout(
        pageIndex = pageIndex,
        displayWidthPx = scaledWidth,
        displayHeightPx = scaledHeight,
        contentTopPx = max(0f, (viewport.heightPx - scaledHeight) / 2f),
        contentLeftPx = max(0f, (viewport.widthPx - scaledWidth) / 2f),
        cropLeftRatio = cropLeftRatio,
        cropTopRatio = cropTopRatio,
        cropRightRatio = cropRightRatio,
        cropBottomRatio = cropBottomRatio
    )
}
