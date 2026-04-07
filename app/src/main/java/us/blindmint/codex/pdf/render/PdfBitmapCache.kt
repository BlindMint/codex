/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.pdf.render

import android.graphics.Bitmap
import us.blindmint.codex.pdf.model.PdfRenderRequest

class PdfBitmapCache(
    private val maxEntries: Int = 12
) {
    // Do not recycle in removeEldestEntry — evicted bitmaps may still be mid-draw on the main
    // thread (e.g. during LazyColumn exit animations on mode switch), which causes
    // "Canvas: trying to use a recycled bitmap" crashes. Let GC reclaim them instead.
    private val cache = object : LinkedHashMap<PdfRenderRequest, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PdfRenderRequest, Bitmap>?): Boolean {
            return size > maxEntries
        }
    }

    @Synchronized
    fun get(request: PdfRenderRequest): Bitmap? = cache[request]

    @Synchronized
    fun put(request: PdfRenderRequest, bitmap: Bitmap): Bitmap? {
        return cache.put(request, bitmap)
    }

    @Synchronized
    fun clear() {
        cache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        cache.clear()
    }
}
