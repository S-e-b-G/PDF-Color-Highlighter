// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class RenderResolutionTest {
    private fun plan(w: Float = 595f, h: Float = 842f, vw: Int = 1200, vh: Int = 900, z: Float = 1f) =
        RenderResolutionPolicy.plan(w, h, vw, vh, z)
    private fun bounded(p: RenderResolution) {
        assertTrue(p.width in 1..4094 && p.height in 1..4094)
        assertTrue(p.reservedPixels <= RenderResolutionPolicy.MAX_PIXELS)
    }
    private fun invalid(block: () -> Unit) {
        try { block(); fail("Invalid input accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun portraitIsBounded() { bounded(plan()) }
    @Test fun landscapeIsBounded() { bounded(plan(842f, 595f)) }
    @Test fun squareFitsPixelBudget() { bounded(plan(100f, 100f, z = 100f)) }
    @Test fun tallPageHasPositiveWidth() { bounded(plan(10f, 1000f, z = 100f)) }
    @Test fun widePageHasPositiveHeight() { bounded(plan(1000f, 10f, z = 100f)) }
    @Test fun extremeZoomIsBounded() { bounded(plan(z = Float.MAX_VALUE)) }
    @Test fun resolutionDoesNotDecreaseWithZoom() {
        val values = listOf(1f, 2f, 3f, 4f, 8f, 20f).map { max(plan(z = it).width, plan(z = it).height) }
        assertEquals(values.sorted(), values)
    }
    @Test fun pixelSizeDoesNotDependOnPointUnits() { assertEquals(plan(), plan(1190f, 1684f)) }
    @Test fun aspectRatioIsPreservedToRounding() {
        val p = plan(z = 3f)
        assertTrue(abs(p.width.toDouble() / p.height - 595.0 / 842) < 0.002)
    }
    @Test fun sameLongEdgeDoesNotUpgrade() {
        assertFalse(RenderResolutionPolicy.needsUpgrade(RenderResolution(1000, 1600), 1000, 1600))
    }
    @Test fun insignificantDifferenceDoesNotUpgrade() {
        assertFalse(RenderResolutionPolicy.needsUpgrade(RenderResolution(1000, 1650), 1000, 1600))
    }
    @Test fun largerTierUpgrades() {
        assertTrue(RenderResolutionPolicy.needsUpgrade(RenderResolution(1400, 2048), 1100, 1600))
    }
    @Test fun invalidPageSizesAreRejected() {
        invalid { plan(w = 0f) }; invalid { plan(h = -1f) }; invalid { plan(w = Float.NaN) }
    }
    @Test fun invalidViewportIsRejected() { invalid { plan(vw = 0) }; invalid { plan(vh = -10) } }
    @Test fun invalidZoomIsRejected() {
        invalid { plan(z = 0.5f) }; invalid { plan(z = Float.NaN) }; invalid { plan(z = Float.POSITIVE_INFINITY) }
    }
    @Test fun unsupportedRatioIsRejected() { invalid { plan(w = 1f, h = 1000f) } }
}
