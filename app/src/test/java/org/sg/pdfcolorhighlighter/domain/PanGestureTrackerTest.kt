// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class PanGestureTrackerTest {
    @Test fun freshDownwardDragWorks() {
        val p = PanGestureTracker(); p.begin(1, 100f, 100f)
        assertEquals(0f to 80f, p.move(1, 100f, 180f, 8f))
    }
    @Test fun freshUpwardDragWorks() {
        val p = PanGestureTracker(); p.begin(1, 100f, 200f)
        assertEquals(0f to -80f, p.move(1, 100f, 120f, 8f))
    }
    @Test fun newGestureDoesNotDependOnPreviousDirection() {
        val p = PanGestureTracker(); p.begin(1, 100f, 300f)
        p.move(1, 100f, 100f, 8f); p.end()
        p.begin(7, 100f, 200f)
        assertEquals(0f to 60f, p.move(7, 100f, 260f, 8f))
    }
    @Test fun reversalWithoutLiftingIsImmediate() {
        val p = PanGestureTracker(); p.begin(1, 0f, 100f)
        assertEquals(0f to -40f, p.move(1, 0f, 60f, 8f))
        assertEquals(0f to 10f, p.move(1, 0f, 70f, 8f))
    }
    @Test fun movementBelowSlopIsAccumulatedNotLost() {
        val p = PanGestureTracker(); p.begin(1, 0f, 0f)
        assertNull(p.move(1, 0f, 4f, 8f))
        assertEquals(0f to 10f, p.move(1, 0f, 10f, 8f))
    }
    @Test fun replacingPointerUsesItsOwnOrigin() {
        val p = PanGestureTracker(); p.begin(1, 10f, 20f)
        p.rebase(9, 400f, 500f)
        assertEquals(5f to -5f, p.move(9, 405f, 495f, 8f))
    }
    @Test fun endedOrUnknownPointerCannotMovePage() {
        val p = PanGestureTracker(); p.begin(1, 0f, 0f)
        assertNull(p.move(2, 30f, 30f, 8f)); p.end()
        assertNull(p.move(1, 30f, 30f, 8f))
        p.begin(1, 100f, 100f)
        assertEquals(0f to -20f, p.move(1, 100f, 80f, 8f))
    }
}
