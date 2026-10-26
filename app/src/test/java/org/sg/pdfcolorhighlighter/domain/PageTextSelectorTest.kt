// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

private fun quad(x: Float, y: Float, w: Float = 10f, h: Float = 10f) =
    GlyphQuad(TextPoint(x,y), TextPoint(x+w,y), TextPoint(x,y+h), TextPoint(x+w,y+h))
private fun line(text: String, x: Float = 0f, y: Float = 0f, block: Int = 0, run: Int = 0, reliable: Boolean = true) =
    PageTextLine(block, run, text.codePoints().toArray().mapIndexed { index, cp -> TextGlyph(cp, quad(x+index*10f,y)) }, reliable)
private fun snapshot(vararg lines: PageTextLine) = PageTextSnapshot(TextBounds(0f,0f,1000f,1000f),lines.toList(),PageTextStatus.READY)
private fun center(glyph: TextGlyph) = TextPoint((glyph.quad.bounds.left+glyph.quad.bounds.right)/2,
    (glyph.quad.bounds.top+glyph.quad.bounds.bottom)/2)
private fun selected(s: PageTextSnapshot, a: TextAddress, b: TextAddress, words: Boolean = true): SelectionSpan {
    val r = PageTextSelector.select(s,center(s.lines[a.line].glyphs[a.glyph]),center(s.lines[b.line].glyphs[b.glyph]),words)
    assertTrue("Expected real selection, got $r",r is TextSelectionAttempt.Selected)
    return (r as TextSelectionAttempt.Selected).span
}

class PageTextSelectorTest {
    @Test fun wordSnappingExpandsBothEnds() {
        val s=snapshot(line("Un texte choisi")); val r=selected(s,TextAddress(0,4),TextAddress(0,6))
        assertEquals("texte",r.quotation); assertEquals(TextAddress(0,3),r.start); assertEquals(TextAddress(0,7),r.end)
    }
    @Test fun reverseDirectionGivesSameSelection() {
        val s=snapshot(line("Un texte choisi")); val a=TextAddress(0,4); val b=TextAddress(0,11)
        assertEquals(selected(s,a,b),selected(s,b,a))
    }
    @Test fun characterAdjustmentDoesNotSnapToWord() {
        val s=snapshot(line("texte")); assertEquals("x",selected(s,TextAddress(0,2),TextAddress(0,2),false).quotation)
    }
    @Test fun combiningMarkCannotBeSeparatedFromBase() {
        val s=snapshot(line("e\u0301tude"))
        assertEquals("é",selected(s,TextAddress(0,1),TextAddress(0,1),false).quotation)
    }
    @Test fun supplementaryCodePointRemainsWhole() {
        val s=snapshot(line("A\uD83D\uDE00B"))
        val r=selected(s,TextAddress(0,1),TextAddress(0,1),false)
        assertEquals(1,r.quotation.codePointCount(0,r.quotation.length)); assertEquals("\uD83D\uDE00",r.quotation)
    }
    @Test fun ligatureIsNotInventedAsDifferentText() {
        val s=snapshot(line("\uFB01n")); assertEquals("\uFB01n",selected(s,TextAddress(0,0),TextAddress(0,0)).quotation)
    }
    @Test fun multilineHasVisualBreakButContinuousQuotation() {
        val s=snapshot(line("Une phrase"),line("se poursuit",y=15f))
        val r=selected(s,TextAddress(0,0),TextAddress(1,10))
        assertEquals("Une phrase\nse poursuit",r.visualText); assertEquals("Une phrase se poursuit",r.quotation)
        assertEquals(21,r.glyphs.size)
    }
    @Test fun endpointWhitespaceIsTrimmed() {
        val s=snapshot(line("  texte  ")); assertEquals("texte",selected(s,TextAddress(0,0),TextAddress(0,8),false).quotation)
    }
    @Test fun whitespaceOnlyIsRejected() {
        val s=snapshot(line("   ")); val r=PageTextSelector.select(s,TextPoint(5f,5f),TextPoint(25f,5f))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.WHITESPACE),r)
    }
    @Test fun blankPageLocationIsNotSnappedToNearestWord() {
        val s=snapshot(line("texte")); assertNull(PageTextSelector.hit(s,TextPoint(500f,500f)))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.NO_GLYPH),PageTextSelector.select(s,TextPoint(500f,500f),TextPoint(5f,5f)))
    }
    @Test fun nativeBlocksAreConservativeSelectionBoundary() {
        val s=snapshot(line("gauche"),line("droite",x=300f,y=15f,block=1))
        val r=PageTextSelector.select(s,TextPoint(5f,5f),TextPoint(305f,20f))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.DIFFERENT_TEXT_FLOW),r)
    }
    @Test fun sameBlockCannotBridgeDisjointColumns() {
        val s=snapshot(line("gauche"),line("droite",x=300f,y=15f))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.DIFFERENT_TEXT_FLOW),
            PageTextSelector.select(s,TextPoint(5f,5f),TextPoint(305f,20f)))
    }
    @Test fun largeParagraphGapIsRejectedNotSilentlyJoined() {
        val s=snapshot(line("haut"),line("bas",y=100f))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.DIFFERENT_TEXT_FLOW),
            PageTextSelector.select(s,TextPoint(5f,5f),TextPoint(5f,105f)))
    }
    @Test fun internalNonbreakingSpaceIsPreserved() {
        val s=snapshot(line("un\u00A0texte")); assertEquals("un\u00A0texte",selected(s,TextAddress(0,0),TextAddress(0,7)).quotation)
    }
    @Test fun frenchApostropheBelongsToWord() {
        val s=snapshot(line("l’élève vient")); assertEquals("l’élève",selected(s,TextAddress(0,3),TextAddress(0,3)).quotation)
    }
    @Test fun restrictionAndComplexityDoNotReturnQuotations() {
        val s=snapshot(line("secret"))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.COPY_RESTRICTED),PageTextSelector.select(
            s.copy(status=PageTextStatus.COPY_RESTRICTED),TextPoint(5f,5f),TextPoint(15f,5f)))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.TOO_COMPLEX),PageTextSelector.select(
            s.copy(status=PageTextStatus.TOO_COMPLEX),TextPoint(5f,5f),TextPoint(15f,5f)))
    }
    @Test fun emptyAndUnsupportedTextAreExplicit() {
        val empty=PageTextSnapshot(TextBounds(0f,0f,10f,10f),emptyList(),PageTextStatus.EMPTY)
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.NO_TEXT),PageTextSelector.select(empty,TextPoint(5f,5f),TextPoint(5f,5f)))
        val rtl=snapshot(line("abc",reliable=false))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),PageTextSelector.select(rtl,TextPoint(5f,5f),TextPoint(15f,5f)))
    }
    @Test fun complexEmojiSequencesAreNotSplitSilently() {
        val s=snapshot(line("A\u200DB"))
        assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),PageTextSelector.select(s,TextPoint(5f,5f),TextPoint(25f,5f)))
    }
}
