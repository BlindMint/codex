/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.engine

import android.graphics.Bitmap
import com.artifex.mupdf.fitz.DisplayList
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.blindmint.codex.pdf.model.PdfPageGeometry
import us.blindmint.codex.pdf.model.PdfTextSelection
import us.blindmint.codex.pdf.model.PdfCropBounds
import us.blindmint.codex.pdf.model.PdfTileRequest
import us.blindmint.codex.pdf.model.PdfViewport
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Android's RecordingCanvas rejects bitmaps larger than 100 MiB; stay under that.
private const val MAX_RENDER_BYTES = 96 * 1024 * 1024 // 96 MiB (ARGB_8888 = 4 bytes/pixel)

class PdfDocumentSession private constructor(
    private val document: Document
) {
    private val mutex = Mutex()

    suspend fun countPages(): Int = mutex.withLock {
        document.countPages()
    }

    suspend fun loadPageGeometry(pageIndex: Int): PdfPageGeometry = mutex.withLock {
        val page = document.loadPage(pageIndex)
        try {
            val bounds = page.bounds
            PdfPageGeometry(
                pageIndex = pageIndex,
                width = bounds.x1 - bounds.x0,
                height = bounds.y1 - bounds.y0
            )
        } finally {
            page.destroy()
        }
    }

    suspend fun renderPagePreview(
        pageIndex: Int,
        viewport: PdfViewport,
        zoomScale: Float,
        invertColors: Boolean
    ): Bitmap? = mutex.withLock {
        val page = document.loadPage(pageIndex)
        try {
            val matrix = page.createRenderMatrix(viewport, zoomScale)
            AndroidDrawDevice.drawPage(page, matrix)?.also { bitmap ->
                if (invertColors) {
                    bitmap.invertLuminanceCompat()
                }
            }
        } finally {
            page.destroy()
        }
    }

    suspend fun renderTile(request: PdfTileRequest): Bitmap? = mutex.withLock {
        val page = document.loadPage(request.pageIndex)
        try {
            val matrix = page.createRenderMatrix(request.viewport, request.zoomScale)
            val displayList = page.toDisplayList()
            try {
                renderTileFromDisplayList(displayList, matrix, request)
            } finally {
                displayList.destroy()
            }
        } finally {
            page.destroy()
        }
    }

    suspend fun estimateCropBounds(pageIndex: Int): PdfCropBounds = mutex.withLock {
        val page = document.loadPage(pageIndex)
        try {
            val bounds = page.bounds
            val width = (bounds.x1 - bounds.x0).coerceAtLeast(1f)
            val height = (bounds.y1 - bounds.y0).coerceAtLeast(1f)

            // Conservative first-pass heuristic: trim modest outer margins only on pages
            // with obvious whitespace potential. This is intentionally safe until we add a
            // content-analysis pass driven by raster/text blocks.
            val horizontalMarginRatio = if (width > height * 0.7f) 0.03f else 0.05f
            val verticalMarginRatio = 0.03f

            PdfCropBounds(
                leftRatio = horizontalMarginRatio,
                topRatio = verticalMarginRatio,
                rightRatio = 1f - horizontalMarginRatio,
                bottomRatio = 1f - verticalMarginRatio
            )
        } finally {
            page.destroy()
        }
    }

    suspend fun findWordAtBitmapPoint(
        pageIndex: Int,
        bitmapX: Float,
        bitmapY: Float,
        viewport: PdfViewport,
        zoomScale: Float
    ): PdfTextSelection? = mutex.withLock {
        val page = document.loadPage(pageIndex)
        try {
            val matrix = page.createRenderMatrix(viewport, zoomScale)
            val pageX = bitmapX / matrix.a
            val pageY = bitmapY / matrix.d
            val structuredText = page.toStructuredText()
            try {
                structuredText.findWordAtPoint(pageX, pageY)
            } finally {
                structuredText.destroy()
            }
        } finally {
            page.destroy()
        }
    }

    fun close() {
        document.destroy()
    }

    companion object {
        fun open(path: String): PdfDocumentSession {
            return PdfDocumentSession(Document.openDocument(path))
        }
    }
}

private fun Page.createRenderMatrix(viewport: PdfViewport, zoomScale: Float): Matrix {
    val bounds = bounds
    val pageWidth = (bounds.x1 - bounds.x0).coerceAtLeast(1f)
    val pageHeight = (bounds.y1 - bounds.y0).coerceAtLeast(1f)

    val fitScale = minOf(
        viewport.widthPx / pageWidth,
        viewport.heightPx / pageHeight
    )

    val maxScaleByBytes = sqrt(MAX_RENDER_BYTES / 4f / (pageWidth * pageHeight))
    val targetScale = (fitScale * zoomScale)
        .coerceAtLeast(1f / 72f)
        .coerceAtMost(maxScaleByBytes)
    return Matrix(targetScale, targetScale)
}

private fun renderTileFromDisplayList(
    displayList: DisplayList,
    matrix: Matrix,
    request: PdfTileRequest
): Bitmap? {
    val tileRect = request.tileRect
    if (tileRect.widthPx <= 0 || tileRect.heightPx <= 0) {
        return null
    }

    val scissor = Rect(
        tileRect.leftPx.toFloat(),
        tileRect.topPx.toFloat(),
        (tileRect.leftPx + tileRect.widthPx).toFloat(),
        (tileRect.topPx + tileRect.heightPx).toFloat()
    )

    val bitmap = Bitmap.createBitmap(
        tileRect.widthPx,
        tileRect.heightPx,
        Bitmap.Config.ARGB_8888
    )

    val device = AndroidDrawDevice(bitmap, tileRect.leftPx, tileRect.topPx)
    try {
        displayList.run(device, matrix, scissor, null)
    } catch (_: Throwable) {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return null
    }

    if (request.invertColors) {
        bitmap.invertLuminanceCompat()
    }

    return bitmap
}

private fun Bitmap.invertLuminanceCompat() {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    for (index in pixels.indices) {
        val color = pixels[index]
        val a = color ushr 24 and 0xFF
        val r = 255 - (color ushr 16 and 0xFF)
        val g = 255 - (color ushr 8 and 0xFF)
        val b = 255 - (color and 0xFF)
        pixels[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    setPixels(pixels, 0, width, 0, 0, width, height)
}

private fun StructuredText.findWordAtPoint(pageX: Float, pageY: Float): PdfTextSelection? {
    for (block in getBlocks()) {
        for (line in block.lines) {
            val bbox = line.bbox
            if (pageY < bbox.y0 || pageY > bbox.y1) continue

            val chars = line.chars
            if (chars.isEmpty()) continue

            val lineText = chars.map { it.c.toChar() }.joinToString("")
            var tapIndex = chars.indexOfFirst { ch ->
                !ch.isWhitespace() && pageX >= ch.quad.ul_x && pageX <= ch.quad.ur_x
            }

            if (tapIndex == -1) {
                var minDistance = Float.MAX_VALUE
                chars.forEachIndexed { index, ch ->
                    if (!ch.isWhitespace()) {
                        val midX = (ch.quad.ul_x + ch.quad.ur_x) / 2f
                        val distance = kotlin.math.abs(midX - pageX)
                        if (distance < minDistance) {
                            minDistance = distance
                            tapIndex = index
                        }
                    }
                }
            }

            if (tapIndex == -1) continue

            var start = tapIndex
            var end = tapIndex
            while (start > 0 && !chars[start - 1].isWhitespace()) start--
            while (end < chars.size - 1 && !chars[end + 1].isWhitespace()) end++

            val word = lineText.substring(start, end + 1).trim()
            if (word.isNotBlank()) {
                var left = Float.MAX_VALUE
                var right = Float.MIN_VALUE
                var top = Float.MAX_VALUE
                var bottom = Float.MIN_VALUE
                for (index in start..end) {
                    val ch = chars[index]
                    left = minOf(left, ch.quad.ul_x, ch.quad.ll_x)
                    right = maxOf(right, ch.quad.ur_x, ch.quad.lr_x)
                    top = minOf(top, ch.quad.ul_y, ch.quad.ur_y)
                    bottom = maxOf(bottom, ch.quad.ll_y, ch.quad.lr_y)
                }
                return PdfTextSelection(
                    word = word,
                    lineText = lineText,
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom
                )
            }
        }
    }

    return null
}
