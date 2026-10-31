// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class HorizontalFlowOrientationTest {
    private fun line(text:String,left:Float,right:Float,top:Float,bottom:Float,
                     block:Int=0,horizontal:Boolean=true,reliable:Boolean=true):PageTextLine {
        val cps=text.codePoints().toArray();val step=(right-left)/cps.size
        return PageTextLine(block,0,cps.mapIndexed{i,cp->val x=left+i*step;TextGlyph(cp,
            GlyphQuad(TextPoint(x,top),TextPoint(x+step,top),TextPoint(x,bottom),TextPoint(x+step,bottom)))},
            selectable=reliable,horizontal=horizontal)
    }
    private fun fixture(diagonal:Boolean=false):PageTextSnapshot {
        val stamp=if(diagonal)line("SYNTHETIC DIAGONAL",60f,250f,390f,583f,90,false,false)
            else line("SYNTHETIC VERTICAL SIDEBAR",17.203f,23.203f,264.421f,708.661f,90,false,false)
        return PageTextSnapshot(TextBounds(0f,0f,595.28f,841.89f),listOf(
            line("First paragraph line",48.556f,283.43f,465.907f,473.497f),
            line("Continuation before author",40.457f,283.43f,474.469f,482.059f,1),
            line("Author reference continues here",40.457f,283.43f,483.031f,490.621f,2),
            line("Last paragraph line",40.457f,283.43f,491.593f,499.183f,3),stamp),PageTextStatus.READY)
    }
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    private fun attempt(s:PageTextSnapshot,first:Int=0,last:Int=3)=PageTextSelector.select(s,
        center(s.lines[first].glyphs.first()),center(s.lines[last].glyphs.last()),false)
    private fun selected(s:PageTextSnapshot):SelectionSpan {
        val r=attempt(s);assertTrue("Expected selection: $r",r is TextSelectionAttempt.Selected)
        return (r as TextSelectionAttempt.Selected).span
    }
    @Test fun verticalSidebarDoesNotRejectAuthorLine(){val r=selected(fixture());assertTrue(r.quotation.contains("Author reference"));assertFalse(r.quotation.contains("SIDEBAR"))}
    @Test fun diagonalOverlayDoesNotEnterHorizontalRange(){val r=selected(fixture(true));assertTrue(r.quotation.contains("Author reference"));assertFalse(r.quotation.contains("DIAGONAL"))}
    @Test fun exactSideWatermarkCenterCannotPoisonItsPhysicalRow(){val s=fixture();assertEquals(listOf(0,1,2,3),ColumnFlowRows.path(s,0,3))}
    @Test fun reverseSelectionHasTheSameCompleteText(){val s=fixture();val a=center(s.lines[0].glyphs.first());val b=center(s.lines[3].glyphs.last());assertEquals(PageTextSelector.select(s,a,b,false),PageTextSelector.select(s,b,a,false))}
    @Test fun sidebarIsNotDeletedFromSnapshot(){val s=fixture();val stamp=s.lines.last();selected(s);assertEquals(5,s.lines.size);assertSame(stamp,s.lines.last());assertFalse(stamp.horizontal)}
    @Test fun selectedBodyGlyphsAndQuadsAreUnchanged(){val s=fixture();val r=selected(s);val expected=s.lines.take(4).flatMap{it.glyphs};assertEquals(expected,r.glyphs);assertEquals(expected.map{it.quad},r.quads)}
    @Test fun unreadableHorizontalTextStillRejectsRatherThanDisappears(){val s=fixture();val rows=s.lines.toMutableList();rows[2]=rows[2].copy(selectable=false);assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),attempt(s.copy(lines=rows)))}
    @Test fun unknownHorizontalReplacementStillRejects(){val s=fixture();val rows=s.lines.toMutableList();val g=rows[2].glyphs.toMutableList();g[2]=g[2].copy(codePoint=0xFFFD,sourceCodePoint=0xFFFD);rows[2]=rows[2].copy(glyphs=g);assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),attempt(s.copy(lines=rows)))}
    @Test fun explicitRotatedEndpointRemainsUnsupported(){val s=fixture();assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),attempt(s,4,4))}
    @Test fun rotatedEndpointDoesNotProduceAFalseHorizontalPath(){val s=fixture();assertNull(ColumnFlowRows.path(s,4,3));assertNull(ColumnFlowRows.path(s,4,4))}
    @Test fun writingDirectionNotCharacterNameControlsExclusion(){val s=fixture();val rows=s.lines.toMutableList();rows[4]=line("ANY OTHER CONTENT",17f,24f,264f,709f,90,false,false);assertFalse(selected(s.copy(lines=rows)).quotation.contains("ANY OTHER"))}
    @Test fun largeFontHorizontalHeadingIsNotExcluded(){val top=line("LARGE HORIZONTAL HEADING",40f,280f,300f,360f);val body=line("small body remains selectable",40f,280f,400f,408f,1);val s=PageTextSnapshot(TextBounds(0f,0f,595f,842f),listOf(top,body),PageTextStatus.READY);val r=attempt(s,0,1);assertTrue(r is TextSelectionAttempt.Selected);assertTrue((r as TextSelectionAttempt.Selected).span.quotation.contains("small body"))}
}
