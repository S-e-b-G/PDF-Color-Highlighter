// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ColumnContinuationTest {
    private fun line(text:String,x:Float,y:Float)=PageTextLine(0,0,text.mapIndexed{i,c->
        val left=x+i*5f;TextGlyph(c.code,GlyphQuad(TextPoint(left,y),TextPoint(left+5,y),TextPoint(left,y+10),TextPoint(left+5,y+10)))})
    private fun snap(n:Int=4):PageTextSnapshot {
        val lines=(0 until n).flatMap{i->listOf(line("LEFT $i body words",20f,20f+i*20),line("RIGHT $i body words",300f,20f+i*20))}
        return PageTextSnapshot(TextBounds(0f,0f,600f,800f),lines,PageTextStatus.READY)
    }
    private fun point(g:TextGlyph):TextPoint {val q=g.quad.bounds;return TextPoint((q.left+q.right)/2,(q.top+q.bottom)/2)}
    private fun span(s:PageTextSnapshot=snap(),a:Int=4,b:Int=3):SelectionSpan {
        return (PageTextSelector.select(s,point(s.lines[a].glyphs.first()),point(s.lines[b].glyphs.last()),false) as TextSelectionAttempt.Selected).span
    }
    @Test fun explicitLeftThenRightOrder(){assertEquals(listOf(0,2,4,6,1,3,5,7),ColumnContinuationRows.path(snap(),4,3))}
    @Test fun selectedSuffixThenPrefixOnly(){assertEquals("LEFT 2 body words\nLEFT 3 body words\nRIGHT 0 body words\nRIGHT 1 body words",span().visualText)}
    @Test fun reverseGestureIsIdentical(){val s=snap();val a=point(s.lines[4].glyphs.first());val b=point(s.lines[3].glyphs.last());assertEquals(PageTextSelector.select(s,a,b,false),PageTextSelector.select(s,b,a,false))}
    @Test fun characterBoundariesAreExact(){val s=snap();val r=PageTextSelector.select(s,point(s.lines[4].glyphs[5]),point(s.lines[3].glyphs[6]),false) as TextSelectionAttempt.Selected;assertEquals(TextAddress(4,5),r.span.start);assertEquals(TextAddress(3,6),r.span.end)}
    @Test fun handlesUseTheSameReadingOrder(){val s=snap();val r=PageTextSelector.adjust(s,point(s.lines[3].glyphs.last()),TextAddress(4,0)) as TextSelectionAttempt.Selected;assertEquals(span(s),r.span)}
    @Test fun sourceArrayOrderDoesNotMatter(){val s=snap();val rows=s.lines.reversed();val other=s.copy(lines=rows);assertEquals(span(s).quotation,span(other,rows.indexOf(s.lines[4]),rows.indexOf(s.lines[3])).quotation)}
    @Test fun twoRowsAreNotSufficient(){assertNull(ColumnContinuationRows.path(snap(2),0,3))}
    @Test fun thirdLaneIsRejected(){val s=snap();assertNull(ColumnContinuationRows.path(s.copy(lines=s.lines+line("MIDDLE",210f,40f)),4,3))}
    @Test fun aSpanningRowIsRejected(){val s=snap();assertNull(ColumnContinuationRows.path(s.copy(lines=s.lines+line("SPANNING ".repeat(9),20f,50f)),4,3))}
    @Test fun copyPermissionStillWins(){val s=snap().copy(status=PageTextStatus.COPY_RESTRICTED);assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.COPY_RESTRICTED),PageTextSelector.select(s,point(s.lines[4].glyphs.first()),point(s.lines[3].glyphs.last())))}
    @Test fun includedUnsupportedTextIsNotSkipped(){val s=snap();val ls=s.lines.toMutableList();ls[6]=ls[6].copy(selectable=false);val bad=s.copy(lines=ls);assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),PageTextSelector.select(bad,point(ls[4].glyphs.first()),point(ls[3].glyphs.last()),false))}
    @Test fun insufficientGutterIsRejected(){val s=snap();val lines=s.lines.mapIndexed{i,l->if(i%2==1)line(l.text,130f,l.bounds.top)else l};assertNull(ColumnContinuationRows.path(s.copy(lines=lines),4,3))}
    @Test fun sameColumnIsUnchanged(){val s=snap();assertEquals(listOf(0,2,4,6),TextFlowResolver.path(s,0,6))}
    @Test fun previewDoesNotPaintAcrossGutter(){val bands=PreviewPaintGeometry.bands(span());assertTrue(bands.isNotEmpty());assertTrue(bands.none{it.left<200 && it.right>280})}
    @Test fun nativeOrderRestoresInterleavedExtraction(){val s=snap();val q=span(s);val e=HighlightEdit(UUID.randomUUID().toString(),0,1,q.quotation,q.quads);val h=ManagedHighlight(e,"Manual quote",TextMode.MANUAL);assertEquals(q,ManagedHighlightSelection.span(s,h));assertEquals("Manual quote",h.resized(q).contents)}
    @Test fun sparseNativeRangesStillReject(){val s=snap();val q=span(s);val e=HighlightEdit(UUID.randomUUID().toString(),0,1,q.quotation,q.quads.filterIndexed{i,_->i!=8});assertNull(ManagedHighlightSelection.span(s,ManagedHighlight(e,q.quotation,TextMode.AUTOMATIC)))}
}
