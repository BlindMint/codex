/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.render

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.blindmint.codex.pdf.engine.PdfDocumentSession
import us.blindmint.codex.pdf.model.PdfRenderRequest
import us.blindmint.codex.pdf.model.PdfRenderResult
import us.blindmint.codex.pdf.model.PdfTileBitmap
import us.blindmint.codex.pdf.model.PdfTileRequest

class PdfRenderController(
    private val session: PdfDocumentSession,
    private val cache: PdfBitmapCache = PdfBitmapCache(),
    private val tileCache: MutableMap<PdfTileRequest, Bitmap> = LinkedHashMap()
) {
    private val mutex = Mutex()
    private var previewCacheHits = 0
    private var previewCacheMisses = 0
    private var tileCacheHits = 0
    private var tileCacheMisses = 0

    suspend fun render(request: PdfRenderRequest): PdfRenderResult = mutex.withLock {
        val cached = cache.get(request)
        if (cached != null && !cached.isRecycled) {
            previewCacheHits += 1
            return@withLock PdfRenderResult(
                pageIndex = request.pageIndex,
                bitmap = cached,
                request = request
            )
        }

        previewCacheMisses += 1

        val bitmap = session.renderPagePreview(
            pageIndex = request.pageIndex,
            viewport = request.viewport,
            zoomScale = request.zoomScale
        ) ?: error("Failed to render page ${request.pageIndex}")

        cache.put(request, bitmap)

        PdfRenderResult(
            pageIndex = request.pageIndex,
            bitmap = bitmap,
            request = request
        )
    }

    suspend fun primeVisibleTiles(requests: List<PdfTileRequest>) {
        if (requests.isEmpty()) return

        requests.distinctBy {
            listOf(
                it.pageIndex,
                it.zoomScale,
                it.tileRect.leftPx,
                it.tileRect.topPx,
                it.tileRect.widthPx,
                it.tileRect.heightPx,
                it.invertColors
            )
        }.forEach { tileRequest ->
            renderTile(tileRequest)
        }
    }

    suspend fun renderTile(request: PdfTileRequest): PdfTileBitmap = mutex.withLock {
        val cached = tileCache[request]
        if (cached != null && !cached.isRecycled) {
            tileCacheHits += 1
            return@withLock PdfTileBitmap(request = request, bitmap = cached)
        }

        tileCacheMisses += 1

        val bitmap = session.renderTile(request) ?: return@withLock PdfTileBitmap(
            request = request,
            bitmap = render(request.toPreviewFallbackRequest()).bitmap
        )
        tileCache.put(request, bitmap)

        PdfTileBitmap(request = request, bitmap = bitmap)
    }

    suspend fun getVisibleTiles(requests: List<PdfTileRequest>): List<PdfTileBitmap> {
        return requests.map { renderTile(it) }
    }

    fun clear() {
        Log.d(
            "CodexPdf",
            "PdfRenderController stats: previewHits=$previewCacheHits previewMisses=$previewCacheMisses tileHits=$tileCacheHits tileMisses=$tileCacheMisses"
        )
        cache.clear()
        tileCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        tileCache.clear()
    }
}

private fun PdfTileRequest.toPreviewFallbackRequest(): PdfRenderRequest {
    return PdfRenderRequest(
        pageIndex = pageIndex,
        viewport = viewport,
        zoomScale = zoomScale,
        invertColors = invertColors
    )
}
