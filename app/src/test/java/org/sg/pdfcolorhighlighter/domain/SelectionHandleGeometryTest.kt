// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class SelectionHandleGeometryTest{
    private fun glyph(x:Float,y:Float=20f,w:Float=10f,h:Float=10f)=TextGlyph(65,GlyphQuad(TextPoint(x,y),TextPoint(x+w,y),TextPoint(x,y+h),TextPoint(x+w,y+h)))
    private fun span(vararg g:TextGlyph)=SelectionSpan(TextAddress(0,0),TextAddress(0,g.size-1),"synthetic","synthetic",g.toList())
    private fun layout(s:SelectionSpan,d:Float=1f,w:Float=400f,h:Float=400f,map:PageCoordinateMap=PageCoordinateMap(TextBounds(0f,0f,100f,100f),0f,0f,400f,400f))=SelectionHandleGeometry.layout(s,map,d,w,h)
    @Test fun endpointsUseFirstAndLastGlyph(){val r=layout(span(glyph(20f),glyph(50f)))!!;assertEquals(TextPoint(25f,25f),r.start.nativeCenter);assertEquals(TextPoint(55f,25f),r.end.nativeCenter)}
    @Test fun circlesAreBelowEndpoints(){val r=layout(span(glyph(20f),glyph(50f)))!!;assertTrue(r.start.center.y>r.start.anchor.y);assertTrue(r.end.center.y>r.end.anchor.y)}
    @Test fun startCircleHasPriorityNearItsCenter(){val r=layout(span(glyph(20f),glyph(50f)))!!;assertEquals(SelectionHandleSide.START,SelectionHandleGeometry.hit(r,r.start.center,1f))}
    @Test fun endCircleCanBeGrabbedSeparately(){val r=layout(span(glyph(20f),glyph(50f)))!!;assertEquals(SelectionHandleSide.END,SelectionHandleGeometry.hit(r,r.end.center,1f))}
    @Test fun distantTouchDoesNotGrabHandle(){val r=layout(span(glyph(20f),glyph(50f)))!!;assertNull(SelectionHandleGeometry.hit(r,TextPoint(390f,390f),1f))}
    @Test fun touchRadiusScalesWithDensity(){val r=layout(span(glyph(20f),glyph(70f)),2f)!!;assertEquals(SelectionHandleSide.START,SelectionHandleGeometry.hit(r,TextPoint(r.start.center.x,r.start.center.y+40f),2f))}
    @Test fun tinyViewportAndInvalidDensityAreRejected(){val s=span(glyph(20f));assertNull(layout(s,w=5f));assertNull(layout(s,d=0f));assertNull(layout(s,d=Float.NaN))}
    @Test fun visibleCircleClampsInsideViewport(){val r=layout(span(glyph(0f),glyph(90f,y=90f)))!!;assertTrue(r.start.center.x>=r.radius);assertTrue(r.end.center.y<=400f-r.radius)}
    @Test fun nativePageOriginIsApplied(){val m=PageCoordinateMap(TextBounds(10f,20f,110f,120f),0f,0f,400f,400f);val r=layout(span(glyph(20f,y=30f)),map=m)!!;assertEquals(TextPoint(40f,80f),r.start.anchor)}
    @Test fun noGeometryMeansNoHandles(){assertNull(layout(span(glyph(20f,w=0f,h=0f))))}
}
