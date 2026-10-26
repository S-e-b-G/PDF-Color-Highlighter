// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class SpacingAccentPolicyTest {
    private fun g(cp:Int,x:Float,y:Float=20f,w:Float=10f,h:Float=10f,reliable:Boolean=true)=
        TextGlyph(cp,GlyphQuad(TextPoint(x,y),TextPoint(x+w,y),TextPoint(x,y+h),TextPoint(x+w,y+h)),reliable)
    private fun row(vararg glyphs:TextGlyph)=PageTextLine(0,0,glyphs.toList())
    private fun acute()=row(g(0x00B4,10f),g(101,10f))
    private fun snap(l:PageTextLine)=PageTextSnapshot(TextBounds(0f,0f,600f,800f),listOf(l),PageTextStatus.READY)
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    private fun quote(l:PageTextLine)=SpacingAccentPolicy.quotation(l,0,l.glyphs.lastIndex)
    private fun select(l:PageTextLine,a:Int=0,b:Int=l.glyphs.lastIndex,words:Boolean=false):SelectionSpan {
        val r=PageTextSelector.select(snap(l),center(l.glyphs[a]),center(l.glyphs[b]),words)
        assertTrue("Expected selection: $r",r is TextSelectionAttempt.Selected)
        return (r as TextSelectionAttempt.Selected).span
    }
    @Test fun precedingAcuteReconstructs(){assertEquals("é",quote(acute()))}
    @Test fun followingAcuteReconstructs(){assertEquals("é",quote(row(g(101,10f),g(0x00B4,10f))))}
    @Test fun graveReconstructs(){assertEquals("è",quote(row(g(96,10f),g(101,10f))))}
    @Test fun circumflexReconstructs(){assertEquals("ê",quote(row(g(94,10f),g(101,10f))))}
    @Test fun diaeresisReconstructs(){assertEquals("ï",quote(row(g(0x00A8,10f),g(105,10f))))}
    @Test fun cedillaReconstructs(){assertEquals("ç",quote(row(g(0x00B8,10f),g(99,10f))))}
    @Test fun modifierAcuteReconstructs(){assertEquals("é",quote(row(g(0x02CA,10f),g(101,10f))))}
    @Test fun uppercaseReconstructs(){assertEquals("É",quote(row(g(0x00B4,10f),g(69,10f))))}
    @Test fun nonOverlappingSpacingAccentRemains(){val l=row(g(0x00B4,0f),g(101,20f));assertTrue(l.spacingAccents.pairs.isEmpty());assertEquals("´e",quote(l))}
    @Test fun realWhitespaceNotConsumed(){val l=row(g(0x00B4,10f),g(32,10f),g(101,10f));assertEquals("´ e",quote(l));assertTrue(l.spacingAccents.pairs.isEmpty())}
    @Test fun ambiguousTwoBasesRemainUnchanged(){val l=row(g(97,10f),g(0x00B4,10f),g(101,10f));assertTrue(l.spacingAccents.pairs.isEmpty());assertEquals("a´e",quote(l))}
    @Test fun conflictingTwoAccentsRemainUnchanged(){val l=row(g(0x00B4,10f),g(101,10f),g(96,10f));assertTrue(l.spacingAccents.pairs.isEmpty())}
    @Test fun unmappedAccentIsNeverInterpreted(){val l=row(g(0x00B4,10f,reliable=false),g(101,10f));assertTrue(l.spacingAccents.pairs.isEmpty())}
    @Test fun unmappedBaseIsNeverInterpreted(){val l=row(g(0x00B4,10f),g(101,10f,reliable=false));assertTrue(l.spacingAccents.pairs.isEmpty())}
    @Test fun badVerticalAlignmentIsNotInterpreted(){val l=row(g(0x00B4,10f,40f),g(101,10f));assertTrue(l.spacingAccents.pairs.isEmpty())}
    @Test fun uncomposableBaseIsNotInterpreted(){val l=row(g(0x00B4,10f),g(113,10f));assertTrue(l.spacingAccents.pairs.isEmpty());assertEquals("´q",quote(l))}
    @Test fun zeroWidthAccentNotInterpreted(){val l=row(g(0x00B4,10f,w=0f),g(101,10f));assertTrue(l.spacingAccents.pairs.isEmpty())}
    @Test fun inferredInternalBoundaryIsNeutralized(){val l=acute().copy(spacesBefore=setOf(1));assertFalse(SpacingAccentPolicy.inferredSpaceBefore(l,1));assertEquals("é",quote(l))}
    @Test fun wordDragRemainsWhole(){val l=row(g(97,0f),g(0x00B4,10f),g(101,10f),g(98,20f));assertEquals("aéb",select(l,2,2,true).quotation)}
    @Test fun characterSelectionKeepsPairWhole(){val l=acute();val r=select(l,1,1);assertEquals("é",r.quotation);assertEquals(TextAddress(0,0),r.start);assertEquals(TextAddress(0,1),r.end)}
    @Test fun rawVisualAndNativeQuadsRemain(){val l=acute();val r=select(l);assertEquals("´e",r.visualText);assertEquals(l.glyphs,r.glyphs);assertEquals(l.glyphs.map{it.quad},r.quads)}
    @Test fun controlCodeIsIdentifiedNotMapped(){val l=row(g(7,0f),g(97,10f));val s=ExceptionalGlyphDiagnostics.describe(l);assertTrue(s.contains("U+0007(control)"));assertFalse(s.contains("bullet"))}
    @Test fun ordinaryTextAndFontIdsAreNotExposed(){val l=row(g(0x41,0f,reliable=false),g(101,10f));val s=ExceptionalGlyphDiagnostics.describe(l);assertTrue(s.contains("unmapped-ID-hidden"));assertFalse(s.contains("U+0041"));assertFalse(s.contains("Ae"))}
    @Test fun evidenceSamplesAreBounded(){val l=PageTextLine(0,0,(0..19).map{g(7,it*10f)});val s=ExceptionalGlyphDiagnostics.describe(l);assertEquals(8,s.split("U+0007").size-1);assertTrue(s.endsWith("+12"))}
}
