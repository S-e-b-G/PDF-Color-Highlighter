// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class PageNavigationViewportTest {
    private fun fraction(v: ViewportGeometry, viewWidth: Float): Float {
        val p = v.placement
        val low = minOf(0f, viewWidth - p.width); val high = maxOf(0f, viewWidth - p.width)
        return if (high - low > 0.001f) (p.left - low) / (high - low) else 0.5f
    }
    @Test fun sameSizedNextPageKeepsZoomAndHorizontalOffsetButStartsAtTop() {
        val old = ViewportGeometry().apply { configure(600f, 900f, 800f, 600f) }
        old.zoomBy(3f, 400f, 300f); old.pan(-100f, -600f)
        assertTrue(old.placement.top < 0f)
        val next = ViewportGeometry().apply { configure(600f, 900f, 800f, 600f, old.forPageNavigation()) }
        assertEquals(old.state.zoom, next.state.zoom, 0.00001f)
        assertEquals(old.placement.left, next.placement.left, 0.001f)
        assertEquals(0f, next.placement.top, 0.001f)
    }
    @Test fun differentPageWidthsKeepProportionalHorizontalTravel() {
        val old = ViewportGeometry().apply { configure(200f, 1000f, 1000f, 600f) }
        old.zoomBy(2f, 500f, 300f); old.pan(100f, -500f)
        val next = ViewportGeometry().apply { configure(400f, 1000f, 1000f, 600f, old.forPageNavigation()) }
        assertEquals(2f, next.state.zoom, 0.00001f)
        assertEquals(fraction(old, 1000f), fraction(next, 1000f), 0.00001f)
        assertEquals(0f, next.placement.top, 0.001f)
    }
    @Test fun rightPositionedSmallPageStaysRightPositioned() {
        val old = ViewportGeometry().apply { configure(200f, 1000f, 1000f, 600f) }
        old.zoomBy(2f, 500f, 300f); old.pan(10000f, -200f)
        val next = ViewportGeometry().apply { configure(300f, 1000f, 1000f, 600f, old.forPageNavigation()) }
        assertEquals(1000f, next.placement.left + next.placement.width, 0.001f)
        assertEquals(0f, next.placement.top, 0.001f)
    }
    @Test fun pageThatFitsVerticallyIsStillShownAtTopAfterNavigation() {
        val old = ViewportGeometry().apply { configure(1000f, 400f, 800f, 1000f) }
        val next = ViewportGeometry().apply { configure(1000f, 400f, 800f, 1000f, old.forPageNavigation()) }
        assertEquals(0f, next.placement.top, 0.001f)
    }
    @Test fun fitPageRestoresTheNormalCenteredDefault() {
        val v = ViewportGeometry().apply {
            configure(1000f, 400f, 800f, 1000f, PageViewportState(startAtTop = true))
        }
        v.fit()
        assertEquals(340f, v.placement.top, 0.001f)
        assertEquals(PageViewportState(), v.state)
    }
    @Test fun zoomAboveTheNextPagesOrdinaryCeilingIsNotReduced() {
        val old = ViewportGeometry().apply { configure(100f, 10000f, 1000f, 500f); fitWidth() }
        val next = ViewportGeometry().apply { configure(1000f, 1000f, 1000f, 500f, old.forPageNavigation()) }
        assertEquals(200f, next.state.zoom, 0.001f)
        assertEquals(0f, next.placement.top, 0.001f)
    }
    @Test fun navigationSeedSurvivesLayoutResizeUntilActualInteraction() {
        val old = ViewportGeometry().apply { configure(200f, 1000f, 1000f, 600f); pan(120f, 0f) }
        val next = ViewportGeometry().apply { configure(400f, 1000f, 1000f, 595f, old.forPageNavigation()) }
        next.configure(400f, 1000f, 1000f, 600f, next.state)
        assertEquals(fraction(old, 1000f), fraction(next, 1000f), 0.00001f)
        next.pan(-20f, 0f)
        assertNull(next.state.navigationHorizontalFraction)
    }
    @Test fun verticalPanAndPinchStillWorkFromNavigationTop() {
        val v = ViewportGeometry().apply { configure(600f, 1000f, 800f, 600f, PageViewportState(3f, startAtTop = true)) }
        v.pan(0f, -100f)
        assertEquals(-100f, v.placement.top, 0.001f)
        val before = v.viewToPage(400f, 200f)
        v.zoomBy(1.2f, 400f, 200f)
        val after = v.viewToPage(400f, 200f)
        assertEquals(before.second, after.second, 0.00001f)
    }
    @Test fun horizontalDraggingDoesNotRecenterAShortNavigatedPageVertically() {
        val v = ViewportGeometry().apply { configure(1200f, 400f, 800f, 1000f, PageViewportState(2f, startAtTop = true)) }
        v.pan(80f, 10f)
        assertEquals(0f, v.placement.top, 0.001f)
        assertTrue(v.state.startAtTop)
    }
    @Test fun bothDirectionsUseTheSameTopStartingState() {
        val original = ViewportGeometry().apply { configure(600f, 900f, 800f, 600f); zoomBy(3f,400f,300f); pan(70f,-500f) }
        val next = ViewportGeometry().apply { configure(600f,900f,800f,600f,original.forPageNavigation()) }
        next.pan(0f,-300f)
        val previous = ViewportGeometry().apply { configure(600f,900f,800f,600f,next.forPageNavigation()) }
        assertEquals(3f, previous.state.zoom, 0.00001f)
        assertEquals(0f, previous.placement.top, 0.001f)
        assertEquals(fraction(next,800f), fraction(previous,800f), 0.00001f)
    }
}
