// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class DocumentTextRepairTest {
    private fun g(cp:Int,x:Float,y:Float=20f,w:Float=10f,h:Float=10f,synthetic:Boolean=false,reliable:Boolean=true)=
        TextGlyph(cp,GlyphQuad(TextPoint(x,y),TextPoint(x+w,y),TextPoint(x,y+h),TextPoint(x+w,y+h)),reliable,synthetic)
    private fun line(vararg glyphs:TextGlyph)=PageTextLine(0,0,glyphs.toList())
    private fun quote(l:PageTextLine)=SpacingAccentPolicy.quotation(l,0,l.glyphs.lastIndex)
    private fun costs(synthetic:Boolean=true,gap:Float=0f,cp:Int=32)=line(
        g(99,0f),g(111,10f),g(117,20f),g(0x02C6,20f),
        g(cp,30f,w=maxOf(1f,gap),synthetic=synthetic),g(116,30f+gap),g(115,40f+gap))
    private fun snap(l:PageTextLine)=PageTextSnapshot(TextBounds(0f,0f,600f,800f),listOf(l),PageTextStatus.READY)
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    private fun selected(l:PageTextLine,a:Int,b:Int,words:Boolean):SelectionSpan {
        val r=PageTextSelector.select(snap(l),center(l.glyphs[a]),center(l.glyphs[b]),words)
        assertTrue("Expected selected: $r",r is TextSelectionAttempt.Selected)
        return (r as TextSelectionAttempt.Selected).span
    }
    @Test fun dotlessCircumflexIsComposed(){assertEquals("î",quote(line(g(0x0131,10f),g(0x02C6,10f))))}
    @Test fun dotlessBeforeAccentAlsoWorks(){assertEquals("î",quote(line(g(0x02C6,10f),g(0x0131,10f))))}
    @Test fun ordinaryDotlessIStaysUnchanged(){assertEquals("ı",quote(line(g(0x0131,10f))))}
    @Test fun dotlessAndDetachedAccentStayLiteral(){assertEquals("ıˆ",quote(line(g(0x0131,0f),g(0x02C6,20f))))}
    @Test fun explicitSpaceBeforeAccentStays(){assertEquals("ı ˆ",quote(line(g(0x0131,10f),g(32,20f),g(0x02C6,10f))))}
    @Test fun dotlessHandleKeepsClusterWhole(){val l=line(g(0x0131,10f),g(0x02C6,10f));val r=selected(l,0,0,false);assertEquals("î",r.quotation);assertEquals(2,r.glyphs.size)}
    @Test fun dotlessWordSelectionRemainsWhole(){val l=line(g(109,0f),g(97,10f),g(0x0131,20f),g(0x02C6,20f),g(116,30f));assertEquals("maît",selected(l,2,2,true).quotation)}
    @Test fun syntheticGapAfterAccentIsSuppressed(){assertEquals("coûts",quote(costs()))}
    @Test fun explicitSpaceAfterAccentIsPreserved(){assertEquals("coû ts",quote(costs(synthetic=false)))}
    @Test fun realWordGapIsPreservedEvenIfSynthetic(){assertEquals("coû ts",quote(costs(gap=4f)))}
    @Test fun nbspIsNeverSuppressed(){assertEquals("coû\u00A0ts",quote(costs(cp=0x00A0)))}
    @Test fun narrowNbspIsNeverSuppressed(){assertEquals("coû\u202Fts",quote(costs(cp=0x202F)))}
    @Test fun syntheticSpaceWithoutAccentIsPreserved(){assertEquals("a b",quote(line(g(97,0f),g(32,10f,synthetic=true),g(98,10f))))}
    @Test fun inferredBoundaryAfterNarrowAccentUsesBaseWidth(){val l=line(g(117,10f,w=10f),g(0x02C6,12f,w=5f),g(116,20f));assertEquals("ût",quote(l.copy(spacesBefore=setOf(2))))}
    @Test fun inferredTrueWordBoundaryRemains(){val l=line(g(117,10f,w=10f),g(0x02C6,12f,w=5f),g(116,24f));assertEquals("û t",quote(l.copy(spacesBefore=setOf(2))))}
    @Test fun wordSnapTraversesOnlySuppressedSyntheticSpace(){val l=costs();assertEquals("coûts",selected(l,0,0,true).quotation)}
    @Test fun rawVisualSpaceAndQuadsRemainAvailable(){val l=costs();val r=selected(l,0,6,false);assertEquals("couˆ ts",r.visualText);assertEquals(l.glyphs,r.glyphs);assertEquals(7,r.quads.size)}
    @Test fun manualContentsAreNotReinterpreted(){val manual="ıˆ and coû ts";assertEquals(manual,HighlightText.created("old").commitContentsEdit(manual).boundariesChanged("new").contents)}
    @Test fun knownBareFamilyMapsOnlyReviewedControl(){assertEquals(0x25CF,KnownSymbolPolicy.normalize(15,"AdvP4C4E74",true))}
    @Test fun knownSubsetFamilyMapsReviewedControl(){assertEquals(0x25CF,KnownSymbolPolicy.normalize(15,"QXMRQZ+AdvP4C4E74",true))}
    @Test fun unknownFontControlIsNotMapped(){assertEquals(15,KnownSymbolPolicy.normalize(15,"Helvetica",true))}
    @Test fun fontNameSuffixIsNotEnough(){assertEquals(15,KnownSymbolPolicy.normalize(15,"OtherAdvP4C4E74",true))}
    @Test fun otherControlInKnownFamilyIsNotMapped(){assertEquals(14,KnownSymbolPolicy.normalize(14,"AdvP4C4E74",true))}
    @Test fun unreliableIdIsNeverGuessed(){assertEquals(15,KnownSymbolPolicy.normalize(15,"AdvP4C4E74",false))}
    @Test fun missingFontNameDoesNotMapControl(){assertEquals(15,KnownSymbolPolicy.normalize(15,null,true))}
    @Test fun rawSourceCodePointIsRetained(){val raw=g(15,0f);val repaired=raw.copy(codePoint=0x25CF);assertEquals(15,repaired.sourceCodePoint);assertEquals(raw.quad,repaired.quad)}
    @Test fun reviewedBulletCanEnterSelection(){val raw=g(15,0f);val l=line(raw.copy(codePoint=KnownSymbolPolicy.normalize(15,"AdvP4C4E74",true)),g(32,10f),g(65,20f));assertEquals("● A",selected(l,0,2,false).quotation)}
    @Test fun unreviewedControlStillRejectsSelection(){val l=line(g(15,0f),g(65,10f));assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),PageTextSelector.select(snap(l),center(l.glyphs[0]),center(l.glyphs[1]),false))}
    @Test fun suppliedDotlessIGeometryReconstructs(){val l=line(g(0x0131,51.857f,333.967f,2.133f,7.591f),g(0x02C6,51.873f,334.028f,2.133f,7.590f),g(116,54.006f,334.028f,2.527f,7.590f));assertEquals("ît",quote(l))}
    @Test fun suppliedCostsGeometryIgnoresOnlyInsertedSpace(){val l=line(g(99,103.686f,333.967f,3.795f,7.591f),g(111,107.481f,333.967f,4.661f,7.591f),g(117,112.142f,333.967f,4.660f,7.591f),g(0x02C6,113.379f,334.028f,2.133f,7.590f),g(32,115.512f,333.967f,1.268f,7.591f,true),g(116,116.780f,333.967f,2.527f,7.591f),g(115,119.307f,333.967f,3.796f,7.591f));assertEquals("coûts",quote(l))}
}
