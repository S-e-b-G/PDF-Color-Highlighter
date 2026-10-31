// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class NativeReplacementBulletTest {
    private fun g(cp:Int,x:Float,y:Float=20f,source:Int=cp)=TextGlyph(cp,
        GlyphQuad(TextPoint(x,y),TextPoint(x+5f,y),TextPoint(x,y+6.41f),TextPoint(x+5f,y+6.41f)),
        true,false,source)
    private fun marker(body:String,y:Float,block:Int):PageTextLine {
        val cp=KnownSymbolPolicy.normalize(0xFFFD,"QXMRQZ+AdvP4C4E74",true,true)
        val glyphs=listOf(g(cp,48.59f,y,0xFFFD),g(32,53.59f,y))+
            body.mapIndexed{i,c->g(c.code,58.59f+i*5f,y)}
        return PageTextLine(block,0,glyphs)
    }
    private fun text(body:String,y:Float,block:Int)=PageTextLine(block,0,
        body.mapIndexed{i,c->g(c.code,48.59f+i*3f,y)})
    private fun snapshot()=PageTextSnapshot(TextBounds(0f,0f,595.28f,841.89f),listOf(
        marker("AFIS : Association",167.81f,3),text("https://example.invalid/afis",177.28f,3),
        marker("INCOSE : Council",190.09f,4),text("https://example.invalid/incose",199.61f,4)),PageTextStatus.READY)
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    @Test fun knownLeadingReplacementBecomesGenericBullet(){assertEquals(0x2022,KnownSymbolPolicy.normalize(0xFFFD,"AdvP4C4E74",true,true))}
    @Test fun knownSubsetNameSupportsNativeReplacement(){assertEquals(0x2022,KnownSymbolPolicy.normalize(0xFFFD,"QXMRQZ+AdvP4C4E74",true,true))}
    @Test fun ordinaryFontReplacementStillRefused(){assertEquals(0xFFFD,KnownSymbolPolicy.normalize(0xFFFD,"Helvetica",true,true))}
    @Test fun interiorReplacementIsNotReinterpreted(){assertEquals(0xFFFD,KnownSymbolPolicy.normalize(0xFFFD,"AdvP4C4E74",true,false))}
    @Test fun omittedPrefixEvidenceDoesNotPermitReplacement(){assertEquals(0xFFFD,KnownSymbolPolicy.normalize(0xFFFD,"AdvP4C4E74",true))}
    @Test fun unreliableFontIdStillUnchanged(){assertEquals(0xFFFD,KnownSymbolPolicy.normalize(0xFFFD,"AdvP4C4E74",false,true))}
    @Test fun nullOrSimilarFontNamesAreNotAccepted(){for(name in listOf(null,"OtherAdvP4C4E74","abcdef+AdvP4C4E74","AdvP4C4E74-Bold"))assertEquals(0xFFFD,KnownSymbolPolicy.normalize(0xFFFD,name,true,true))}
    @Test fun reviewedControlMappingRemainsFilledCircle(){assertEquals(0x25CF,KnownSymbolPolicy.normalize(15,"AdvP4C4E74",true))}
    @Test fun otherControlsAreNotEnabled(){for(cp in listOf(0,3,14,31,127))assertEquals(cp,KnownSymbolPolicy.normalize(cp,"AdvP4C4E74",true,true))}
    @Test fun nativeReplacementProvenanceAndQuadAreRetained(){val l=marker("AFIS",167.81f,3);val first=l.glyphs.first();assertEquals(0xFFFD,first.sourceCodePoint);assertEquals(0x2022,first.codePoint);assertEquals(48.59f,first.quad.bounds.left,0f);val r=SelectionLayoutDiagnostics.build(PageTextSnapshot(TextBounds(0f,0f,595f,842f),listOf(l),PageTextStatus.READY),null).lines.single();assertEquals(0,r.unsupportedGlyphs);assertEquals(1,r.bulletGlyphs)}
    @Test fun diagnosticGeometryCanSelectAcrossBothItems(){val s=snapshot();val a=center(s.lines[0].glyphs.first());val b=center(s.lines[3].glyphs.last());val r=PageTextSelector.select(s,a,b,false);assertTrue("Expected selection: $r",r is TextSelectionAttempt.Selected);val span=(r as TextSelectionAttempt.Selected).span;assertTrue(span.quotation.startsWith("• AFIS"));assertTrue(span.quotation.contains("\n• INCOSE"));assertFalse(span.quotation.contains("\uFFFD"));assertEquals(r,PageTextSelector.select(s,b,a,false))}
    @Test fun unreviewedInteriorReplacementStillRejectsRange(){val s=snapshot();val rows=s.lines.toMutableList();val target=rows[2];val glyphs=target.glyphs.toMutableList();glyphs[3]=glyphs[3].copy(codePoint=0xFFFD,sourceCodePoint=0xFFFD);rows[2]=target.copy(glyphs=glyphs);val altered=s.copy(lines=rows);assertEquals(TextSelectionAttempt.Rejected(SelectionRejection.UNSUPPORTED_TEXT),PageTextSelector.select(altered,center(rows[0].glyphs.first()),center(rows[3].glyphs.last()),false))}
}
