// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class SelectionLayoutReportTest {
    private val bounds=TextBounds(-10f,-20f,590f,822f)
    private fun line(text:String="SECRET PRIVATE QUOTATION",block:Int=3,run:Int=2,reliable:Boolean=true)=PageTextLine(block,run,
        text.codePoints().toArray().mapIndexed{index,cp ->
            val x=20f+index*5f
            TextGlyph(cp,GlyphQuad(TextPoint(x,30f),TextPoint(x+5f,30f),TextPoint(x,40f),TextPoint(x+5f,40f)))
        },reliable)
    private fun snapshot(vararg lines:PageTextLine)=PageTextSnapshot(bounds,lines.toList(),PageTextStatus.READY)
    @Test fun nullSnapshotIsExplicitlyUnavailable(){val r=SelectionLayoutDiagnostics.build(null,null);assertNull(r.status);assertNull(r.pageBounds);assertEquals(0,r.totalLines);assertTrue(r.lines.isEmpty())}
    @Test fun totalLineCountIsCorrect(){assertEquals(2,SelectionLayoutDiagnostics.build(snapshot(line("A"),line("B")),null).totalLines)}
    @Test fun glyphCountIncludesAllRows(){assertEquals(5L,SelectionLayoutDiagnostics.build(snapshot(line("AB"),line("C D")),null).totalGlyphs)}
    @Test fun nativeBoundsAndOriginArePreserved(){assertEquals(bounds,SelectionLayoutDiagnostics.build(snapshot(line()),null).pageBounds)}
    @Test fun numericIdsAndCompatibilityAreCopied(){val r=SelectionLayoutDiagnostics.build(snapshot(line(block=9,run=4,reliable=false)),null).lines.single();assertEquals(9,r.block);assertEquals(4,r.run);assertFalse(r.selectable);assertEquals(0,r.index)}
    @Test fun truncationIsBoundedButTotalsAreNotTruncated(){val r=SelectionLayoutDiagnostics.build(snapshot(line("A"),line("BC"),line("D")),null,2);assertEquals(2,r.lines.size);assertEquals(3,r.totalLines);assertEquals(4L,r.totalGlyphs);assertTrue(r.truncated)}
    @Test fun noTruncationAtExactLimit(){assertFalse(SelectionLayoutDiagnostics.build(snapshot(line("A"),line("B")),null,2).truncated)}
    @Test fun invalidLimitIsRejected(){for(n in listOf(0,-1,501))try{SelectionLayoutDiagnostics.build(null,null,n);fail("Invalid limit accepted")}catch(_:IllegalArgumentException){}}
    @Test fun wordsAndQuotationAreNotInReport(){val r=SelectionLayoutDiagnostics.build(snapshot(line()),SelectionRejection.DIFFERENT_TEXT_FLOW);assertFalse(r.toString().contains("SECRET"));assertFalse(r.toString().contains("PRIVATE"));assertFalse(r.toString().contains("QUOTATION"))}
    @Test fun statusAndReasonAreReportedNotInferred(){val s=snapshot(line()).copy(status=PageTextStatus.COPY_RESTRICTED);val r=SelectionLayoutDiagnostics.build(s,SelectionRejection.NO_GLYPH);assertEquals(PageTextStatus.COPY_RESTRICTED,r.status);assertEquals(SelectionRejection.NO_GLYPH,r.rejection)}
}
