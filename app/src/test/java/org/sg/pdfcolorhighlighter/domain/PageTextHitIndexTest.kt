// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
import java.util.Random
class PageTextHitIndexTest{
    private fun line(y:Float=0f,h:Float=6f)=PageTextLine(0,0,List(10){i->TextGlyph(65+i,
        GlyphQuad(TextPoint(i*10f,y),TextPoint(i*10f+8f,y),TextPoint(i*10f,y+h),TextPoint(i*10f+8f,y+h)))})
    private fun snapshot(lines:List<PageTextLine>,b:TextBounds=TextBounds(0f,0f,1000f,1000f))=PageTextSnapshot(b,lines,PageTextStatus.READY)
    private fun brute(s:PageTextSnapshot,p:TextPoint):TextAddress?{
        s.lines.forEachIndexed{i,l->l.glyphs.forEachIndexed{j,g->if(g.quad.contains(p))return TextAddress(i,j)}};return null
    }
    @Test fun glyphBoundsAreCached(){val q=line().glyphs.first().quad;assertSame(q.bounds,q.bounds)}
    @Test fun lineBoundsAreCached(){val l=line();assertSame(l.bounds,l.bounds)}
    @Test fun findsCorrectNativeAddress(){val s=snapshot(List(100){line(it*10f)});val index=PageTextHitIndex(s);assertTrue(index.usesBuckets);assertTrue(index.maximumBucketSize<10);assertEquals(TextAddress(50,4),index.hit(TextPoint(44f,503f)))}
    @Test fun overlappingQuadsPreserveOriginalOrder(){val l=line();val s=snapshot(listOf(l,l));assertEquals(TextAddress(0,0),PageTextHitIndex(s).hit(TextPoint(4f,3f)))}
    @Test fun blankSpaceIsNotAGlyphHit(){assertNull(PageTextHitIndex(snapshot(listOf(line()))).hit(TextPoint(500f,500f)))}
    @Test fun negativePageOriginStillHits(){val s=snapshot(listOf(line(-20f)),TextBounds(-100f,-100f,900f,900f));assertEquals(TextAddress(0,0),PageTextHitIndex(s).hit(TextPoint(4f,-17f)))}
    @Test fun oversizedIndexFallsBackWithoutWrongHits(){val l=line();val large=PageTextHitIndex(snapshot(List(20001){l}));assertFalse(large.usesBuckets);assertEquals(TextAddress(0,0),large.hit(TextPoint(4f,3f)));val tall=line(0f,1000f);assertFalse(PageTextHitIndex(snapshot(List(2000){tall})).usesBuckets)}
    @Test fun indexedHitsEqualStrictQuadScan(){val s=snapshot(List(100){line(it*10f)});val index=PageTextHitIndex(s);val random=Random(268);repeat(300){val p=TextPoint(random.nextFloat()*110f,random.nextFloat()*1000f);assertEquals(brute(s,p),index.hit(p))}}
}
