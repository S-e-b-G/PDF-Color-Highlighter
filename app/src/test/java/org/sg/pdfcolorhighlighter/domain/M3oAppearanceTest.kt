// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class M3oAppearanceTest {
    private fun g(x:Float,y:Float=10f,angle:Float=.01f,w:Float=8f)=TextGlyph(65,
        GlyphQuad(TextPoint(x,y+angle*x),TextPoint(x+w,y+angle*(x+w)),TextPoint(x,y+10+angle*x),TextPoint(x+w,y+10+angle*(x+w))))
    private fun span(gs:List<TextGlyph>)=SelectionSpan(TextAddress(0,0),TextAddress(0,gs.lastIndex),"kept","kept",gs)
    private fun bands(gs:List<TextGlyph>)=NativeHighlightAppearance.forSpan(span(gs))
    @Test fun slightSkewMerges(){assertEquals(1,bands(listOf(g(0f),g(12f),g(24f))).size)}
    @Test fun negativeSkewMerges(){assertEquals(1,bands(listOf(g(0f,angle=-.01f),g(15f,angle=-.01f))).size)}
    @Test fun skewedWordGapFilled(){assertEquals(1,bands(listOf(g(0f),g(30f))).size)}
    @Test fun skewedColumnGapNotFilled(){assertEquals(2,bands(listOf(g(0f),g(100f))).size)}
    @Test fun skewedRowsSeparate(){assertEquals(2,bands(listOf(g(0f),g(0f,y=40f))).size)}
    @Test fun trueRotationPreserved(){val gs=listOf(g(0f,angle=.25f),g(20f,angle=.25f));assertEquals(gs.map{it.quad},bands(gs))}
    @Test fun exactGeometryKept(){val gs=listOf(g(0f),g(20f));val s=span(gs);NativeHighlightAppearance.forSpan(s);assertEquals(gs.map{it.quad},s.quads)}
    @Test fun textKept(){val s=span(listOf(g(0f),g(20f)));NativeHighlightAppearance.forSpan(s);assertEquals("kept",s.quotation)}
    @Test fun bandTiltKept(){val q=bands(listOf(g(0f),g(20f))).single();assertTrue(q.ur.y>q.ul.y)}
    @Test fun rawQuadPathMerges(){assertEquals(1,NativeHighlightAppearance.forQuads(listOf(g(0f).quad,g(20f).quad)).size)}
}
