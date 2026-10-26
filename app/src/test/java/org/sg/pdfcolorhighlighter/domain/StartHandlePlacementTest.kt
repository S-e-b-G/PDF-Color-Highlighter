// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class StartHandlePlacementTest{
    private fun span(y:Float):SelectionSpan{val g=TextGlyph(65,GlyphQuad(TextPoint(20f,y),TextPoint(30f,y),TextPoint(20f,y+10f),TextPoint(30f,y+10f)));return SelectionSpan(TextAddress(0,0),TextAddress(0,0),"A","A",listOf(g))}
    private val map=PageCoordinateMap(TextBounds(0f,0f,100f,100f),0f,0f,400f,400f)
    @Test fun startCircleIsEntirelyAboveTextWhenRoomExists(){val h=SelectionHandleGeometry.layout(span(20f),map,2f,400f,400f)!!;assertTrue(h.start.visible);assertTrue(h.start.center.y+h.radius<h.start.anchor.y)}
    @Test fun noRoomAboveHidesStartRatherThanCoverText(){val h=SelectionHandleGeometry.layout(span(0f),map,2f,400f,400f)!!;assertFalse(h.start.visible);assertTrue(h.end.visible)}
    @Test fun endCircleStillStaysBelowRange(){val h=SelectionHandleGeometry.layout(span(20f),map,2f,400f,400f)!!;assertTrue(h.end.center.y>h.end.anchor.y)}
    @Test fun nativeGrabCenterIsIndependentOfMarkerSide(){val h=SelectionHandleGeometry.layout(span(20f),map,1f,400f,400f)!!;assertEquals(TextPoint(25f,25f),h.start.nativeCenter);assertEquals(h.start.nativeCenter,h.end.nativeCenter)}
}
