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
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.domain.PanGestureTracker
import org.sg.pdfcolorhighlighter.domain.ViewportGeometry

/** M2a.1: explicit fresh-gesture reset, independent one-finger pan and debug-only event tracing. */
class PdfViewportView(context: Context) : View(context) {
    private var image: Bitmap? = null
    private var logicalPageWidth = 1f
    private var logicalPageHeight = 1f
    private var logicalKey = 0L
    private var previewText: PageTextSnapshot?=null
    private var previewSlot: Int?=null
    private var previewSpan: SelectionSpan?=null
    private var previewBands: List<PreviewPaintBand> = emptyList()
    private var selectionAnchor: TextPoint?=null
    private var selectionActive=false
    private var selectionPinchCancelled=false
    private val selectionPaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val previewPalette=intArrayOf(0xFFFFAD5B.toInt(),0xFF00FF00.toInt(),0xFFFC3636.toInt(),
        0xFFFFFF00.toInt(),0xFFD1DFEB.toInt(),0xFFFCD7FB.toInt())
    private var handlesEnabled=false
    private var activeHandle:SelectionHandleSide?=null
    private var handleBaseSpan:SelectionSpan?=null
    private var handleLayout:SelectionHandles?=null
    private var handleFixed:TextAddress?=null
    private var handleDownTouch:TextPoint?=null
    private var handlePointOffset:TextPoint?=null
    private var handleDelta=TextPoint(0f,0f)
    private var handleMoved=false
    private val handlePaint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val handleDensity get()=resources.displayMetrics.density
    var onPreviewAdjust: ((TextPoint,TextAddress,Boolean)->Unit)?=null
    var onPreviewDrag: ((TextPoint,TextPoint,Boolean)->Unit)?=null
    var onPreviewCancel: (()->Unit)?=null
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
        cancelSelectionGesture()
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

    fun setSelectionPreview(snapshot: PageTextSnapshot?, slot: Int?, span: SelectionSpan?, handles: Boolean = false) {
        require(slot==null || slot in 0..5)
        if (snapshot!==previewText || (slot==null)!=(previewSlot==null)) resetInput("preview-mode")
        if (span!==previewSpan) previewBands=span?.let{PreviewPaintGeometry.bands(it)}.orEmpty()
        previewText=snapshot;previewSlot=slot;previewSpan=span;handlesEnabled=handles
        invalidate()
    }
    private fun clearHandleState(){
        activeHandle=null;handleBaseSpan=null;handleLayout=null;handleFixed=null
        handleDownTouch=null;handlePointOffset=null;handleDelta=TextPoint(0f,0f);handleMoved=false
    }
    private fun cancelSelectionGesture() {
        val notify=selectionActive || activeHandle!=null
        selectionActive=false;selectionAnchor=null;clearHandleState()
        if(notify)onPreviewCancel?.invoke()
    }
    private fun currentSelectionHandles():SelectionHandles?{
        val span=handleBaseSpan ?: previewSpan ?: return null
        val mapping=coordinateMap() ?: return null
        return try{SelectionHandleGeometry.layout(span,mapping,handleDensity,width.toFloat(),height.toFloat())}
            catch(_:IllegalArgumentException){null}
    }
    private fun beginPreviewGesture(x:Float,y:Float){
        val span=previewSpan
        val layout=if(handlesEnabled)currentSelectionHandles() else null
        val side=if(layout!=null && x.isFinite() && y.isFinite())SelectionHandleGeometry.hit(layout,TextPoint(x,y),handleDensity)else null
        if(side!=null && span!=null && layout!=null){
            val handle=if(side==SelectionHandleSide.START)layout.start else layout.end
            val mapping=coordinateMap() ?: return
            val nativePoint=try{mapping.toView(handle.nativeCenter)}catch(_:IllegalArgumentException){return}
            activeHandle=side;handleBaseSpan=span;handleLayout=layout
            handleFixed=if(side==SelectionHandleSide.START)span.end else span.start
            handleDownTouch=TextPoint(x,y);handlePointOffset=TextPoint(x-nativePoint.x,y-nativePoint.y)
            handleDelta=TextPoint(0f,0f);handleMoved=false
        }else{
            onPreviewCancel?.invoke()
            selectionAnchor=nativeAt(x,y);selectionActive=selectionAnchor!=null
            selectionAnchor?.let{onPreviewDrag?.invoke(it,it,false)}
        }
    }
    private fun updateHandle(x:Float,y:Float,finished:Boolean){
        if(activeHandle==null || selectionPinchCancelled || !x.isFinite() || !y.isFinite())return
        val down=handleDownTouch ?: return;val offset=handlePointOffset ?: return;val fixed=handleFixed ?: return
        val dx=x-down.x;val dy=y-down.y
        if(!handleMoved && dx*dx+dy*dy<=slop*slop)return
        handleMoved=true;handleDelta=TextPoint(dx,dy)
        nativeAt(x-offset.x,y-offset.y)?.let{onPreviewAdjust?.invoke(it,fixed,finished)}
        invalidate()
    }
    private fun finishHandle(x:Float,y:Float){
        if(activeHandle==null)return
        updateHandle(x,y,true)
        clearHandleState()
    }
    private fun drawSelectionHandles(canvas:Canvas){
        if(previewSlot==null || (!handlesEnabled && activeHandle==null))return
        val layout=handleLayout ?: currentSelectionHandles() ?: return
        val density=handleDensity
        for(h in listOf(layout.start,layout.end)){
            if(!h.visible)continue
            val delta=if(activeHandle==h.side && handleMoved)handleDelta else TextPoint(0f,0f)
            val ax=h.anchor.x+delta.x;val ay=h.anchor.y+delta.y
            val cx=h.center.x+delta.x;val cy=h.center.y+delta.y
            handlePaint.alpha=255;handlePaint.style=Paint.Style.STROKE;handlePaint.strokeWidth=2f*density
            handlePaint.color=Color.DKGRAY;canvas.drawLine(ax,ay,cx,cy,handlePaint)
            handlePaint.style=Paint.Style.FILL;handlePaint.color=previewPalette[checkNotNull(previewSlot)]
            canvas.drawCircle(cx,cy,layout.radius,handlePaint)
            handlePaint.style=Paint.Style.STROKE;handlePaint.color=Color.DKGRAY
            canvas.drawCircle(cx,cy,layout.radius,handlePaint)
        }
    }
    private fun coordinateMap(): PageCoordinateMap? {
        val snapshot=previewText ?: return null
        val p=geometry.placement;val b=snapshot.pageBounds
        if (p.width<=0f || p.height<=0f || b.width<=0f || b.height<=0f) return null
        return PageCoordinateMap(b,p.left,p.top,p.width,p.height)
    }
    private fun nativeAt(x: Float,y: Float): TextPoint? {
        if (!x.isFinite() || !y.isFinite()) return null
        return coordinateMap()?.toPage(TextPoint(x,y))
    }
    private fun drawSelectionPreview(canvas: Canvas) {
        val slot=previewSlot ?: return
        if(previewSpan==null) return
        val mapping=coordinateMap() ?: return
        selectionPaint.color=previewPalette[slot];selectionPaint.alpha=85
        for (band in previewBands) {
            val a=mapping.toView(TextPoint(band.left,band.top))
            val b=mapping.toView(TextPoint(band.right,band.bottom))
            canvas.drawRect(a.x,a.y,b.x,b.y,selectionPaint)
        }
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
            drawSelectionPreview(canvas)
            drawSelectionHandles(canvas)
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
            selectionPinchCancelled=false
            if (previewSlot!=null && previewText!=null) beginPreviewGesture(event.x,event.y)
            downTime = event.downTime
            pan.begin(event.getPointerId(0), event.x, event.y)
            parent?.requestDisallowInterceptTouchEvent(true)
            trace("DOWN id=${pan.pointerId} x=${event.x} y=${event.y}")
        }
        scaleDetector.onTouchEvent(event)
        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                hadMultiplePointers = true
                cancelSelectionGesture()
                selectionPinchCancelled=true
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
                    if (previewSlot!=null) {
                        if(activeHandle!=null)updateHandle(x,y,false)
                        val first=selectionAnchor
                        if (selectionActive && !selectionPinchCancelled && first!=null) {
                            nativeAt(x,y)?.let { onPreviewDrag?.invoke(first,it,false) }
                        }
                        pan.rebase(pan.pointerId,x,y)
                    } else {
                        val delta = pan.move(pan.pointerId, x, y, slop)
                        if (delta != null) { geometry.pan(delta.first, delta.second); changed() }
                    }
                    if (event.eventTime - lastMoveLog >= 250L) {
                        trace("MOVE x=$x y=$y preview=${previewSlot != null} top=${geometry.placement.top}")
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
                finishHandle(event.x,event.y)
                val first=selectionAnchor
                if (selectionActive && !selectionPinchCancelled && first!=null) {
                    nativeAt(event.x,event.y)?.let { onPreviewDrag?.invoke(first,it,true) }
                    selectionActive=false;selectionAnchor=null
                }
                val tap = !pan.hasDragged && !hadMultiplePointers && previewSlot==null
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
