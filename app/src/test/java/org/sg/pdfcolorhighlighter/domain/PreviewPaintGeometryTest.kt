// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class PreviewPaintGeometryTest{
    private fun g(x:Float,y:Float=0f,w:Float=5f,h:Float=10f)=TextGlyph(65,GlyphQuad(TextPoint(x,y),TextPoint(x+w,y),TextPoint(x,y+h),TextPoint(x+w,y+h)))
    private fun span(vararg glyphs:TextGlyph)=SelectionSpan(TextAddress(0,0),TextAddress(0,glyphs.size-1),"synthetic","synthetic",glyphs.toList())
    @Test fun fillsOrdinaryWordGap(){assertEquals(listOf(PreviewPaintBand(0f,0f,14f,10f)),PreviewPaintGeometry.bands(span(g(0f),g(9f))))}
    @Test fun measuredColumnGutterIsNotFilled(){val bands=PreviewPaintGeometry.bands(span(g(40.48f,w=243.01f,h=7.33f),g(310.45f,w=242.95f,h=7.33f)));assertEquals(2,bands.size)}
    @Test fun separateRowsRemainSeparate(){assertEquals(2,PreviewPaintGeometry.bands(span(g(0f),g(0f,y=15f))).size)}
    @Test fun partialSelectionDoesNotFillRestOfLine(){assertEquals(PreviewPaintBand(20f,0f,25f,10f),PreviewPaintGeometry.bands(span(g(20f))).single())}
    @Test fun nativeSelectionQuadsRemainUnchanged(){val s=span(g(0f),g(9f));val before=s.quads;PreviewPaintGeometry.bands(s);assertEquals(before,s.quads);assertEquals(2,s.quads.size)}
    @Test fun overlappingCharactersUseOneBand(){assertEquals(listOf(PreviewPaintBand(0f,0f,8f,10f)),PreviewPaintGeometry.bands(span(g(0f),g(3f))))}
    @Test fun paragraphGapCannotBecomeOneRectangle(){assertEquals(2,PreviewPaintGeometry.bands(span(g(0f),g(5f,y=40f))).size)}
    @Test fun degenerateQuadsDoNotPaint(){assertTrue(PreviewPaintGeometry.bands(span(g(0f,w=0f))).isEmpty())}
}
