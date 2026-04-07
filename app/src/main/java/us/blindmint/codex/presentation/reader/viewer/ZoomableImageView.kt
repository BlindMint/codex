/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_MAX_SCALE = 5f

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    init {
        isClickable = true
        isLongClickable = true
        isFocusable = true
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    companion object {
        var sharedScale: Float = 1f
        var sharedOffsetX: Float = 0f
        var sharedOffsetY: Float = 0f
        var useSharedZoom: Boolean = false
        var sharedZoomInitialized: Boolean = false

        fun resetSharedZoom() {
            sharedScale = 1f
            sharedOffsetX = 0f
            sharedOffsetY = 0f
            sharedZoomInitialized = false
        }
    }

    data class GestureCallbacks(
        val onTap: ((x: Float, y: Float) -> Unit)? = null,
        val onLongPress: ((x: Float, y: Float) -> Unit)? = null,
        val onScaleChanged: ((scale: Float) -> Unit)? = null,
        val onInteractionStateChanged: ((isInteracting: Boolean) -> Unit)? = null
    )

    private var bitmap: Bitmap? = null
    private var fitRect = RectF()
    private var bitmapRect = RectF()
    private val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private var invertColors = false
    private var contentScaleMode = ContentScaleMode.FIT
    private var verticalMode = false
    private var maxScale = DEFAULT_MAX_SCALE
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var interactionClaimed = false
    private var multiTouchPanAllowed = false

    var callbacks: GestureCallbacks = GestureCallbacks()

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                callbacks.onTap?.invoke(e.x, e.y)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val point = mapViewToBitmap(e.x, e.y) ?: return
                callbacks.onLongPress?.invoke(point.first, point.second)
            }
        }
    )

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                claimInteraction()
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val effectiveScale = if (useSharedZoom) sharedScale else scale
                val effectiveOffsetX = if (useSharedZoom) sharedOffsetX else offsetX
                val effectiveOffsetY = if (useSharedZoom) sharedOffsetY else offsetY
                val oldScale = effectiveScale
                val newScale = (effectiveScale * detector.scaleFactor).coerceIn(1f, maxScale)
                if (abs(newScale - oldScale) < 0.0001f) return false

                val focusX = detector.focusX
                val focusY = detector.focusY
                val contentX = focusX - fitRect.left - effectiveOffsetX
                val contentY = focusY - fitRect.top - effectiveOffsetY
                val ratio = newScale / oldScale

                if (useSharedZoom) {
                    sharedScale = newScale
                    sharedOffsetX += contentX * (1f - ratio)
                    sharedOffsetY += contentY * (1f - ratio)
                } else {
                    scale = newScale
                    offsetX += contentX * (1f - ratio)
                    offsetY += contentY * (1f - ratio)
                }
                clampOffsets()
                callbacks.onScaleChanged?.invoke(if (useSharedZoom) sharedScale else scale)
                invalidate()
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                releaseInteractionIfNeeded()
            }
        }
    )

    fun setBitmap(bitmap: Bitmap?) {
        if (this.bitmap === bitmap) return
        this.bitmap = bitmap
        updateBaseGeometry()
        invalidate()
    }

    fun setInvertColors(enabled: Boolean) {
        if (invertColors == enabled) return
        invertColors = enabled
        drawPaint.colorFilter = if (enabled) {
            ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else {
            null
        }
        invalidate()
    }

    fun setContentScaleMode(mode: ContentScaleMode) {
        if (contentScaleMode == mode) return
        contentScaleMode = mode
        updateBaseGeometry()
        invalidate()
    }

    fun setVerticalMode(vertical: Boolean) {
        if (this.verticalMode == vertical) return
        this.verticalMode = vertical
        updateBaseGeometry()
        invalidate()
    }

    fun setUseSharedZoom(enabled: Boolean) {
        useSharedZoom = enabled
        if (enabled && !sharedZoomInitialized) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            sharedZoomInitialized = true
        } else if (enabled) {
            scale = sharedScale
            offsetX = sharedOffsetX
            offsetY = sharedOffsetY
        }
        invalidate()
    }

    fun setMaxScale(value: Float) {
        maxScale = value.coerceAtLeast(1f)
        if (scale > maxScale) {
            scale = maxScale
            if (useSharedZoom) {
                sharedScale = scale
            }
            clampOffsets()
            callbacks.onScaleChanged?.invoke(scale)
            invalidate()
        }
    }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        if (useSharedZoom) {
            sharedScale = 1f
            sharedOffsetX = 0f
            sharedOffsetY = 0f
        }
        callbacks.onScaleChanged?.invoke(scale)
        invalidate()
    }

    fun isZoomed(): Boolean = scale > 1.01f
    fun isInteracting(): Boolean = interactionClaimed

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBaseGeometry()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val activeBitmap = bitmap ?: return
        if (fitRect.isEmpty) {
            updateBaseGeometry()
        }
        val effectiveScale = if (useSharedZoom) sharedScale else scale
        val effectiveOffsetX = if (useSharedZoom) sharedOffsetX else offsetX
        val effectiveOffsetY = if (useSharedZoom) sharedOffsetY else offsetY
        drawMatrix.reset()
        val baseScaleX = fitRect.width() / activeBitmap.width.toFloat()
        val baseScaleY = fitRect.height() / activeBitmap.height.toFloat()
        drawMatrix.postScale(baseScaleX * effectiveScale, baseScaleY * effectiveScale)
        drawMatrix.postTranslate(fitRect.left + effectiveOffsetX, fitRect.top + effectiveOffsetY)
        canvas.drawBitmap(activeBitmap, drawMatrix, drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(event.pointerCount > 1 || isZoomed())
        val gestureHandled = gestureDetector.onTouchEvent(event)
        val scaleHandled = scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                multiTouchPanAllowed = true
                claimInteraction()
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val index = event.findPointerIndex(activePointerId)
                    if (index >= 0) {
                        val x = event.getX(index)
                        val y = event.getY(index)
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        val effectiveScale = if (useSharedZoom) sharedScale else scale
                        val effectiveOffsetX = if (useSharedZoom) sharedOffsetX else offsetX
                        val effectiveOffsetY = if (useSharedZoom) sharedOffsetY else offsetY
                        if (effectiveScale > 1.01f) {
                            if (!isDragging && (abs(dx) > slop || abs(dy) > slop)) {
                                isDragging = true
                                claimInteraction()
                            }
                            if (isDragging) {
                                val effectiveDx = if (multiTouchPanAllowed) dx else 0f
                                val newOffsetX = effectiveOffsetX + effectiveDx
                                val newOffsetY = effectiveOffsetY + dy
                                if (useSharedZoom) {
                                    sharedOffsetX = newOffsetX
                                    sharedOffsetY = newOffsetY
                                } else {
                                    offsetX = newOffsetX
                                    offsetY = newOffsetY
                                }
                                clampOffsets()
                                invalidate()
                            }
                        }
                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == activePointerId) {
                    val newIndex = if (event.actionIndex == 0) 1 else 0
                    if (newIndex < event.pointerCount) {
                        activePointerId = event.getPointerId(newIndex)
                        lastTouchX = event.getX(newIndex)
                        lastTouchY = event.getY(newIndex)
                    }
                }
                if (event.pointerCount <= 2) {
                    multiTouchPanAllowed = false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isDragging = false
                multiTouchPanAllowed = false
                if (event.pointerCount <= 1) {
                    releaseInteractionIfNeeded()
                }
            }
        }

        super.onTouchEvent(event)
        return true
    }

    private fun updateBaseGeometry() {
        val activeBitmap = bitmap ?: return
        if (width <= 0 || height <= 0) return

        val bitmapWidth = activeBitmap.width.toFloat().coerceAtLeast(1f)
        val bitmapHeight = activeBitmap.height.toFloat().coerceAtLeast(1f)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val widthScale = viewWidth / bitmapWidth
        val heightScale = viewHeight / bitmapHeight
        val fitScale = when (contentScaleMode) {
            ContentScaleMode.CROP -> max(widthScale, heightScale)
            ContentScaleMode.FILL_WIDTH -> widthScale
            ContentScaleMode.FILL_HEIGHT -> heightScale
            ContentScaleMode.NONE -> 1f
            ContentScaleMode.FIT -> min(widthScale, heightScale)
        }

        val fittedWidth = bitmapWidth * fitScale
        val fittedHeight = bitmapHeight * fitScale
        if (verticalMode) {
            fitRect = RectF(
                0f,
                0f,
                fittedWidth,
                fittedHeight
            )
        } else {
            fitRect = RectF(
                (viewWidth - fittedWidth) / 2f,
                (viewHeight - fittedHeight) / 2f,
                (viewWidth + fittedWidth) / 2f,
                (viewHeight + fittedHeight) / 2f
            )
        }
        bitmapRect = RectF(0f, 0f, bitmapWidth, bitmapHeight)
        clampOffsets()
    }

    private fun clampOffsets() {
        if (fitRect.isEmpty) return
        val effectiveScale = if (useSharedZoom) sharedScale else scale
        val effectiveOffsetX = if (useSharedZoom) sharedOffsetX else offsetX
        val effectiveOffsetY = if (useSharedZoom) sharedOffsetY else offsetY
        val scaledWidth = fitRect.width() * effectiveScale
        val scaledHeight = fitRect.height() * effectiveScale
        val minX = min(0f, width - fitRect.left - scaledWidth)
        val maxX = max(0f, -fitRect.left)
        val minY = min(0f, height - fitRect.top - scaledHeight)
        val maxY = max(0f, -fitRect.top)
        val clampedX = effectiveOffsetX.coerceIn(minX, maxX)
        val clampedY = effectiveOffsetY.coerceIn(minY, maxY)
        if (useSharedZoom) {
            sharedOffsetX = clampedX
            sharedOffsetY = clampedY
        } else {
            offsetX = clampedX
            offsetY = clampedY
        }
    }

    private fun claimInteraction() {
        if (!interactionClaimed) {
            parent?.requestDisallowInterceptTouchEvent(true)
            interactionClaimed = true
            callbacks.onInteractionStateChanged?.invoke(true)
        }
    }

    private fun releaseInteractionIfNeeded() {
        if (interactionClaimed && !scaleGestureDetector.isInProgress) {
            val effectiveScale = if (useSharedZoom) sharedScale else scale
            val wasZoomed = effectiveScale > 1.01f
            parent?.requestDisallowInterceptTouchEvent(wasZoomed)
            callbacks.onInteractionStateChanged?.invoke(wasZoomed)
            interactionClaimed = wasZoomed
        }
    }

    private fun mapViewToBitmap(viewX: Float, viewY: Float): Pair<Float, Float>? {
        val activeBitmap = bitmap ?: return null
        drawMatrix.reset()
        val baseScaleX = fitRect.width() / activeBitmap.width.toFloat()
        val baseScaleY = fitRect.height() / activeBitmap.height.toFloat()
        drawMatrix.postScale(baseScaleX * scale, baseScaleY * scale)
        drawMatrix.postTranslate(fitRect.left + offsetX, fitRect.top + offsetY)
        if (!drawMatrix.invert(inverseMatrix)) return null

        val pts = floatArrayOf(viewX, viewY)
        inverseMatrix.mapPoints(pts)
        val bitmapX = pts[0]
        val bitmapY = pts[1]
        if (!bitmapRect.contains(bitmapX, bitmapY)) return null
        return bitmapX to bitmapY
    }
}

enum class ContentScaleMode {
    FIT,
    CROP,
    FILL_WIDTH,
    FILL_HEIGHT,
    NONE
}
