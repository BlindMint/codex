/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.model

import androidx.compose.runtime.Immutable
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Immutable
data class PdfTileRect(
    val leftPx: Int,
    val topPx: Int,
    val widthPx: Int,
    val heightPx: Int
)

@Immutable
data class PdfTileRequest(
    val pageIndex: Int,
    val viewport: PdfViewport,
    val zoomScale: Float,
    val tileRect: PdfTileRect,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val invertColors: Boolean = false
)

fun PdfPageLayout.visibleTileRequests(
    viewport: PdfViewport,
    zoomScale: Float,
    panX: Float,
    panY: Float,
    invertColors: Boolean,
    tileSizePx: Int = 768,
    prefetchMarginPx: Int = 256
): List<PdfTileRequest> {
    val contentWidth = max(displayWidthPx.toInt(), 1)
    val contentHeight = max(displayHeightPx.toInt(), 1)

    val viewportLeft = ((contentWidth - viewport.widthPx) / 2f - panX - prefetchMarginPx)
        .toInt()
        .coerceIn(0, max(contentWidth - 1, 0))
    val viewportTop = ((contentHeight - viewport.heightPx) / 2f - panY - prefetchMarginPx)
        .toInt()
        .coerceIn(0, max(contentHeight - 1, 0))
    val viewportRight = (viewportLeft + viewport.widthPx + prefetchMarginPx * 2)
        .coerceIn(viewportLeft + 1, contentWidth)
    val viewportBottom = (viewportTop + viewport.heightPx + prefetchMarginPx * 2)
        .coerceIn(viewportTop + 1, contentHeight)

    val visibleWidth = viewportRight - viewportLeft
    val visibleHeight = viewportBottom - viewportTop

    val tilesX = ceil(visibleWidth / tileSizePx.toFloat()).toInt().coerceAtLeast(1)
    val tilesY = ceil(visibleHeight / tileSizePx.toFloat()).toInt().coerceAtLeast(1)

    return buildList {
        for (y in 0 until tilesY) {
            for (x in 0 until tilesX) {
                val left = viewportLeft + x * tileSizePx
                val top = viewportTop + y * tileSizePx
                add(
                    PdfTileRequest(
                        pageIndex = pageIndex,
                        viewport = viewport,
                        zoomScale = zoomScale,
                        tileRect = PdfTileRect(
                            leftPx = left,
                            topPx = top,
                            widthPx = min(tileSizePx, viewportRight - left),
                            heightPx = min(tileSizePx, viewportBottom - top)
                        ),
                        panX = panX,
                        panY = panY,
                        invertColors = invertColors
                    )
                )
            }
        }
    }
}
