// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class ViewportGeometryTest {
    private fun square() = ViewportGeometry().apply { configure(1000f, 1000f, 800f, 800f) }
    @Test fun fitCentersSmallerAxis() {
        val v = ViewportGeometry().apply { configure(500f, 1000f, 800f, 800f) }
        assertEquals(200f, v.placement.left, 0.001f)
        assertEquals(0f, v.placement.top, 0.001f)
        assertEquals(800f, v.placement.height, 0.001f)
    }
    @Test fun zoomPreservesUnclampedFocalPoint() {
        val v = square()
        v.zoomBy(2f, 400f, 400f)
        val before = v.viewToPage(300f, 450f)
        v.zoomBy(1.5f, 300f, 450f)
        val after = v.viewToPage(300f, 450f)
        assertEquals(before.first, after.first, 0.00001f)
        assertEquals(before.second, after.second, 0.00001f)
    }
    @Test fun panCannotLoseThePage() {
        val v = square(); v.zoomBy(2f, 400f, 400f); v.pan(100000f, -100000f)
        assertEquals(0f, v.placement.left, 0.001f)
        assertEquals(-800f, v.placement.top, 0.001f)
    }
    @Test fun coordinateRoundTripAfterPanAndZoom() {
        val v = square(); v.zoomBy(3f, 400f, 400f); v.pan(-100f, 70f)
        val view = v.pageToView(0.37f, 0.61f)
        val page = v.viewToPage(view.first, view.second)
        assertEquals(0.37f, page.first, 0.00001f)
        assertEquals(0.61f, page.second, 0.00001f)
    }
    @Test fun restoreAfterResizeKeepsValidZoomAndAnchor() {
        val v = square(); v.zoomBy(3f, 400f, 400f); v.pan(-70f, 50f)
        val saved = v.state
        v.configure(1000f, 1000f, 1200f, 700f, saved)
        assertEquals(saved.zoom, v.state.zoom, 0.00001f)
        assertEquals(saved.centerX, v.state.centerX, 0.00001f)
        assertEquals(saved.centerY, v.state.centerY, 0.00001f)
    }
    @Test fun fitResetsZoomAndPosition() {
        val v = square(); v.zoomBy(8f, 400f, 400f); v.pan(500f, -500f); v.fit()
        assertEquals(PageViewportState(), v.state)
    }
    @Test fun invalidRestoredStateAndZoomFactorsStaySafe() {
        val v = square()
        v.configure(1000f, 1000f, 800f, 800f, PageViewportState(Float.NaN, Float.POSITIVE_INFINITY, -100f))
        v.zoomBy(Float.NaN, 400f, 400f)
        v.pan(Float.NaN, 0f)
        assertEquals(PageViewportState(), v.state)
        v.zoomBy(100f, 400f, 400f)
        assertEquals(8f, v.state.zoom, 0.00001f)
    }
}
