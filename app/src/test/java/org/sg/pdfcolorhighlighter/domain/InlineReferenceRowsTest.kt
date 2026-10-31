// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class InlineReferenceRowsTest {
    private fun line(text:String,x0:Float,x1:Float,y:Float,block:Int=16,run:Int=0,h:Float=6.41f)=
        PageTextLine(block,run,text.mapIndexed{i,c->
            val step=(x1-x0)/text.length;val x=x0+i*step
            TextGlyph(c.code,GlyphQuad(TextPoint(x,y),TextPoint(x+step,y),TextPoint(x,y+h),TextPoint(x+step,y+h)))})
    private fun fixture():PageTextSnapshot = PageTextSnapshot(TextBounds(0f,0f,595.28f,841.89f),listOf(
        line("prEN 9277:2013",48.59f,97.53f,377.34f),line("(2013).",121.49f,142.09f,377.34f),
        line("Description of the first standard.",40.48f,195.92f,384.94f),
        line("prEN 9320:2013",48.58f,97.52f,395.83f,17),line("(2013)",121.48f,140.21f,395.83f,17),
        line("Description of the second standard continued",40.47f,283.43f,403.42f,17),
        line("on its next visual row.",40.47f,97.61f,411.02f,17)),PageTextStatus.READY)
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    private fun normalized()=InlineReferenceRows.normalize(fixture())
    private fun selection(s:PageTextSnapshot,a:Int=0,b:Int=s.lines.lastIndex):SelectionSpan {
        val r=PageTextSelector.select(s,center(s.lines[a].glyphs.first()),center(s.lines[b].glyphs.last()),false)
        assertTrue("Expected selection: $r",r is TextSelectionAttempt.Selected)
        return (r as TextSelectionAttempt.Selected).span
    }
    private fun changed(index:Int,transform:(PageTextLine)->PageTextLine):PageTextSnapshot {
        val s=fixture();return s.copy(lines=s.lines.mapIndexed{i,l->if(i==index)transform(l)else l})
    }
    @Test fun bothDetachedYearsAreJoined(){val n=normalized();assertEquals(5,n.lines.size);assertEquals("prEN 9277:2013 (2013).",n.lines[0].text);assertEquals("prEN 9320:2013 (2013)",n.lines[2].text)}
    @Test fun singleEntryQuotationRetainsYear(){assertTrue(selection(normalized(),0,1).quotation.contains("9277:2013 (2013)."))}
    @Test fun twoEntryQuotationRetainsBothYears(){val q=selection(normalized()).quotation;assertTrue(q.contains("9277:2013 (2013)."));assertTrue(q.contains("9320:2013 (2013)"));assertTrue(q.contains("next visual row"))}
    @Test fun reverseSelectionKeepsSameCompleteSpan(){val s=normalized();val a=center(s.lines.first().glyphs.first());val b=center(s.lines.last().glyphs.last());assertEquals(PageTextSelector.select(s,a,b,false),PageTextSelector.select(s,b,a,false))}
    @Test fun realGlyphCountIsPreserved(){val s=fixture();val n=InlineReferenceRows.normalize(s);assertEquals(s.lines.sumOf{it.glyphs.size},n.lines.sumOf{it.glyphs.size})}
    @Test fun realHeaderGlyphObjectsAreReused(){val s=fixture();val n=InlineReferenceRows.normalize(s);assertSame(s.lines[0].glyphs.first(),n.lines[0].glyphs.first());assertSame(s.lines[1].glyphs.last(),n.lines[0].glyphs.last())}
    @Test fun nativeQuadsAreNotInvented(){val s=fixture();val n=InlineReferenceRows.normalize(s);assertEquals(s.lines.flatMap{it.glyphs}.map{it.quad},n.lines.flatMap{it.glyphs}.map{it.quad})}
    @Test fun logicalGapMetadataIsRecorded(){val s=fixture();val n=InlineReferenceRows.normalize(s);assertTrue(s.lines[0].glyphs.size in n.lines[0].spacesBefore);assertEquals(2,n.lines[0].sourceFragments)}
    @Test fun normalizationIsIdempotent(){val n=normalized();assertSame(n,InlineReferenceRows.normalize(n))}
    @Test fun restrictedSnapshotIsUntouched(){val s=fixture().copy(status=PageTextStatus.COPY_RESTRICTED);assertSame(s,InlineReferenceRows.normalize(s))}
    @Test fun complexSnapshotIsUntouched(){val s=fixture().copy(status=PageTextStatus.TOO_COMPLEX);assertSame(s,InlineReferenceRows.normalize(s))}
    @Test fun oversizeSnapshotIsNotNormalized(){val l=fixture().lines[0];val s=fixture().copy(lines=List(InlineReferenceRows.MAX_LINES+1){l});assertSame(s,InlineReferenceRows.normalize(s))}
    @Test fun differentNativeBlockDoesNotJoin(){val s=changed(1){it.copy(block=99)};val n=InlineReferenceRows.normalize(s);assertFalse(n.lines.first().text.contains("(2013)"))}
    @Test fun differentRunDoesNotJoin(){val s=changed(1){it.copy(run=1)};assertFalse(InlineReferenceRows.normalize(s).lines.first().text.contains("(2013)"))}
    @Test fun wideColumnGapDoesNotJoin(){val s=changed(1){line("(2013).",310f,331f,377.34f)};assertFalse(InlineReferenceRows.normalize(s).lines.first().text.contains("(2013)"))}
    @Test fun aBareTwoCellRowDoesNotJoin(){val s=fixture().copy(lines=fixture().lines.take(2));assertSame(s,InlineReferenceRows.normalize(s))}
    @Test fun distantBodyDoesNotSupportMerge(){val s=changed(2){line("Description",40.48f,195.92f,450f)};assertFalse(InlineReferenceRows.normalize(s).lines.first().text.contains("(2013)"))}
    @Test fun aNarrowSeparateColumnIsNotSupport(){val s=changed(2){line("Description",121.49f,195.92f,384.94f)};assertFalse(InlineReferenceRows.normalize(s).lines.first().text.contains("(2013)"))}
    @Test fun nonYearMetadataIsNotBlindlyJoined(){val s=changed(1){line("(draft)",121.49f,142.09f,377.34f)};assertFalse(InlineReferenceRows.normalize(s).lines.first().text.contains("draft"))}
    @Test fun alreadyMergedFragmentIsNotMergedAgain(){val s=changed(0){it.copy(sourceFragments=2)};assertFalse(InlineReferenceRows.normalize(s).lines.first().text.contains("(2013)"))}
    @Test fun unknownCharacterPreventsInterpretation(){val s=changed(1){val g=it.glyphs.toMutableList();g[1]=g[1].copy(unicodeReliable=false);it.copy(glyphs=g)};assertFalse(InlineReferenceRows.normalize(s).lines.first().text.contains("(2013)"))}
    @Test fun ambiguousTwoYearCandidatesDoNotJoin(){val s=fixture();val extra=line("(2014)",122f,141f,377.34f);val n=InlineReferenceRows.normalize(s.copy(lines=s.lines+extra));assertFalse(n.lines.first().text.contains("(2013)"))}
    @Test fun yearRangesAreSupportedWithoutHardcodedYear(){val s=changed(1){line("(2008–2010).",121.49f,145f,377.34f)};assertTrue(InlineReferenceRows.normalize(s).lines.first().text.contains("(2008–2010)."))}
    @Test fun unrelatedRightColumnStaysSeparate(){val s=fixture();val right=line("RIGHT COLUMN",310f,553f,377.34f,90);val n=InlineReferenceRows.normalize(s.copy(lines=s.lines+right));assertEquals(6,n.lines.size);assertSame(right,n.lines.last());assertFalse(n.lines.first().text.contains("RIGHT"))}
}
