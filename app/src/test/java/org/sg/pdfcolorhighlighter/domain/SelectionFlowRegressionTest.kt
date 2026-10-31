// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class SelectionFlowRegressionTest {
    private fun line(text:String,x:Float=20f,y:Float=20f,block:Int=0,run:Int=0,reliable:Boolean=true):PageTextLine {
        val glyphs=text.codePoints().toArray().mapIndexed { index,cp ->
            val left=x+index*10f
            TextGlyph(cp,GlyphQuad(TextPoint(left,y),TextPoint(left+10f,y),TextPoint(left,y+10f),TextPoint(left+10f,y+10f)))
        }
        return PageTextLine(block,run,glyphs,reliable)
    }
    private fun snap(vararg lines:PageTextLine)=PageTextSnapshot(TextBounds(0f,0f,600f,800f),lines.toList(),PageTextStatus.READY)
    private fun select(s:PageTextSnapshot,a:TextPoint,b:TextPoint):SelectionSpan {
        val result=PageTextSelector.select(s,a,b)
        assertTrue("Expected selection, got $result",result is TextSelectionAttempt.Selected)
        return (result as TextSelectionAttempt.Selected).span
    }
    private fun rejected(s:PageTextSnapshot,a:TextPoint,b:TextPoint,reason:SelectionRejection) {
        assertEquals(TextSelectionAttempt.Rejected(reason),PageTextSelector.select(s,a,b))
    }
    @Test fun beginsBeforeFirstWord() { assertEquals("alpha",select(snap(line("alpha beta")),TextPoint(2f,25f),TextPoint(45f,25f)).quotation) }
    @Test fun endsAfterLastWord() { assertEquals("beta",select(snap(line("alpha beta")),TextPoint(95f,25f),TextPoint(200f,25f)).quotation) }
    @Test fun bothMarginsSelectWholeLine() { assertEquals("alpha beta",select(snap(line("alpha beta")),TextPoint(1f,25f),TextPoint(590f,25f)).quotation) }
    @Test fun reverseMarginSelectionIsIdentical() { val s=snap(line("alpha beta"));val a=TextPoint(1f,25f);val b=TextPoint(590f,25f);assertEquals(select(s,a,b),select(s,b,a)) }
    @Test fun smallGlyphGapIsAnEndpointNotFabricatedSpace() {
        val a=line("A").glyphs.single();val b=line("B",x=40f).glyphs.single()
        val s=snap(PageTextLine(0,0,listOf(a,b)))
        assertEquals("AB",select(s,TextPoint(33f,25f),TextPoint(45f,25f)).quotation)
    }
    @Test fun verticalBlankPageStillDoesNotSnap() { rejected(snap(line("text")),TextPoint(25f,500f),TextPoint(25f,25f),SelectionRejection.NO_GLYPH) }
    @Test fun outsideSheetDoesNotSnap() { rejected(snap(line("text")),TextPoint(-10f,25f),TextPoint(25f,25f),SelectionRejection.NO_GLYPH) }
    @Test fun exactColumnGutterIsAmbiguous() {
        val s=snap(line("LEFT",x=20f),line("RIGHT",x=300f,block=1))
        assertNull(TextFlowResolver.endpoint(s,TextPoint(180f,25f)))
    }
    @Test fun leftMarginStaysInLeftColumn() {
        val s=snap(line("LEFT",x=20f),line("RIGHT",x=300f,block=1))
        assertEquals("LEFT",select(s,TextPoint(1f,25f),TextPoint(45f,25f)).quotation)
    }
    @Test fun gutterNearLeftLineEndsLeftLine() {
        val s=snap(line("LEFT",x=20f),line("RIGHT",x=300f,block=1))
        assertEquals("LEFT",select(s,TextPoint(25f,25f),TextPoint(100f,25f)).quotation)
    }
    @Test fun cannotCrossTwoColumnsOnSameRow() {
        val s=snap(line("LEFT",x=20f),line("RIGHT",x=300f,block=1))
        rejected(s,TextPoint(25f,25f),TextPoint(345f,25f),SelectionRejection.DIFFERENT_TEXT_FLOW)
    }
    @Test fun multilineSameNativeBlock() {
        val s=snap(line("first line"),line("second line",y=35f))
        assertEquals("first line second line",select(s,TextPoint(25f,25f),TextPoint(125f,40f)).quotation)
    }
    @Test fun multilineAcrossNativeBlocks() {
        val s=snap(line("first line",block=7),line("second line",y=35f,block=9))
        assertEquals("first line second line",select(s,TextPoint(25f,25f),TextPoint(125f,40f)).quotation)
    }
    @Test fun reverseAcrossBlocksIsIdentical() {
        val s=snap(line("first line",block=7),line("second line",y=35f,block=9));val a=TextPoint(25f,25f);val b=TextPoint(125f,40f)
        assertEquals(select(s,a,b),select(s,b,a))
    }
    @Test fun threeLinesAcrossBlocksAndMargins() {
        val s=snap(line("first",block=1),line("second",y=35f,block=2),line("third",y=50f,block=3))
        assertEquals("first second third",select(s,TextPoint(1f,25f),TextPoint(150f,55f)).quotation)
    }
    @Test fun sourceArrayMayInterleaveAnotherColumn() {
        val s=snap(line("left one",block=1),line("right one",x=300f,block=2),
            line("left two",y=35f,block=3),line("right two",x=300f,y=35f,block=4))
        assertEquals("left one left two",select(s,TextPoint(25f,25f),TextPoint(95f,40f)).quotation)
    }
    @Test fun diagonalCrossColumnRangeIsRejected() {
        val s=snap(line("left one",block=1),line("right two",x=300f,y=35f,block=2))
        rejected(s,TextPoint(25f,25f),TextPoint(305f,40f),SelectionRejection.DIFFERENT_TEXT_FLOW)
    }
    @Test fun largeParagraphGapIsNotASelectionBoundary() {
        val s=snap(line("top",block=1),line("bottom",y=100f,block=2))
        val q=select(s,TextPoint(25f,25f),TextPoint(25f,105f)).quotation
        assertTrue(q.contains("top") && q.contains("bottom"))
    }
    @Test fun shortContinuationAcrossBlocksIsAllowed() {
        val s=snap(line("A short line",block=1),line("yes",y=35f,block=2))
        assertEquals("A short line yes",select(s,TextPoint(25f,25f),TextPoint(45f,40f)).quotation)
    }
    @Test fun raggedLineWidthsDoNotRequireSameBlock() {
        val s=snap(line("very long first line",block=1),line("short",y=35f,block=2))
        assertEquals("very long first line short",select(s,TextPoint(25f,25f),TextPoint(60f,40f)).quotation)
    }
    @Test fun wideHeaderUsesTheExplicitEndColumn() {
        val s=snap(line("A very wide header spanning columns",block=1),line("left",y=35f,block=2),
            line("right",x=250f,y=35f,block=3),line("end",y=50f,block=4))
        val q=select(s,TextPoint(25f,25f),TextPoint(25f,55f)).quotation
        assertTrue(q.contains("header") && q.contains("left") && q.contains("end"))
        assertFalse(q.contains("right"))
    }
    @Test fun unsupportedIntermediateLineIsNotSkipped() {
        val s=snap(line("first",block=1),line("unknown",y=35f,block=2,reliable=false),line("third",y=50f,block=3))
        rejected(s,TextPoint(25f,25f),TextPoint(25f,55f),SelectionRejection.UNSUPPORTED_TEXT)
    }
    @Test fun visualLineBreaksRemainSeparateFromQuotation() {
        val s=snap(line("bonjour",block=1),line("monde",y=35f,block=2))
        val selected=select(s,TextPoint(25f,25f),TextPoint(65f,40f))
        assertEquals("bonjour\nmonde",selected.visualText);assertEquals("bonjour monde",selected.quotation)
    }
    @Test fun nearLineMarginUsesNearestRow() {
        val s=snap(line("first",block=1),line("second",y=35f,block=2))
        assertEquals(TextAddress(0,0),TextFlowResolver.endpoint(s,TextPoint(1f,31f)))
    }
}
