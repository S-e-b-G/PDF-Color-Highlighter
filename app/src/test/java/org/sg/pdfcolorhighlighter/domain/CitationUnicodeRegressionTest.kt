// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class CitationUnicodeRegressionTest {
    private fun line(text:String,spaces:Set<Int> = emptySet()):PageTextLine =
        PageTextLine(0,0,text.codePoints().toArray().mapIndexed{i,cp->
            val x=10f+i*10f
            TextGlyph(cp,GlyphQuad(
                TextPoint(x,20f),TextPoint(x+10f,20f),
                TextPoint(x,30f),TextPoint(x+10f,30f)))
        },spacesBefore=spaces)

    private fun snap(l:PageTextLine)=PageTextSnapshot(
        TextBounds(0f,0f,1000f,800f),listOf(l),PageTextStatus.READY)

    private fun at(l:PageTextLine,i:Int):TextPoint {
        val b=l.glyphs[i].quad.bounds
        return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)
    }

    private fun select(
        l:PageTextLine,
        a:Int=0,
        b:Int=l.glyphs.lastIndex
    ):SelectionSpan {
        val r=PageTextSelector.select(snap(l),at(l,a),at(l,b),false)
        assertTrue("Expected selection: $r",r is TextSelectionAttempt.Selected)
        return (r as TextSelectionAttempt.Selected).span
    }

    private fun quote(s:String)=
        QuotationNormalizer.fromParagraphs(listOf(listOf(s)))

    @Test fun precomposedFrenchPreserved() {
        assertEquals("élève été cœur naïveté",quote("élève été cœur naïveté"))
    }

    @Test fun decomposedAcuteComposes() {
        assertEquals("été",quote("e\u0301te\u0301"))
    }

    @Test fun decomposedGraveComposes() {
        assertEquals("élève",quote("e\u0301le\u0300ve"))
    }

    @Test fun ligatureIsNotCompatibilityExpanded() {
        assertEquals("\uFB01 œ",quote("\uFB01 œ"))
    }

    @Test fun nonbreakingSpacesPreserved() {
        assertEquals("été\u00A0;\u202Foui",quote("e\u0301te\u0301\u00A0;\u202Foui"))
    }

    @Test fun spacingAccentsAreNotGuessed() {
        assertEquals("´ e ` a",quote("´ e ` a"))
    }

    @Test fun actualSpaceBeforeMarkIsNotRemoved() {
        assertEquals("e \u0301",quote("e \u0301"))
    }

    @Test fun inferredSpaceDoesNotDetachAccent() {
        assertEquals("e\u0301tude",line("e\u0301tude",setOf(1)).text)
        assertEquals("étude",select(line("e\u0301tude",setOf(1))).quotation)
    }

    @Test fun inferredWordBoundaryRemains() {
        assertEquals("ab cd",line("abcd",setOf(2)).text)
    }

    @Test fun rawVisualTextAndQuadsRemainNative() {
        val l=line("e\u0301",setOf(1))
        val r=select(l)
        assertEquals("e\u0301",r.visualText)
        assertEquals(l.glyphs,r.glyphs)
        assertEquals(l.glyphs.map{it.quad},r.quads)
    }

    @Test fun handleCannotDetachInferredAccent() {
        val l=line("e\u0301a",setOf(1))
        val r=PageTextSelector.adjust(snap(l),at(l,2),TextAddress(0,1))
        assertEquals("éa",(r as TextSelectionAttempt.Selected).span.quotation)
    }

    @Test fun wordSnapCrossesFalseAccentBoundary() {
        val l=line("e\u0301tude",setOf(1))
        val r=PageTextSelector.select(snap(l),at(l,2),at(l,3))
        assertEquals("étude",(r as TextSelectionAttempt.Selected).span.quotation)
    }

    @Test fun unmappedPrefixDoesNotPoisonBody() {
        val l=line("X abc")
        val g=l.glyphs.toMutableList()
        g[0]=g[0].copy(unicodeReliable=false)
        assertEquals("abc",select(l.copy(glyphs=g),2,4).quotation)
    }

    @Test fun includedUnmappedGlyphStillRejects() {
        val l=line("X abc")
        val g=l.glyphs.toMutableList()
        g[0]=g[0].copy(unicodeReliable=false)
        val s=l.copy(glyphs=g)
        assertEquals(
            TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),
            PageTextSelector.select(snap(s),at(s,0),at(s,4),false))
    }

    @Test fun unsupportedControlPrefixDoesNotPoisonBody() {
        assertEquals("abc",select(line("\u0001 abc"),2,4).quotation)
    }

    @Test fun mappedBlackCircleIsSelectable() {
        assertEquals("● été",select(line("● été")).quotation)
    }

    @Test fun blackCircleSeparatesItems() {
        val a=line("● A")
        val b=line("● B").copy(block=1)
        val s=snap(a).copy(lines=listOf(a,b))
        assertEquals(
            "● A\n● B",
            SelectionQuotationPolicy.compose(s,listOf(0,1),listOf(a.text,b.text)))
    }

    @Test fun diagnosticCountsCategoriesWithoutText() {
        val l=line("e\u0301 ´ PRIVATE",setOf(1))
        val g=l.glyphs.toMutableList()
        g[g.lastIndex]=g.last().copy(unicodeReliable=false)
        val r=SelectionLayoutDiagnostics.build(snap(l.copy(glyphs=g)),null)
        val row=r.lines.single()
        assertEquals(1,row.combiningMarks)
        assertEquals(1,row.spacingAccents)
        assertEquals(1,row.inferredClusterSplits)
        assertEquals(1,row.unreliableGlyphs)
        assertFalse(r.toString().contains("PRIVATE"))
        assertFalse(r.toString().contains("U+"))
    }

    @Test fun manualContentsRemainByteForByteUnnormalized() {
        val manual="e\u0301\ncorrection"
        val result=HighlightText.created("old")
            .commitContentsEdit(manual)
            .boundariesChanged("new")
        assertEquals(manual,result.contents)
    }

    @Test fun wholeRowZwjGuardRemains() {
        val l=line("A\u200DB C")
        assertEquals(
            TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),
            PageTextSelector.select(snap(l),at(l,4),at(l,4),false))
    }
}
