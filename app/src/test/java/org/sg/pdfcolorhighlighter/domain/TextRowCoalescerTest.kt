// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class TextRowCoalescerTest {
    private val page=TextBounds(0f,0f,595.28f,841.89f)
    private fun line(text:String,x0:Float,x1:Float,y0:Float,y1:Float,block:Int=2,run:Int=0,reliable:Boolean=true,
                     gaps:Set<Int> = emptySet()):PageTextLine {
        val cps=text.codePoints().toArray();val width=(x1-x0)/cps.size
        return PageTextLine(block,run,cps.mapIndexed{i,cp ->
            val x=x0+i*width
            TextGlyph(cp,GlyphQuad(TextPoint(x,y0),TextPoint(x+width,y0),TextPoint(x,y1),TextPoint(x+width,y1)))
        },reliable,gaps)
    }
    private fun snapshot(lines:List<PageTextLine>)=PageTextSnapshot(page,lines,PageTextStatus.READY)
    private fun support(y:Float)=line("supporting full physical row",40.48f,283.44f,y,y+7.32f)
    private fun fragments():List<PageTextLine> {
        val bounds=listOf(40.48f to 49.33f,56.18f to 84.39f,91.22f to 122.91f,129.78f to 195.20f,
            202.11f to 217.36f,224.16f to 248.31f,255.12f to 270.31f,277.12f to 283.44f)
        val counts=listOf(2,8,9,18,4,6,4,2)
        return bounds.mapIndexed{i,b->line(('a'+i).toString().repeat(counts[i]),b.first,b.second,282.86f,290.18f)}
    }
    private fun input(parts:List<PageTextLine> = fragments())=snapshot(listOf(support(274.30f))+parts+support(291.42f))
    private fun merged(s:PageTextSnapshot)=TextRowCoalescer.normalize(s).lines.first{it.sourceFragments>1}
    private fun center(g:TextGlyph)=TextPoint((g.quad.bounds.left+g.quad.bounds.right)/2,(g.quad.bounds.top+g.quad.bounds.bottom)/2)
    private fun quotation(s:PageTextSnapshot,a:TextPoint,b:TextPoint,words:Boolean=true):SelectionSpan {
        val result=PageTextSelector.select(s,a,b,words)
        assertTrue("Expected selection: $result",result is TextSelectionAttempt.Selected)
        return(result as TextSelectionAttempt.Selected).span
    }
    @Test fun reportedEightFragmentsBecomeOnePhysicalRow(){assertEquals(3,TextRowCoalescer.normalize(input()).lines.size);assertEquals(8,merged(input()).sourceFragments)}
    @Test fun noPhysicalGlyphIsInventedOrDropped(){val p=fragments();val row=merged(input(p));assertEquals(53,row.glyphs.size);assertEquals(p.flatMap{it.glyphs},row.glyphs)}
    @Test fun inferredSpacesHaveNoGlyphOrQuad(){val row=merged(input());assertEquals(7,row.spacesBefore.size);assertEquals(8,row.text.split(' ').size);assertEquals(53,row.glyphs.size)}
    @Test fun nativeQuadsRemainUnchanged(){val p=fragments();assertEquals(p.flatMap{it.glyphs.map{g->g.quad}},merged(input(p)).glyphs.map{it.quad})}
    @Test fun characterObjectsAreRetained(){val p=fragments();assertTrue(merged(input(p)).glyphs.first()===p.first().glyphs.first())}
    @Test fun unorderedSourceFragmentsAreSortedLeftToRight(){assertEquals(merged(input()).text,merged(input(fragments().reversed())).text)}
    @Test fun normalizationIsIdempotent(){val once=TextRowCoalescer.normalize(input());assertSame(once,TextRowCoalescer.normalize(once))}
    @Test fun twoDifferentSupportRowsAreRequired(){val s=snapshot(listOf(support(274.30f))+fragments());assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun duplicateSupportAtSameBaselineDoesNotCountTwice(){val s=snapshot(listOf(support(274.30f),support(274.30f))+fragments());assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun unsupportedFragmentsAreNotMerged(){val p=fragments().map{it.copy(selectable=false)};val s=input(p);assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun differentBlocksDoNotGetMerged(){val p=fragments().mapIndexed{i,l->l.copy(block=i+100)};val s=input(p);assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun differentRunsDoNotGetMerged(){val p=fragments().mapIndexed{i,l->l.copy(run=i+100)};val s=input(p);assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun remoteRowsDoNotSupportCoalescing(){val s=snapshot(listOf(support(100f))+fragments()+support(500f));assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun narrowAnchorDoesNotCoverWholeFragmentRow(){val narrow=listOf(line("A",40f,80f,274f,281.32f),line("B",40f,80f,291.42f,298.74f));val s=snapshot(narrow+fragments());assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun physicalColumnGutterIsNotMerged(){val parts=listOf(line("left",40.48f,283.49f,282.86f,290.18f),line("right",310.45f,553.40f,282.86f,290.18f));val s=snapshot(listOf(line("wide support",40.48f,553.40f,274.30f,281.62f))+parts+line("wide support",40.48f,553.40f,291.42f,298.74f));assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun restrictedAndEmptySnapshotsAreUntouched(){for(status in listOf(PageTextStatus.COPY_RESTRICTED,PageTextStatus.EMPTY,PageTextStatus.TOO_COMPLEX)){val s=input().copy(status=status);assertSame(s,TextRowCoalescer.normalize(s))}}
    @Test fun overBudgetSnapshotIsUntouched(){val s=snapshot(List(2049){support(100f)});assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun existingNativeSpacesAreNotDuplicated(){val p=fragments().toMutableList();p[0]=line("a ",40.48f,49.33f,282.86f,290.18f);val row=merged(input(p));assertFalse(2 in row.spacesBefore);assertTrue(row.text.startsWith("a b"));assertFalse(row.text.startsWith("a  b"))}
    @Test fun inheritedSpaceMetadataIsOffsetCorrectly(){val p=fragments().toMutableList();p[1]=line("bc",56.18f,84.39f,282.86f,290.18f,gaps=setOf(1));val row=merged(input(p));assertTrue(3 in row.spacesBefore)}
    @Test fun combiningMarkFragmentIsNotRelocated(){val p=fragments().toMutableList();p[1]=line("\u0301b",56.18f,84.39f,282.86f,290.18f);val s=input(p);assertSame(s,TextRowCoalescer.normalize(s))}
    @Test fun wordSnappingStopsAtInferredSpace(){val s=TextRowCoalescer.normalize(input());val row=s.lines.first{it.sourceFragments==8};val g=row.glyphs[12];assertEquals("c".repeat(9),quotation(s,center(g),center(g)).quotation)}
    @Test fun wholeReconstructedRowKeepsSpaces(){val s=TextRowCoalescer.normalize(input());val row=s.lines.first{it.sourceFragments==8};assertEquals(row.text,quotation(s,TextPoint(30f,286.52f),TextPoint(290f,286.52f)).quotation)}
    @Test fun reverseReconstructedRangeMatches(){val s=TextRowCoalescer.normalize(input());val a=TextPoint(30f,286.52f);val b=TextPoint(290f,286.52f);assertEquals(quotation(s,a,b),quotation(s,b,a))}
    @Test fun characterModeUsesRealCharacterGeometry(){val s=TextRowCoalescer.normalize(input());val row=s.lines.first{it.sourceFragments==8};val g=row.glyphs[12];val r=quotation(s,center(g),center(g),false);assertEquals("c",r.quotation);assertEquals(listOf(g.quad),r.quads)}
    @Test fun spanningTwoWordsIncludesSpaceInQuoteOnly(){val s=TextRowCoalescer.normalize(input());val row=s.lines.first{it.sourceFragments==8};val r=quotation(s,center(row.glyphs[0]),center(row.glyphs[3]));assertEquals("aa "+"b".repeat(8),r.quotation);assertEquals(10,r.glyphs.size)}
    @Test fun multilineCrossesReconstructedRow(){val s=TextRowCoalescer.normalize(input());val first=s.lines.first{it.bounds.top==274.30f};val last=s.lines.first{it.bounds.top==291.42f};assertTrue(quotation(s,center(first.glyphs.first()),center(last.glyphs.last())).quotation.contains(merged(input()).text))}
    @Test fun measuredParagraphGapNowPasses(){val a=line("previous paragraph",40.49f,205.13f,661.81f,669.14f,8);val b=line("next paragraph",48.59f,283.44f,676.78f,684.11f,9);assertTrue(TextGeometryPolicy.sameFlow(a,b));assertEquals("previous paragraph next paragraph",quotation(snapshot(listOf(a,b)),center(a.glyphs.first()),center(b.glyphs.last())).quotation)}
    @Test fun largeVerticalGapAndColumnJumpRemainRejected(){val a=line("left",40f,205f,661f,669f,8);val b=line("footer",40f,205f,738f,746f,9);val c=line("right",310f,553f,677f,685f,9);assertFalse(TextGeometryPolicy.sameFlow(a,b));assertFalse(TextGeometryPolicy.sameFlow(a,c))}
}
