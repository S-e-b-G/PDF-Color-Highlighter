// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class ViewportFitWidthTest {
    @Test fun widthFitsAndIsHorizontallyCentered() {
        val v = ViewportGeometry().apply { configure(600f, 900f, 1200f, 600f) }
        v.zoomBy(4f, 500f, 300f); v.pan(-200f, 0f); v.fitWidth()
        assertEquals(1200f, v.placement.width, 0.001f)
        assertEquals(0f, v.placement.left, 0.001f)
    }
    @Test fun widthAlreadyLimitsThePage() {
        val v = ViewportGeometry().apply { configure(1000f, 500f, 800f, 1000f) }
        v.zoomBy(4f, 400f, 500f); v.fitWidth()
        assertEquals(1f, v.state.zoom, 0.00001f)
        assertEquals(800f, v.placement.width, 0.001f)
    }
    @Test fun widthFitPreservesVerticalReadingAnchorWhenNotClamped() {
        val v = ViewportGeometry().apply {
            configure(600f, 1000f, 800f, 700f, PageViewportState(3f, 0.5f, 0.65f))
        }
        v.fitWidth()
        assertEquals(0.65f, v.state.centerY, 0.00001f)
        assertEquals(800f, v.placement.width, 0.001f)
    }
    @Test fun tallPagesCanFitWidthAboveOrdinaryPinchLimit() {
        val v = ViewportGeometry().apply { configure(100f, 10000f, 1000f, 500f) }
        v.fitWidth()
        assertEquals(200f, v.state.zoom, 0.001f)
        assertEquals(1000f, v.placement.width, 0.001f)
        v.zoomBy(1.01f, 500f, 250f)
        assertEquals(1000f, v.placement.width, 0.001f)
    }
    @Test fun fitPageStillResetsWidthFitAndPan() {
        val v = ViewportGeometry().apply { configure(600f, 1000f, 800f, 700f) }
        v.fitWidth(); v.pan(0f, -100f); v.fit()
        assertEquals(PageViewportState(), v.state)
        assertTrue(v.placement.width <= 800.001f)
        assertTrue(v.placement.height <= 700.001f)
    }
}
