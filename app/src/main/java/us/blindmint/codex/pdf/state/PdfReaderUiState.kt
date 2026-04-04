/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.state

import androidx.compose.runtime.Immutable
import us.blindmint.codex.pdf.model.PdfPageLayout
import us.blindmint.codex.pdf.model.PdfViewport
import kotlin.math.max
import kotlin.math.min

@Immutable
data class PdfReaderUiState(
    val viewport: PdfViewport? = null,
    val zoomScale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val horizontalLockActive: Boolean = false,
    val isGestureActive: Boolean = false
)

fun PdfReaderUiState.withViewport(viewport: PdfViewport): PdfReaderUiState {
    val horizontalLockActive = zoomScale > 1.02f
    return copy(
        viewport = viewport,
        horizontalLockActive = horizontalLockActive
    )
}

fun PdfReaderUiState.applyZoom(newZoomScale: Float): PdfReaderUiState {
    val clampedZoom = newZoomScale.coerceIn(0.75f, 5f)
    return copy(
        zoomScale = clampedZoom,
        horizontalLockActive = clampedZoom > 1.02f,
        panX = if (clampedZoom <= 1.02f) 0f else panX,
        panY = if (clampedZoom <= 1.02f) 0f else panY.coerceAtMost(0f)
    )
}

fun PdfReaderUiState.applyPan(deltaX: Float, deltaY: Float): PdfReaderUiState {
    if (zoomScale <= 1.02f) {
        return copy(panX = 0f, panY = 0f, horizontalLockActive = false)
    }

    return copy(
        panX = if (horizontalLockActive) 0f else panX + deltaX,
        panY = panY + deltaY
    )
}

fun PdfReaderUiState.beginGesture(): PdfReaderUiState = copy(isGestureActive = true)

fun PdfReaderUiState.endGesture(): PdfReaderUiState = copy(
    isGestureActive = false,
    panX = if (horizontalLockActive) 0f else panX,
    panY = if (zoomScale <= 1.02f) 0f else panY
)

fun PdfReaderUiState.clampPan(layout: PdfPageLayout?): PdfReaderUiState {
    if (layout == null || viewport == null || zoomScale <= 1.02f) {
        return copy(panX = 0f, panY = 0f)
    }

    val maxPanX = max(0f, (layout.displayWidthPx - viewport.widthPx) / 2f)
    val maxPanY = max(0f, (layout.displayHeightPx - viewport.heightPx) / 2f)

    return copy(
        panX = if (horizontalLockActive) 0f else panX.coerceIn(-maxPanX, maxPanX),
        panY = panY.coerceIn(-maxPanY, maxPanY)
    )
}
