// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

data class PageViewportState(
    val zoom: Float = 1f, val centerX: Float = 0.5f, val centerY: Float = 0.5f,
    val freeHorizontalPan: Boolean = false,
    val startAtTop: Boolean = false,
    val navigationHorizontalFraction: Float? = null,
)
data class PagePlacement(val left: Float, val top: Float, val width: Float, val height: Float)

/** Navigation preserves fit-relative zoom and proportional horizontal travel, and starts at page top. */
class ViewportGeometry {
    private var pageWidth = 1f
    private var pageHeight = 1f
    private var viewWidth = 1f
    private var viewHeight = 1f
    private var zoomCeiling = 8f
    var state = PageViewportState()
        private set
    val placement: PagePlacement
        get() = place(state)
    private fun fitScale() = min(viewWidth / pageWidth, viewHeight / pageHeight)
    private fun widthZoom() = (viewWidth / (pageWidth * fitScale())).coerceAtLeast(1f)

    fun configure(pageWidth: Float, pageHeight: Float, viewWidth: Float, viewHeight: Float,
                  restored: PageViewportState = state) {
        require(listOf(pageWidth, pageHeight, viewWidth, viewHeight).all { it.isFinite() && it > 0f })
        this.pageWidth = pageWidth; this.pageHeight = pageHeight
        this.viewWidth = viewWidth; this.viewHeight = viewHeight
        require(fitScale().isFinite() && fitScale() > 0f)
        var zoom = restored.zoom.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
        val scale = fitScale() * zoom
        if (!(pageWidth * scale).isFinite() || !(pageHeight * scale).isFinite()) zoom = 1f
        // A carried zoom must not be reduced just because the next page has a different aspect ratio.
        zoomCeiling = max(max(8f, widthZoom()), zoom)
        val fraction = restored.navigationHorizontalFraction?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
        val freeX = restored.freeHorizontalPan || (fraction != null && abs(fraction - 0.5f) > 0.00001f)
        val rawX = restored.centerX.takeIf { it.isFinite() } ?: 0.5f
        val x = if (fraction != null) {
            val width = pageWidth * fitScale() * zoom
            val minimum = min(0f, viewWidth - width)
            val maximum = max(0f, viewWidth - width)
            val left = minimum + fraction * (maximum - minimum)
            (viewWidth / 2f - left) / width
        } else if (freeX) rawX else rawX.coerceIn(0f, 1f)
        val y = restored.centerY.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
        apply(PageViewportState(zoom, x, y, freeX, restored.startAtTop, fraction))
    }
    private fun clampAxis(offset: Float, viewport: Float, content: Float, freeSmall: Boolean = false): Float =
        if (content <= viewport) {
            if (freeSmall) offset.coerceIn(0f, viewport - content) else (viewport - content) / 2f
        } else offset.coerceIn(viewport - content, 0f)
    private fun place(value: PageViewportState): PagePlacement {
        val scale = fitScale() * value.zoom
        val width = pageWidth * scale; val height = pageHeight * scale
        return PagePlacement(
            clampAxis(viewWidth / 2f - value.centerX * width, viewWidth, width, value.freeHorizontalPan),
            if (value.startAtTop) 0f else clampAxis(viewHeight / 2f - value.centerY * height, viewHeight, height),
            width, height)
    }
    private fun apply(value: PageViewportState) {
        val p = place(value)
        state = value.copy(centerX = (viewWidth / 2f - p.left) / p.width,
            centerY = (viewHeight / 2f - p.top) / p.height)
    }
    fun forPageNavigation(): PageViewportState {
        val p = placement
        val minimum = min(0f, viewWidth - p.width)
        val maximum = max(0f, viewWidth - p.width)
        val span = maximum - minimum
        val fraction = if (span > 0.001f) ((p.left - minimum) / span).coerceIn(0f, 1f) else 0.5f
        return state.copy(centerY = 0f, startAtTop = true, navigationHorizontalFraction = fraction)
    }
    fun fit() {
        zoomCeiling = max(8f, widthZoom())
        apply(PageViewportState())
    }
    fun fitWidth() {
        zoomCeiling = max(zoomCeiling, widthZoom())
        apply(PageViewportState(widthZoom(), 0.5f, state.centerY, startAtTop = state.startAtTop))
    }
    fun pan(dx: Float, dy: Float) {
        if (!dx.isFinite() || !dy.isFinite()) return
        val p = placement
        val keepTop = state.startAtTop && (p.height <= viewHeight || dy == 0f)
        apply(state.copy(centerX = (viewWidth / 2f - p.left - dx) / p.width,
            centerY = (viewHeight / 2f - p.top - dy) / p.height,
            freeHorizontalPan = state.freeHorizontalPan || dx != 0f,
            startAtTop = keepTop, navigationHorizontalFraction = null))
    }
    fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
        if (!factor.isFinite() || factor <= 0f || !focusX.isFinite() || !focusY.isFinite()) return
        val old = placement
        val x = (focusX - old.left) / old.width; val y = (focusY - old.top) / old.height
        val zoom = (state.zoom * factor).coerceIn(1f, zoomCeiling)
        val ratio = zoom / state.zoom
        val width = old.width * ratio; val height = old.height * ratio
        apply(state.copy(zoom = zoom, centerX = (viewWidth / 2f - focusX + x * width) / width,
            centerY = (viewHeight / 2f - focusY + y * height) / height,
            startAtTop = state.startAtTop && height <= viewHeight, navigationHorizontalFraction = null))
    }
    fun viewToPage(x: Float, y: Float): Pair<Float, Float> {
        val p = placement
        return (x - p.left) / p.width to (y - p.top) / p.height
    }
    fun pageToView(x: Float, y: Float): Pair<Float, Float> {
        val p = placement
        return p.left + x * p.width to p.top + y * p.height
    }
}
