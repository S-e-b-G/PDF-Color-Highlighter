// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LocalHighlightEditTest {
    private fun q()=GlyphQuad(TextPoint(0f,0f),TextPoint(10f,0f),TextPoint(0f,10f),TextPoint(10f,10f))
    private fun edit(text:String="text",count:Int=1)=HighlightEdit(UUID.randomUUID().toString(),0,0,text,List(count){q()})
    private fun rejects(block:()->Unit){try{block();fail("Expected rejection")}catch(_:Exception){}}
    @Test fun sameSourceIdentityIsStable(){assertEquals(LocalEditIdentity.key("content://test/A","a".repeat(64)),LocalEditIdentity.key("content://test/A","a".repeat(64)))}
    @Test fun differentUriIsNotGuessedFromContent(){assertNotEquals(LocalEditIdentity.key("content://test/A","a".repeat(64)),LocalEditIdentity.key("content://test/B","a".repeat(64)))}
    @Test fun changedSourceBytesHaveAnotherWorkspace(){assertNotEquals(LocalEditIdentity.key("content://test/A","a".repeat(64)),LocalEditIdentity.key("content://test/A","b".repeat(64)))}
    @Test fun identityIsSafeAndDoesNotContainUriText(){val key=LocalEditIdentity.key("content://PRIVATE/name","a".repeat(64));assertTrue(Regex("[0-9a-f]{64}-[0-9a-f]{64}").matches(key));assertFalse(key.contains("PRIVATE"))}
    @Test fun invalidSourceIdentityIsRejected(){rejects{LocalEditIdentity.key("","a".repeat(64))};rejects{LocalEditIdentity.key("uri","../bad")}}
    @Test fun creationAddsUndoNotRedo(){val h=LocalHighlightHistory();val a=edit();h.added(a);assertEquals(a,h.undo);assertNull(h.redo)}
    @Test fun undoThenRedoRetainsTheSameCommand(){val h=LocalHighlightHistory();val a=edit();h.added(a);h.didUndo(a);assertNull(h.undo);assertEquals(a,h.redo);h.didRedo(a);assertEquals(a,h.undo);assertNull(h.redo)}
    @Test fun multipleCommandsAreLifo(){val h=LocalHighlightHistory();val a=edit();val b=edit();h.added(a);h.added(b);h.didUndo(b);assertEquals(a,h.undo);h.didUndo(a);assertEquals(a,h.redo);h.didRedo(a);assertEquals(b,h.redo)}
    @Test fun newCreationInvalidatesRedoBranch(){val h=LocalHighlightHistory();val a=edit();h.added(a);h.didUndo(a);val b=edit();h.added(b);assertNull(h.redo);assertEquals(b,h.undo)}
    @Test fun duplicateAcknowledgementDoesNotDuplicateUndo(){val h=LocalHighlightHistory();val a=edit();h.added(a);h.added(a);h.didUndo(a);assertNull(h.undo)}
    @Test fun wrongUndoAndRedoAreRejected(){val h=LocalHighlightHistory();val a=edit();h.added(a);rejects{h.didUndo(edit())};rejects{h.didRedo(a)};assertEquals(a,h.undo)}
    @Test fun historyBudgetIncludesTextAndGeometry(){val h=LocalHighlightHistory(10);val a=edit("12345",2);val b=edit("abcdef",2);h.added(a);h.added(b);h.didUndo(b);assertNull(h.undo)}
    @Test fun oneLatestLargeCommandRemainsUndoable(){val h=LocalHighlightHistory(2);val a=edit("longer",5);h.added(a);assertEquals(a,h.undo);h.didUndo(a);assertEquals(a,h.redo)}
    @Test fun commandRetainsExactGeometryAndQuotation(){val a=edit("é\nnext");assertEquals("é\nnext",a.quotation);assertEquals(q(),a.quads.single())}
    @Test fun allSixSlotsAreValid(){val a=edit();for(slot in 0..5)assertEquals(slot,a.copy(slot=slot).slot)}
    @Test fun invalidSlotOrPageIsRejected(){val a=edit();rejects{a.copy(slot=-1)};rejects{a.copy(slot=6)};rejects{a.copy(page=-1)}}
    @Test fun invalidIdentityOrBlankContentsIsRejected(){val a=edit();rejects{a.copy(id="not-a-uuid")};rejects{a.copy(quotation="  ")}}
    @Test fun missingOrDegenerateGeometryIsRejected(){val a=edit();rejects{a.copy(quads=emptyList())};val zero=GlyphQuad(TextPoint(0f,0f),TextPoint(1f,0f),TextPoint(0f,0f),TextPoint(1f,0f));rejects{a.copy(quads=listOf(zero))}}
    @Test fun oversizedCommandsAreRejectedNotTruncated(){val a=edit();rejects{a.copy(quads=List(25001){q()})};rejects{a.copy(quotation="A".repeat(100001))}}
    @Test fun newSessionHistoryStartsEmpty(){val h=LocalHighlightHistory();h.added(edit());val fresh=LocalHighlightHistory();assertNull(fresh.undo);assertNull(fresh.redo)}
}
