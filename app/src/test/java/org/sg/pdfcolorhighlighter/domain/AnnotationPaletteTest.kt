// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AnnotationPaletteTest {
    private val q=GlyphQuad(TextPoint(1f,1f),TextPoint(12f,1f),TextPoint(1f,12f),TextPoint(12f,12f))
    private fun h()=HighlightEdit(UUID.randomUUID().toString(),0,0,"Text",listOf(q))
    private fun n()=ManagedNote(UUID.randomUUID().toString(),0,0,TextBounds(0f,0f,12f,12f),"Note")
    @Test fun defaultsHaveSixStableColors(){assertEquals(listOf(0xFFAD5B,0x00FF00,0xFC3636,0xFFFF00,0xD1DFEB,0xFCD7FB),AnnotationPalette.DEFAULT.colors)}
    @Test fun parseUppercase(){assertEquals(0xABCDEF,AnnotationPalette.parse("ABCDEF"))}
    @Test fun parseLowercase(){assertEquals(0xabcdef,AnnotationPalette.parse("abcdef"))}
    @Test fun parsePrefixAndWhitespace(){assertEquals(0x123456,AnnotationPalette.parse(" #123456 "))}
    @Test fun parseBlack(){assertEquals(0,AnnotationPalette.parse("000000"))}
    @Test fun parseWhite(){assertEquals(0xffffff,AnnotationPalette.parse("ffffff"))}
    @Test fun rejectShort(){assertNull(AnnotationPalette.parse("FFF"))}
    @Test fun rejectLong(){assertNull(AnnotationPalette.parse("FF123456"))}
    @Test fun rejectNonHex(){assertNull(AnnotationPalette.parse("gg1234"))}
    @Test fun rejectEmbeddedWhitespace(){assertNull(AnnotationPalette.parse("FF 123"))}
    @Test fun duplicateDraftRejected(){assertNull(AnnotationPalette.draft(List(6){"123456"}))}
    @Test fun incompleteDraftRejected(){assertNull(AnnotationPalette.draft(listOf("123456")))}
    @Test fun normalizeHex(){assertEquals("0000AB",AnnotationPalette.hex(0xab))}
    @Test fun codecRoundTrip(){val p=AnnotationPalette.DEFAULT;assertEquals(p,AnnotationPalette.decode(AnnotationPalette.encode(p)))}
    @Test fun customCodecRoundTrip(){val p=AnnotationPalette(listOf(1,2,3,4,5,6));assertEquals(p,AnnotationPalette.decode(AnnotationPalette.encode(p)))}
    @Test fun corruptCodecRejected(){assertTrue(runCatching{AnnotationPalette.decode("INVALID".toByteArray())}.isFailure)}
    @Test fun exactMatchOnly(){val p=AnnotationPalette.DEFAULT;assertNull(p.match(p[0]-1));assertEquals(0,p.match(p[0]))}
    @Test fun highlightColorSurvivesOtherEdits(){val x=h().copy(rgb=0x123456);assertEquals(x.color,x.copy(quotation="Other").color)}
    @Test fun noteColorSurvivesContents(){val x=n().copy(rgb=0x654321);assertEquals(x.color,x.copy(contents="Other").color)}
    @Test fun canonicalDefaultsKeepLegacyCopies(){val x=h();assertNull(AnnotationPalette.storedColor(x.color,0));assertEquals(AnnotationPalette.DEFAULT[1],x.copy(slot=1).color);assertEquals(AnnotationPalette.DEFAULT[1],n().copy(slot=1).color)}
}
