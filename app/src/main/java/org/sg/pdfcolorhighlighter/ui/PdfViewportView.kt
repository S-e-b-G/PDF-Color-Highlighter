// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import org.sg.pdfcolorhighlighter.domain.PageViewportState
import org.sg.pdfcolorhighlighter.domain.PanGestureTracker
import org.sg.pdfcolorhighlighter.domain.ViewportGeometry

/** M2a.1: explicit fresh-gesture reset, independent one-finger pan and debug-only event tracing. */
class PdfViewportView(context: Context) : View(context) {
    private var image: Bitmap? = null
    private var logicalPageWidth = 1f
    private var logicalPageHeight = 1f
    private var logicalKey = 0L
    private var gestureReported = false
    var onRenderViewport: ((Int, Int, Float) -> Unit)? = null
    var onRenderGesture: ((Boolean) -> Unit)? = null
    private val geometry = ViewportGeometry()
    private var pendingState = PageViewportState()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val target = RectF()
    private val pan = PanGestureTracker()
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val debug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val traceId = Integer.toHexString(System.identityHashCode(this))
    private var hadMultiplePointers = false
    private var downTime = 0L
    private var lastMoveLog = 0L
    var onViewportChanged: ((PageViewportState) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                val accepted = isEnabled && image != null && width > 0 && height > 0
                trace("scale-begin accepted=$accepted")
                return accepted
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                geometry.zoomBy(detector.scaleFactor, detector.focusX, detector.focusY)
                changed()
                return true
            }
            override fun onScaleEnd(detector: ScaleGestureDetector) { trace("scale-end") }
        }).apply { setQuickScaleEnabled(false); setStylusScaleEnabled(false) }

    init { setBackgroundColor(Color.rgb(225, 228, 232)); isFocusable = true; isClickable = true }

    private fun trace(message: String) {
        if (debug) Log.d("PdfViewport", "M2a.1 view=$traceId $message enabled=$isEnabled size=${width}x$height zoom=${geometry.state.zoom}")
    }
    private fun resetInput(reason: String, cancelScale: Boolean = true) {
        if (cancelScale) {
            val now = SystemClock.uptimeMillis()
            val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
            try { scaleDetector.onTouchEvent(cancel) } finally { cancel.recycle() }
        }
        pan.end(); hadMultiplePointers = false; downTime = 0L
        reportGesture(false)
        parent?.requestDisallowInterceptTouchEvent(false)
        trace("reset=$reason")
    }
    fun setInteractionEnabled(value: Boolean) {
        if (isEnabled == value) return
        resetInput("enabled-change")
        isEnabled = value
        trace("interaction=$value")
    }
    fun show(bitmap: Bitmap, restored: PageViewportState,
             pageWidth: Float = bitmap.width.toFloat(), pageHeight: Float = bitmap.height.toFloat(),
             contentKey: Long = 0L) {
        if (image === bitmap) return
        require(pageWidth.isFinite() && pageWidth > 0f && pageHeight.isFinite() && pageHeight > 0f)
        val samePage = image != null && logicalKey == contentKey &&
            logicalPageWidth == pageWidth && logicalPageHeight == pageHeight
        if (samePage) {
            image = bitmap
            invalidate() // Refinement never resets pointers, geometry or reading coordinates.
            return
        }
        resetInput("new-page")
        image = bitmap
        logicalPageWidth = pageWidth; logicalPageHeight = pageHeight; logicalKey = contentKey
        pendingState = restored
        configure()
        invalidate()
    }
    private fun reportGesture(active: Boolean) {
        if (gestureReported == active) return
        gestureReported = active
        onRenderGesture?.invoke(active)
    }
    private fun reportRenderViewport() {
        if (image != null && width > 0 && height > 0) {
            onRenderViewport?.invoke(width, height, geometry.state.zoom)
        }
    }
    private fun configure() {
        if (image == null || width <= 0 || height <= 0) return
        geometry.configure(logicalPageWidth, logicalPageHeight, width.toFloat(), height.toFloat(), pendingState)
        changed()
    }
    fun fitPage() {
        resetInput("fit")
        geometry.fit()
        changed()
    }
    fun fitWidth() {
        resetInput("fit-width")
        geometry.fitWidth()
        changed()
    }

    fun activateNavigation() { resetInput("navigation") }

    fun pageNavigationState(): PageViewportState = geometry.forPageNavigation()

    private fun changed() {
        pendingState = geometry.state
        onViewportChanged?.invoke(pendingState)
        reportRenderViewport()
        invalidate()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetInput("size-change")
        configure()
    }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) resetInput("focus-lost") else reportRenderViewport()
        trace("focus=$hasWindowFocus")
    }
    override fun onDetachedFromWindow() {
        resetInput("detached")
        super.onDetachedFromWindow()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = image ?: return
        val saved = canvas.save()
        try {
            canvas.clipRect(0, 0, width, height)
            val p = geometry.placement
            target.set(p.left, p.top, p.left + p.width, p.top + p.height)
            canvas.drawBitmap(bitmap, null, target, paint)
        } finally {
            canvas.restoreToCount(saved)
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (!isEnabled || image == null || width <= 0 || height <= 0) {
            if (action == MotionEvent.ACTION_DOWN) trace("ignored-down image=${image != null}")
            resetInput("unavailable")
            return false
        }
        if (action == MotionEvent.ACTION_DOWN) {
            resetInput("fresh-down")
            reportGesture(true)
            downTime = event.downTime
            pan.begin(event.getPointerId(0), event.x, event.y)
            parent?.requestDisallowInterceptTouchEvent(true)
            trace("DOWN id=${pan.pointerId} x=${event.x} y=${event.y}")
        }
        scaleDetector.onTouchEvent(event)
        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                hadMultiplePointers = true
                val index = event.findPointerIndex(pan.pointerId)
                if (index >= 0) pan.rebase(pan.pointerId, event.getX(index), event.getY(index))
                trace("POINTER_DOWN count=${event.pointerCount}")
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(pan.pointerId)
                if (index < 0 || event.downTime != downTime) {
                    // Establish a new origin; never apply a delta from a stale pointer/stream.
                    resetInput("missing-or-stale-pointer")
                    reportGesture(true)
                    downTime = event.downTime
                    pan.begin(event.getPointerId(0), event.x, event.y)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    trace("MOVE-recovered count=${event.pointerCount}")
                    return true
                }
                val x = event.getX(index); val y = event.getY(index)
                if (event.pointerCount == 1) {
                    if (scaleDetector.isInProgress) {
                        val now = SystemClock.uptimeMillis()
                        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, x, y, 0)
                        try { scaleDetector.onTouchEvent(cancel) } finally { cancel.recycle() }
                        trace("stale-scale-cleared")
                    }
                    val delta = pan.move(pan.pointerId, x, y, slop)
                    if (delta != null) { geometry.pan(delta.first, delta.second); changed() }
                    if (event.eventTime - lastMoveLog >= 250L) {
                        trace("MOVE x=$x y=$y pan=${delta != null} top=${geometry.placement.top}")
                        lastMoveLog = event.eventTime
                    }
                } else {
                    pan.rebase(pan.pointerId, x, y)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val lifted = event.actionIndex
                var remaining = event.findPointerIndex(pan.pointerId)
                if (remaining == lifted || remaining < 0) remaining = if (lifted == 0) 1 else 0
                if (remaining in 0 until event.pointerCount && remaining != lifted) {
                    pan.rebase(event.getPointerId(remaining), event.getX(remaining), event.getY(remaining))
                } else pan.end()
                trace("POINTER_UP remaining=${pan.pointerId}")
            }
            MotionEvent.ACTION_UP -> {
                val tap = !pan.hasDragged && !hadMultiplePointers
                trace("UP tap=$tap")
                resetInput("up", cancelScale = false)
                if (tap) performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                trace("CANCEL")
                resetInput("cancel", cancelScale = false)
            }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
