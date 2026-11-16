// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.*

class AutomaticFallbackTest {
    private val stamp="2026-11-15_17-34-00"
    private fun reject(f:()->Unit){var bad=false;try{f()}catch(_:Exception){bad=true};assertTrue(bad)}
    private class Target:FallbackTarget{
        var access=true;var made=0;var writes=0;var bytes=byteArrayOf();var alias=false;var lose=false;var corrupt=false;var fail=false
        override fun authorized()=access
        override fun createNew(baseName:String,original:String):String {made++;if(lose)access=false;return if(alias)original else "new"}
        override fun read(id:String):InputStream=ByteArrayInputStream(if(corrupt && bytes.isNotEmpty())byteArrayOf(1)else bytes)
        override fun write(id:String):OutputStream {writes++;if(fail)throw IOException("Synthetic");return object:ByteArrayOutputStream(){override fun close(){bytes=toByteArray();super.close()}}}
    }
    private fun copy(t:Target):VerifiedDocumentExport.Receipt {val f=File.createTempFile("fallback-unit-",".pdf");try{f.writeBytes("%PDF synthetic".toByteArray());return AutomaticFallback.copy(f,"content://source/pdf","sample.pdf",stamp,t)}finally{f.delete()}}
    @Test fun requestedLocalFilename(){assertEquals("sample_$stamp.pdf",AutomaticFallback.baseName("sample.pdf",stamp))}
    @Test fun pathIsRemoved(){assertEquals("a_$stamp.pdf",AutomaticFallback.baseName("C:\\folder\\a.PDF",stamp))}
    @Test fun unsafeNameIsSanitized(){assertFalse(AutomaticFallback.baseName("a:b?.pdf",stamp).contains(":"))}
    @Test fun missingNameHasDefault(){assertEquals("document_$stamp.pdf",AutomaticFallback.baseName(null,stamp))}
    @Test fun leadingDotsAreRemoved(){assertFalse(AutomaticFallback.baseName("...",stamp).startsWith("."))}
    @Test fun invalidStampRejects(){reject{AutomaticFallback.baseName("a","now")}}
    @Test fun freeNameUnchanged(){assertEquals("a.pdf",AutomaticFallback.freshName("a.pdf",emptySet()))}
    @Test fun collisionsGetSuffix(){assertEquals("a_3.pdf",AutomaticFallback.freshName("a.pdf",setOf("a.pdf","a_2.pdf")))}
    @Test fun disabledPreferenceRoundTrip(){assertNull(AutomaticFallback.decode(AutomaticFallback.encode(null)))}
    @Test fun preferenceRoundTrip(){val u="content://provider/tree/folder";assertEquals(u,AutomaticFallback.decode(AutomaticFallback.encode(u)))}
    @Test fun checksumRejectsCorruption(){val x=AutomaticFallback.encode(null);x[0]=1;reject{AutomaticFallback.decode(x)}}
    @Test fun truncatedPreferenceRejects(){reject{AutomaticFallback.decode(byteArrayOf(1))}}
    @Test fun fileUriRejects(){reject{AutomaticFallback.encode("file:///tmp")}}
    @Test fun oversizedPreferenceRejects(){reject{AutomaticFallback.encode("content://"+"a".repeat(5000))}}
    @Test fun controlPreferenceRejects(){reject{AutomaticFallback.encode("content://a\n")}}
    @Test fun brokenUnicodeRejects(){reject{AutomaticFallback.encode("content://\uD800")}}
    @Test fun copyChecksBytes(){val t=Target();val r=copy(t);assertEquals(1,t.made);assertEquals(1,t.writes);assertEquals(t.bytes.size.toLong(),r.sizeBytes);assertEquals(64,r.sha256.length)}
    @Test fun noAuthorizationDoesNothing(){val t=Target().apply{access=false};reject{copy(t)};assertEquals(0,t.made)}
    @Test fun lostAuthorizationDoesNotWrite(){val t=Target().apply{lose=true};reject{copy(t)};assertEquals(0,t.writes)}
    @Test fun nonemptyTargetDoesNotWrite(){val t=Target().apply{bytes=byteArrayOf(7)};reject{copy(t)};assertEquals(0,t.writes);assertArrayEquals(byteArrayOf(7),t.bytes)}
    @Test fun sourceAliasDoesNotWrite(){val t=Target().apply{alias=true};reject{copy(t)};assertEquals(0,t.writes)}
    @Test fun outputFailureIsFailure(){reject{copy(Target().apply{fail=true})}}
    @Test fun badReadbackIsFailure(){val t=Target().apply{corrupt=true};reject{copy(t)};assertTrue(t.bytes.isNotEmpty())}
    @Test fun missingSourceCreatesNothing(){val t=Target();reject{AutomaticFallback.copy(File("missing-fallback-unit-source"),"content://source/pdf","a",stamp,t)};assertEquals(0,t.made)}

    private fun managed(opacity:Float):ManagedHighlight {
        val q=GlyphQuad(TextPoint(0f,0f),TextPoint(10f,0f),TextPoint(0f,10f),TextPoint(10f,10f))
        return ManagedHighlight(HighlightEdit("11111111-1111-1111-1111-111111111111",0,1,"A",listOf(q)),"A",TextMode.MANUAL,opacity)
    }
    @Test fun recolorKeepsOpacity(){val h=managed(.35f);assertEquals(.35f,h.copy(edit=h.edit.copy(slot=2)).opacity,.000001f)}
    @Test fun contentsKeepsOpacity(){assertEquals(.35f,managed(.35f).copy(contents="Manual").opacity,.000001f)}
    @Test fun boundaryEditKeepsOpacity(){val h=managed(.35f);val q=h.edit.quads.single();val span=SelectionSpan(TextAddress(0,0),TextAddress(0,0),"A","A",listOf(TextGlyph(65,q)));assertEquals(.35f,h.resized(span).opacity,.000001f)}
    @Test fun invalidOpacityRejects(){reject{managed(Float.NaN)};reject{managed(-.1f)};reject{managed(1.1f)}}
}
