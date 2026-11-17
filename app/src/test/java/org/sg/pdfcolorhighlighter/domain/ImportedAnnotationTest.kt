// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class ImportedAnnotationTest{
    private val ref=ImportedAnnotationRef("external", "a".repeat(64))
    private val quad=GlyphQuad(TextPoint(1f,1f),TextPoint(10f,1f),TextPoint(1f,10f),TextPoint(10f,10f))
    private fun edit(text:String="",imported:Boolean=true)=HighlightEdit(ref.id(0,"Highlight"),0,0,text,listOf(quad),listOf(quad),0x123456,imported)
    @Test fun idIsDeterministic(){assertEquals(ref.id(0,"Highlight"),ref.copy(signature="b".repeat(64)).id(0,"Highlight"))}
    @Test fun pageSeparatesIdentity(){assertNotEquals(ref.id(0,"Highlight"),ref.id(1,"Highlight"))}
    @Test fun kindSeparatesIdentity(){assertNotEquals(ref.id(0,"Highlight"),ref.id(0,"Text"))}
    @Test fun blankIdentityRefused(){assertTrue(runCatching{ImportedAnnotationRef("", "a".repeat(64))}.isFailure)}
    @Test fun badSignatureRefused(){assertTrue(runCatching{ImportedAnnotationRef("external", "bad")}.isFailure)}
    @Test fun ownedEmptyQuotationStillRefused(){assertTrue(runCatching{edit(imported=false)}.isFailure)}
    @Test fun importedEmptyQuotationSupported(){assertEquals("",ManagedHighlight(edit(),"",TextMode.MANUAL,.37f,ref).contents)}
    @Test fun importedRequiresReference(){assertTrue(runCatching{ManagedHighlight(edit(),"",TextMode.MANUAL,.37f)}.isFailure)}
    @Test fun importedCannotPretendAutomatic(){assertTrue(runCatching{ManagedHighlight(edit(),"",TextMode.AUTOMATIC,.37f,ref)}.isFailure)}
    @Test fun ownedEmptyNoteStillRefused(){assertTrue(runCatching{ManagedNote(ref.id(0,"Text"),0,0,TextBounds(1f,1f,10f,10f),"")}.isFailure)}
    @Test fun importedEmptyNoteSupported(){assertEquals("",ManagedNote(ref.id(0,"Text"),0,0,TextBounds(1f,1f,10f,10f),"",imported=ref).contents)}
    @Test fun emptyTextGeometryHasNoHandles(){assertNull(ImportedHighlightGeometry.span(PageTextSnapshot(TextBounds(0f,0f,20f,20f),emptyList(),PageTextStatus.EMPTY),listOf(quad)))}
}
