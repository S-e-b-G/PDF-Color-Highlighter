// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class SmallPagePanTest {
    private fun page() = ViewportGeometry().apply { configure(200f, 1000f, 1000f, 600f) }
    @Test fun smallPageStartsAndZoomsCenteredUntilDragged() {
        val v = page()
        assertEquals(440f, v.placement.left, 0.001f)
        v.zoomBy(1.5f, 100f, 200f)
        assertEquals(410f, v.placement.left, 0.001f)
        assertFalse(v.state.freeHorizontalPan)
    }
    @Test fun smallPageCanBeDraggedInBothHorizontalDirections() {
        val v = page(); v.pan(120f, 0f)
        assertEquals(560f, v.placement.left, 0.001f)
        v.pan(-200f, 0f)
        assertEquals(360f, v.placement.left, 0.001f)
    }
    @Test fun smallPageStaysEntirelyInsideTheViewport() {
        val v = page(); v.pan(100000f, 0f)
        assertEquals(880f, v.placement.left, 0.001f)
        v.pan(-200000f, 0f)
        assertEquals(0f, v.placement.left, 0.001f)
    }
    @Test fun anOffPageCenterAnchorSurvivesRestoration() {
        val v = page(); v.pan(10000f, 0f)
        val saved = v.state
        assertTrue(saved.freeHorizontalPan)
        assertTrue(saved.centerX < 0f)
        val restored = ViewportGeometry().apply { configure(200f, 1000f, 1000f, 600f, saved) }
        assertEquals(v.placement.left, restored.placement.left, 0.001f)
    }
    @Test fun fitPageRecentersAndResetsFreePanningState() {
        val v = page(); v.pan(200f, 0f); v.fit()
        assertEquals(440f, v.placement.left, 0.001f)
        assertFalse(v.state.freeHorizontalPan)
    }
    @Test fun fitWidthAlsoRecentersHorizontally() {
        val v = page(); v.pan(-200f, 0f); v.fitWidth()
        assertEquals(0f, v.placement.left, 0.001f)
        assertEquals(1000f, v.placement.width, 0.001f)
        assertFalse(v.state.freeHorizontalPan)
    }
}
