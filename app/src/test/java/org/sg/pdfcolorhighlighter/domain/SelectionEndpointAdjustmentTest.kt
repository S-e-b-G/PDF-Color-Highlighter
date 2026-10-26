// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class SelectionEndpointAdjustmentTest{
    private fun line(text:String,x:Float=10f,y:Float=20f,block:Int=0)=PageTextLine(block,0,text.codePoints().toArray().mapIndexed{i,cp->
        val left=x+i*10f;TextGlyph(cp,GlyphQuad(TextPoint(left,y),TextPoint(left+10f,y),TextPoint(left,y+10f),TextPoint(left+10f,y+10f)))})
    private fun snap(vararg lines:PageTextLine)=PageTextSnapshot(TextBounds(0f,0f,600f,800f),lines.toList(),PageTextStatus.READY)
    private fun p(s:PageTextSnapshot,i:Int,line:Int=0):TextPoint{val b=s.lines[line].glyphs[i].quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    private fun result(s:PageTextSnapshot,m:TextPoint,f:TextAddress):SelectionSpan{val r=PageTextSelector.adjust(s,m,f);assertTrue("Expected selection: $r",r is TextSelectionAttempt.Selected);return(r as TextSelectionAttempt.Selected).span}
    @Test fun startMovesInsideWordWithoutWordSnap(){val s=snap(line("abc def"));assertEquals("bc def",result(s,p(s,1),TextAddress(0,6)).quotation)}
    @Test fun endMovesInsideWordWithoutWordSnap(){val s=snap(line("abc def"));assertEquals("abc de",result(s,p(s,5),TextAddress(0,0)).quotation)}
    @Test fun fixedEndpointIsNotResolvedFromTouchCoordinates(){val s=snap(line("abcdef"));assertEquals(TextAddress(0,3),result(s,p(s,1),TextAddress(0,3)).end)}
    @Test fun crossingTheFixedEndpointSortsTheRange(){val s=snap(line("abcdef"));val r=result(s,p(s,1),TextAddress(0,3));assertEquals("bcd",r.quotation);assertEquals(TextAddress(0,1),r.start)}
    @Test fun fixedZeroAreaPunctuationRemainsInQuotation(){val ordinary=line("abc.");val g=ordinary.glyphs.toMutableList();g[3]=TextGlyph(46,GlyphQuad(TextPoint(40f,30f),TextPoint(44f,30f),TextPoint(40f,30f),TextPoint(44f,30f)));val s=snap(ordinary.copy(glyphs=g));assertEquals("abc.",result(s,p(s,0),TextAddress(0,3)).quotation)}
    @Test fun combiningSequenceRemainsWhole(){val s=snap(line("e\u0301abc"));assertEquals("éa",result(s,p(s,2),TextAddress(0,1)).quotation)}
    @Test fun inferredSpacesArePreserved(){val l=line("abcd").copy(spacesBefore=setOf(2),sourceFragments=2);val s=snap(l);assertEquals("ab cd",result(s,p(s,0),TextAddress(0,3)).quotation)}
    @Test fun invalidFixedAddressReturnsRejection(){val s=snap(line("abc"));assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.NO_GLYPH),PageTextSelector.adjust(s,p(s,0),TextAddress(20,99)))}
    @Test fun copyRestrictionIsStillHonored(){val s=snap(line("abc"));assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.COPY_RESTRICTED),PageTextSelector.adjust(s.copy(status=PageTextStatus.COPY_RESTRICTED),p(s,0),TextAddress(0,1)))}
    @Test fun handleCannotJumpIntoAnotherColumn(){val s=snap(line("left"),line("right",x=310f,y=35f,block=1));assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.DIFFERENT_TEXT_FLOW),PageTextSelector.adjust(s,p(s,1,1),TextAddress(0,0)))}
}
