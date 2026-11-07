// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class NativeHighlightAppearanceTest {
    private fun q(x:Float,y:Float=10f,w:Float=8f,h:Float=10f)=GlyphQuad(TextPoint(x,y),TextPoint(x+w,y),TextPoint(x,y+h),TextPoint(x+w,y+h))
    private fun glyph(x:Float,y:Float=10f,cp:Int=65,w:Float=8f,h:Float=10f)=TextGlyph(cp,q(x,y,w,h))
    private fun span(g:List<TextGlyph>)=SelectionSpan(TextAddress(0,0),TextAddress(0,g.lastIndex),"abc","abc",g)
    private fun bands(vararg g:TextGlyph)=NativeHighlightAppearance.forSpan(span(g.toList()))
    @Test fun adjacentCharactersMerge(){assertEquals(1,bands(glyph(0f),glyph(8f),glyph(16f)).size)}
    @Test fun overlappingCharactersMerge(){assertEquals(1,bands(glyph(0f),glyph(7f)).size)}
    @Test fun overlapIsCoveredOnce(){assertEquals(15f,bands(glyph(0f),glyph(7f)).single().bounds.width,0f)}
    @Test fun wordGapFilled(){assertEquals(28f,bands(glyph(0f),glyph(20f)).single().bounds.width,0f)}
    @Test fun justifiedGapFilled(){assertEquals(1,bands(glyph(0f),glyph(30f)).size)}
    @Test fun columnGutterNotFilled(){assertEquals(2,bands(glyph(0f),glyph(100f)).size)}
    @Test fun separateRowsNotMerged(){assertEquals(2,bands(glyph(0f),glyph(0f,30f)).size)}
    @Test fun differentHeightsStaySeparate(){assertEquals(2,bands(glyph(0f),glyph(10f,h=30f)).size)}
    @Test fun nativeOutputPositiveArea(){assertTrue(bands(glyph(0f),glyph(8f)).all{it.area>0})}
    @Test fun logicalQuadsUnchanged(){val g=listOf(glyph(0f),glyph(20f));val s=span(g);bands(*g.toTypedArray());assertEquals(g.map{it.quad},s.quads)}
    @Test fun quotationUnchanged(){val s=span(listOf(glyph(0f)));NativeHighlightAppearance.forSpan(s);assertEquals("abc",s.quotation)}
    @Test fun trailingEllipsisBorrowsHeight(){assertEquals(10f,bands(glyph(0f),glyph(8f,cp=0x2026,w=6f,h=0f)).single().bounds.height,0f)}
    @Test fun leadingEllipsisBorrowsHeight(){assertEquals(10f,bands(glyph(0f,cp=0x2026,w=6f,h=0f),glyph(6f)).single().bounds.height,0f)}
    @Test fun threeDotsJoinBody(){assertEquals(1,bands(glyph(0f),glyph(8f,cp=46,w=2f,h=0f),glyph(10f,cp=46,w=2f,h=0f),glyph(12f,cp=46,w=2f,h=0f),glyph(14f)).size)}
    @Test fun isolatedZeroHeightDotNotInvented(){assertTrue(bands(glyph(0f,cp=46,h=0f)).isEmpty())}
    @Test fun unrelatedZeroHeightLetterNotInflated(){assertTrue(bands(glyph(0f,h=0f)).isEmpty())}
    @Test fun ellipsisAcrossGutterNotBorrowed(){assertEquals(1,bands(glyph(0f),glyph(100f,cp=0x2026,h=0f)).size)}
    @Test fun forQuadsMerges(){assertEquals(1,NativeHighlightAppearance.forQuads(listOf(q(0f),q(8f))).size)}
    @Test fun tiltedQuadsPreserved(){val tilted=GlyphQuad(TextPoint(0f,0f),TextPoint(8f,2f),TextPoint(0f,10f),TextPoint(8f,12f));assertEquals(listOf(tilted),NativeHighlightAppearance.forQuads(listOf(tilted)))}
    @Test fun codecExactRoundTrip(){val qs=listOf(q(0.123456f),q(20f));assertEquals(qs,HighlightGeometryCodec.decode(HighlightGeometryCodec.encode(qs)))}
    @Test fun codecVersionRejected(){try{HighlightGeometryCodec.decode("SGQ2|0,1");fail()}catch(_:IllegalArgumentException){}}
    @Test fun codecNonfiniteRejected(){try{HighlightGeometryCodec.decode("SGQ1|NaN,0,8,0,0,10,8,10");fail()}catch(_:IllegalArgumentException){}}
    @Test fun codecMissingFieldsRejected(){try{HighlightGeometryCodec.decode("SGQ1|1,2");fail()}catch(_:IllegalArgumentException){}}
    @Test fun codecEmptyRejected(){try{HighlightGeometryCodec.encode(emptyList());fail()}catch(_:IllegalArgumentException){}}
    @Test fun codecDegenerateRejected(){try{HighlightGeometryCodec.decode("SGQ1|0,0,0,0,0,0,0,0");fail()}catch(_:IllegalArgumentException){}}
    @Test fun factoryKeepsExactGlyphsAndAddsBand(){val s=span(listOf(glyph(0f),glyph(8f)));val e=HighlightEdit.from(0,0,s);assertEquals(s.quads,e.quads);assertEquals(1,e.appearance.size)}
    @Test fun resizeRetainsManualContents(){val s=span(listOf(glyph(0f)));val e=HighlightEdit.from(0,0,s);val m=ManagedHighlight(e,"e\u0301 corrected",TextMode.MANUAL);assertEquals(m.contents,m.resized(s).contents)}
    @Test fun emptyManualContentsRetained(){val s=span(listOf(glyph(0f)));val m=ManagedHighlight(HighlightEdit.from(0,0,s),"",TextMode.MANUAL);assertEquals("",m.resized(s).contents)}
    @Test fun rawGeometryFactoryDoesNotMutateInput(){val qs=listOf(q(0f),q(20f));NativeHighlightAppearance.forQuads(qs);assertEquals(2,qs.size)}
    @Test fun incompleteCodecRejected(){try{HighlightGeometryCodec.decode("SGQ1|");fail()}catch(_:IllegalArgumentException){}}
}
