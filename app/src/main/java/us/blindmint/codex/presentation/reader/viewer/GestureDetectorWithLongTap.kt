/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader.viewer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

open class GestureDetectorWithLongTap(
    context: Context,
    private val listener: Listener,
) : GestureDetector(context, listener) {

    private val handler = Handler(Looper.getMainLooper())
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val longTapTime = ViewConfiguration.getLongPressTimeout().toLong()
    private val doubleTapTime = ViewConfiguration.getDoubleTapTimeout().toLong()

    private var downX = 0f
    private var downY = 0f
    private var lastUp = 0L
    private var lastDownEvent: MotionEvent? = null

    private val longTapFn = Runnable { listener.onLongTapConfirmed(lastDownEvent!!) }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastDownEvent?.recycle()
                lastDownEvent = MotionEvent.obtain(ev)
                if (ev.downTime - lastUp > doubleTapTime) {
                    downX = ev.x
                    downY = ev.y
                    handler.postDelayed(longTapFn, longTapTime)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) {
                    handler.removeCallbacks(longTapFn)
                }
            }
            MotionEvent.ACTION_UP -> {
                lastUp = ev.eventTime
                handler.removeCallbacks(longTapFn)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                handler.removeCallbacks(longTapFn)
            }
        }
        return super.onTouchEvent(ev)
    }

    open class Listener : GestureDetector.SimpleOnGestureListener() {
        open fun onLongTapConfirmed(ev: MotionEvent) {}
    }
}
