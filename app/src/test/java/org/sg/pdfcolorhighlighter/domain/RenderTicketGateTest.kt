// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class RenderTicketGateTest {
    @Test fun initialTicketIsCurrent() { val g = RenderTicketGate(); assertTrue(g.accepts(g.ticket())) }
    @Test fun pageChangeRejectsPreviousTicket() {
        val g = RenderTicketGate(); val t = g.ticket(); g.invalidate(); assertFalse(g.accepts(t))
    }
    @Test fun documentChangeRejectsEvenSamePageNumber() {
        val g = RenderTicketGate(); val a = g.ticket(); g.invalidate(); val b = g.ticket()
        assertFalse(g.accepts(a)); assertTrue(g.accepts(b))
    }
    @Test fun latestTicketSurvivesRepeatedReads() {
        val g = RenderTicketGate(); g.invalidate(); assertEquals(g.ticket(), g.ticket())
    }
    @Test fun rotationAndGestureInvalidationsRejectOlderRequests() {
        val g = RenderTicketGate(); val first = g.ticket(); g.invalidate(); val second = g.ticket()
        g.invalidate(); assertFalse(g.accepts(first)); assertFalse(g.accepts(second))
    }
    @Test fun manyRapidInvalidationsOnlyAcceptLatest() {
        val g = RenderTicketGate(); val old = mutableListOf<Long>()
        repeat(1000) { old.add(g.ticket()); g.invalidate() }
        assertTrue(old.none { g.accepts(it) }); assertTrue(g.accepts(g.ticket()))
    }
}
