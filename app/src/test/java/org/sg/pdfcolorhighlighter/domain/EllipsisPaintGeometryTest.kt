// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class EllipsisPaintGeometryTest{
    private fun g(cp:Int,x:Float,y:Float,w:Float,h:Float)=TextGlyph(cp,GlyphQuad(TextPoint(x,y),TextPoint(x+w,y),TextPoint(x,y+h),TextPoint(x+w,y+h)))
    private fun span(vararg g:TextGlyph)=SelectionSpan(TextAddress(0,0),TextAddress(0,g.size-1),"synthetic","synthetic",g.toList())
    @Test fun internalLowEllipsisBorrowsRowHeight(){val r=PreviewPaintGeometry.bands(span(g(65,0f,0f,10f,10f),g(0x2026,12f,8f,6f,2f),g(66,20f,0f,10f,10f)));assertEquals(listOf(PreviewPaintBand(0f,0f,30f,10f)),r)}
    @Test fun threeAsciiDotsPaintAtRowHeight(){val r=PreviewPaintGeometry.bands(span(g(65,0f,0f,10f,10f),g(46,11f,8f,2f,2f),g(46,14f,8f,2f,2f),g(46,17f,8f,2f,2f)));assertEquals(PreviewPaintBand(0f,0f,19f,10f),r.single())}
    @Test fun leadingEllipsisCanUseSelectedFollowingGlyph(){val r=PreviewPaintGeometry.bands(span(g(0x2026,0f,8f,6f,2f),g(65,8f,0f,10f,10f)));assertEquals(PreviewPaintBand(0f,0f,18f,10f),r.single())}
    @Test fun zeroAreaDotDoesNotAcquireNativeGeometry(){val dot=g(46,12f,10f,3f,0f);val r=PreviewPaintGeometry.bands(span(g(65,0f,0f,10f,10f),dot));assertEquals(PreviewPaintBand(0f,0f,15f,10f),r.single());assertEquals(0.0,dot.quad.area,0.0)}
    @Test fun isolatedPunctuationUsesItsActualBounds(){assertEquals(PreviewPaintBand(0f,8f,6f,10f),PreviewPaintGeometry.bands(span(g(0x2026,0f,8f,6f,2f))).single())}
    @Test fun punctuationCannotBorrowAcrossColumnGutter(){assertEquals(2,PreviewPaintGeometry.bands(span(g(0x2026,0f,8f,6f,2f),g(65,100f,0f,10f,10f))).size)}
}
