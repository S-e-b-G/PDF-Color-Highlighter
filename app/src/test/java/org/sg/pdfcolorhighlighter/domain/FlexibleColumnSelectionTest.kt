// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class FlexibleColumnSelectionTest {
    private fun line(text:String,left:Float,right:Float,y:Float,h:Float=12.19f,block:Int=0):PageTextLine {
        val cp=text.codePoints().toArray();val step=(right-left)/cp.size
        return PageTextLine(block,0,cp.mapIndexed{i,c->val x=left+i*step;TextGlyph(c,
            GlyphQuad(TextPoint(x,y),TextPoint(x+step,y),TextPoint(x,y+h),TextPoint(x+step,y+h)))})
    }
    private fun jury():PageTextSnapshot=PageTextSnapshot(TextBounds(0f,0f,596f,842f),listOf(
        line("Committee includes the following members :",99.78f,479.81f,90.02f,block=2),
        line("● ",117.78f,126.60f,104.66f,11.18f,3),
        line("the chair and delegates,",135.78f,383.84f,104.04f,block=3),
        line("● regional directors,",117.78f,253.97f,118.06f,block=3),
        line("● studies coordinators.",117.78f,311.91f,132.07f,block=3),
        line("The final paragraph records the decision.",99.78f,525.20f,146.09f,block=3)),PageTextStatus.READY)
    private fun snap(vararg rows:PageTextLine)=PageTextSnapshot(TextBounds(0f,0f,1000f,100000f),rows.toList(),PageTextStatus.READY)
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    private fun select(s:PageTextSnapshot,first:Int=0,last:Int=s.lines.lastIndex):SelectionSpan {
        val r=PageTextSelector.select(s,center(s.lines[first].glyphs.first()),center(s.lines[last].glyphs.last()),false)
        assertTrue("Expected selection: $r",r is TextSelectionAttempt.Selected)
        return (r as TextSelectionAttempt.Selected).span
    }
    private fun glyph()=line("A",10f,20f,10f).glyphs.single()
    private fun span(n:Int)=SelectionSpan(TextAddress(0,0),TextAddress(0,0),"A","A",List(n){glyph()})
    @Test fun introThroughSecondItemIsAllowed(){val q=select(jury(),0,3).quotation;assertTrue(q.contains("Committee") && q.contains("delegates") && q.contains("regional directors"))}
    @Test fun reverseLastItemThroughIntroIsIdentical(){val s=jury();val a=center(s.lines[0].glyphs.first());val b=center(s.lines[4].glyphs.last());assertEquals(PageTextSelector.select(s,a,b,false),PageTextSelector.select(s,b,a,false));assertTrue(select(s,0,4).quotation.contains("coordinators"))}
    @Test fun detachedMarkerIsIncludedInIntermediateRow(){val r=select(jury(),0,3);assertEquals(2,r.glyphs.count{it.codePoint==0x25CF})}
    @Test fun endingOnFirstItemAlsoIncludesItsMarker(){assertEquals(1,select(jury(),0,2).glyphs.count{it.codePoint==0x25CF})}
    @Test fun startingInsideItemStillContinues(){val q=select(jury(),2,4).quotation;assertTrue(q.contains("chair") && q.contains("coordinators"))}
    @Test fun actualSplitRowOrderIsLeftToRight(){assertEquals(listOf(0,1,2,3,4),ColumnFlowRows.path(jury(),0,4))}
    @Test fun nativeBlockIdsAreNotBarriers(){val s=jury();val changed=s.copy(lines=s.lines.mapIndexed{i,l->l.copy(block=i+100)});assertTrue(select(changed,0,4).quotation.contains("coordinators"))}
    @Test fun followingParagraphIsIncluded(){assertTrue(select(jury()).quotation.contains("The final paragraph"))}
    @Test fun largeVerticalGapIsNotABoundary(){val s=snap(line("start",20f,100f,20f),line("end",20f,100f,600f,block=9));assertTrue(select(s).quotation.contains("end"))}
    @Test fun fontHeightChangesAreNotBoundaries(){val s=snap(line("TITLE",20f,200f,20f,30f),line("body",20f,120f,80f,8f,9));assertTrue(select(s).quotation.contains("body"))}
    @Test fun indentChangesStaySelectable(){val s=snap(line("intro",20f,400f,20f),line("indented",100f,300f,50f,block=9),line("end",20f,200f,100f,block=10));assertTrue(select(s).quotation.contains("indented"))}
    @Test fun unknownIntermediateTextIsNotSilentlySkipped(){val s=jury();val ls=s.lines.toMutableList();ls[3]=ls[3].copy(selectable=false);val bad=s.copy(lines=ls);assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),PageTextSelector.select(bad,center(ls[0].glyphs.first()),center(ls[4].glyphs.last()),false))}
    @Test fun copyRestrictionIsStillRespected(){val s=jury().copy(status=PageTextStatus.COPY_RESTRICTED);assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.COPY_RESTRICTED),PageTextSelector.select(s,TextPoint(100f,95f),TextPoint(150f,140f)))}
    @Test fun separateColumnsStillRequireAReadingOrderDecision(){val s=snap(line("LEFT",20f,100f,20f),line("RIGHT",300f,400f,50f,block=9));assertNull(ColumnFlowRows.path(s,0,1))}
    @Test fun interleavedOtherColumnDoesNotEnterRange(){val s=snap(line("left one",20f,120f,20f),line("right one",300f,430f,20f,block=9),line("left two",20f,120f,60f,block=2),line("right two",300f,430f,60f,block=10));val q=select(s,0,2).quotation;assertTrue(q.contains("left two"));assertFalse(q.contains("right"))}
    @Test fun wideHeadingUsesEndColumnInsteadOfRejecting(){val s=snap(line("wide title",20f,500f,20f),line("left",20f,100f,50f,block=9),line("right",300f,450f,50f,block=10),line("end",20f,100f,90f,block=11));val q=select(s).quotation;assertTrue(q.contains("left"));assertFalse(q.contains("right"))}
    @Test fun differentValidMarkerShapesDoNotBlockSelection(){val s=jury();val ls=s.lines.toMutableList();ls[1]=line("◆ ",117.78f,126.60f,104.66f,11.18f,99);val q=select(s.copy(lines=ls),0,4).quotation;assertTrue(q.contains("◆") && q.contains("coordinators"))}
    @Test fun sourceArrayOrderDoesNotControlVisualRows(){val s=jury();val rows=listOf(s.lines[4],s.lines[2],s.lines[0],s.lines[3],s.lines[1],s.lines[5]);val shuffled=s.copy(lines=rows);assertTrue(select(shuffled,2,0).quotation.contains("chair"))}
    @Test fun sameBaselineMarkerAndTextCanBeSelectedTogether(){val s=jury();val r=select(s,1,2);assertTrue(r.quotation.contains("●") && r.quotation.contains("chair"))}
    @Test fun anAdjacentDifferentBlockPrefixIsIncluded(){val s=jury();val ls=s.lines.toMutableList();ls[1]=ls[1].copy(block=77);val r=select(s.copy(lines=ls),0,3);assertEquals(2,r.glyphs.count{it.codePoint==0x25CF})}
    @Test fun moreThan5000GlyphsCanBePreviewed(){assertTrue(SelectionPreviewQueue.allows(span(6000)))}
    @Test fun newPreviewBudgetIsExplicit(){assertEquals(25000,SelectionPreviewQueue.MAX_VISIBLE_GLYPHS);assertTrue(SelectionPreviewQueue.allows(span(25000)))}
    @Test fun resourceBudgetRejectsRatherThanTruncates(){assertFalse(SelectionPreviewQueue.allows(span(25001)))}
    @Test fun longActualRangeIsComplete(){val rows=(0..4).map{line("A".repeat(1200),20f,800f,20f+it*20f,block=it)};val s=snap(*rows.toTypedArray());val r=select(s);assertEquals(6000,r.glyphs.size);assertTrue(SelectionPreviewQueue.allows(r))}
    @Test fun moreThan2048RowsHasNoOldFlowLimit(){val rows=(0 until 2500).map{line("A",20f,30f,it*20f+20f,block=it)};val s=snap(*rows.toTypedArray());assertEquals(2500,ColumnFlowRows.path(s,0,2499)!!.size)}
    @Test fun allRealGlyphObjectsAndQuadsArePreserved(){val s=jury();val r=select(s,0,4);val expected=s.lines.take(5).flatMap{it.glyphs};assertEquals(expected,r.glyphs);assertEquals(expected.map{it.quad}.filter{it.area>=.000001},r.quads)}
    @Test fun handleAdjustmentUsesTheSameFlexibleRange(){val s=jury();val r=PageTextSelector.adjust(s,center(s.lines[4].glyphs.last()),TextAddress(0,0));assertTrue(r is TextSelectionAttempt.Selected);assertTrue((r as TextSelectionAttempt.Selected).span.quotation.contains("coordinators"))}
    @Test fun repeatedCallsKeepTheSameDeterministicPath(){val s=jury();val first=ColumnFlowRows.path(s,0,4);repeat(20){assertEquals(first,ColumnFlowRows.path(s,4,0))}}
}
