// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class TextGeometryPolicyTest {
    private fun q(x: Float,y: Float,w: Float=10f,h: Float=10f)=GlyphQuad(
        TextPoint(x,y),TextPoint(x+w,y),TextPoint(x,y+h),TextPoint(x+w,y+h))
    @Test fun pointInsideRealQuadHits() { assertTrue(q(0f,0f).contains(TextPoint(5f,5f))) }
    @Test fun boundingBoxAloneIsNotAHit() {
        val diamond=GlyphQuad(TextPoint(0f,10f),TextPoint(10f,0f),TextPoint(10f,20f),TextPoint(20f,10f))
        assertFalse(diamond.contains(TextPoint(1f,1f)));assertTrue(diamond.contains(TextPoint(10f,10f)))
    }
    @Test fun zeroAreaCannotHit() { assertFalse(q(0f,0f,0f).contains(TextPoint(0f,5f))) }
    @Test fun nonFiniteGeometryAndSurrogateCodePointAreRejected() {
        try { TextPoint(Float.NaN,0f);fail("NaN accepted") } catch (_:IllegalArgumentException) { }
        try { TextGlyph(0xD800,q(0f,0f));fail("Surrogate accepted") } catch (_:IllegalArgumentException) { }
    }
    @Test fun largeInlineGapSplitsColumns() {
        val a=TextGlyph('A'.code,q(0f,0f));val b=TextGlyph('B'.code,q(300f,0f))
        assertEquals(listOf(listOf(a),listOf(b)),TextGeometryPolicy.splitRuns(listOf(a,b)))
    }
    @Test fun wideWhitespaceSeparatesRuns() {
        val a=TextGlyph('A'.code,q(0f,0f));val space=TextGlyph(32,q(10f,0f,200f));val b=TextGlyph('B'.code,q(210f,0f))
        assertEquals(listOf(listOf(a),listOf(b)),TextGeometryPolicy.splitRuns(listOf(a,space,b)))
    }
    @Test fun ordinarySpacesKeepSameFlow() {
        val glyphs=listOf(TextGlyph('A'.code,q(0f,0f)),TextGlyph(32,q(10f,0f)),TextGlyph('B'.code,q(20f,0f)))
        assertEquals(listOf(glyphs),TextGeometryPolicy.splitRuns(glyphs))
        val a=PageTextLine(0,0,glyphs);val b=PageTextLine(0,0,glyphs.map { TextGlyph(it.codePoint,q(it.quad.bounds.left,15f)) })
        assertTrue(TextGeometryPolicy.sameFlow(a,b))
    }
}
