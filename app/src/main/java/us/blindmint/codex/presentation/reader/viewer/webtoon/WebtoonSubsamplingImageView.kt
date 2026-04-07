/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader.viewer.webtoon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet

class WebtoonSubsamplingImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    private var imageRect = RectF()
    private var imageBitmap: Bitmap? = null

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        return false
    }

    fun setImageBitmapAndRect(bitmap: Bitmap?, rect: RectF) {
        imageBitmap = bitmap
        imageRect = rect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        imageBitmap?.let { bitmap ->
            if (!imageRect.isEmpty) {
                canvas.drawBitmap(bitmap, null, imageRect, null)
            }
        }
    }
}
