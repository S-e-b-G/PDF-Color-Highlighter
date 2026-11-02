// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ManagedHighlightInteractionTest {
    private fun q(x:Float,y:Float=10f)=GlyphQuad(TextPoint(x,y),TextPoint(x+8,y),TextPoint(x,y+10),TextPoint(x+8,y+10))
    private fun snapshot()=PageTextSnapshot(TextBounds(0f,0f,100f,100f),listOf(PageTextLine(0,0,
        "abc def".mapIndexed{i,c->TextGlyph(c.code,q(i*8f))})),PageTextStatus.READY)
    private fun span():SelectionSpan {
        val s=snapshot()
        return (PageTextSelector.select(s,TextPoint(2f,15f),TextPoint(18f,15f),false) as TextSelectionAttempt.Selected).span
    }
    private fun item()=ManagedHighlight(HighlightEdit("00000000-0000-0000-0000-000000000001",0,0,"abc",span().quads),"abc",TextMode.AUTOMATIC)
    @Test fun hitUsesActualQuads(){assertEquals(1,ManagedHighlightSelection.hits(listOf(item()),TextPoint(2f,15f)).size)}
    @Test fun outsideDoesNotHit(){assertTrue(ManagedHighlightSelection.hits(listOf(item()),TextPoint(80f,80f)).isEmpty())}
    @Test fun overlappingReturnsBoth(){assertEquals(2,ManagedHighlightSelection.hits(listOf(item(),item().copy(edit=item().edit.copy(id="00000000-0000-0000-0000-000000000002"))),TextPoint(2f,15f)).size)}
    @Test fun resolvedRangeHasExactStart(){assertEquals(TextAddress(0,0),ManagedHighlightSelection.span(snapshot(),item())!!.start)}
    @Test fun resolvedRangeHasExactEnd(){assertEquals(TextAddress(0,2),ManagedHighlightSelection.span(snapshot(),item())!!.end)}
    @Test fun resolvedRangePreservesGlyphs(){assertEquals(span().glyphs,ManagedHighlightSelection.span(snapshot(),item())!!.glyphs)}
    @Test fun sparseRangeRefused(){val i=item();assertNull(ManagedHighlightSelection.span(snapshot(),i.copy(edit=i.edit.copy(quads=listOf(q(0f),q(16f))))))}
    @Test fun unrepresentedQuadRefused(){val i=item();assertNull(ManagedHighlightSelection.span(snapshot(),i.copy(edit=i.edit.copy(quads=i.edit.quads+q(90f)))))}

    @Test fun smallSerializationRoundingAccepted(){val i=item();val qs=i.edit.quads.map{q->GlyphQuad(q.ul.copy(x=q.ul.x+.001f),q.ur.copy(x=q.ur.x+.001f),q.ll.copy(x=q.ll.x+.001f),q.lr.copy(x=q.lr.x+.001f))};assertNotNull(ManagedHighlightSelection.span(snapshot(),i.copy(edit=i.edit.copy(quads=qs))))}
    @Test fun shiftedAnnotationRefused(){val i=item();val qs=i.edit.quads.map{q->GlyphQuad(q.ul.copy(x=q.ul.x+1f),q.ur.copy(x=q.ur.x+1f),q.ll.copy(x=q.ll.x+1f),q.lr.copy(x=q.lr.x+1f))};assertNull(ManagedHighlightSelection.span(snapshot(),i.copy(edit=i.edit.copy(quads=qs))))}
    @Test fun absentTextRefused(){assertNull(ManagedHighlightSelection.span(snapshot().copy(lines=emptyList(),status=PageTextStatus.EMPTY),item()))}
    @Test fun automaticResizeUpdatesContents(){val i=item().copy(contents="old");assertEquals("abc",i.resized(span()).contents)}
    @Test fun manualResizePreservesContents(){val i=item().copy(contents="e\u0301\ncorrected",mode=TextMode.MANUAL);assertEquals(i.contents,i.resized(span()).contents)}
    @Test fun resizeKeepsIdentity(){assertEquals(item().edit.id,item().resized(span()).edit.id)}
    @Test fun resizeKeepsColor(){assertEquals(0,item().resized(span()).edit.slot)}
    @Test fun recolorKeepsContentsAndGeometry(){val i=item();val n=i.copy(edit=i.edit.copy(slot=3));assertEquals(i.contents,n.contents);assertEquals(i.edit.quads,n.edit.quads)}
    private fun history(action:(CheckpointHistory,File,File,File)->Unit){
        val root=Files.createTempDirectory("checkpoint-history").toFile()
        try{val a=File(root,"a").apply{writeText("a")};val b=File(root,"b").apply{writeText("b")};val c=File(root,"c").apply{writeText("c")};action(CheckpointHistory(),a,b,c)}finally{root.deleteRecursively()}
    }
    @Test fun emptyHistory(){val h=CheckpointHistory();assertNull(h.undo);assertNull(h.redo);assertTrue(h.pins.isEmpty())}
    @Test fun addPinsBothSnapshots(){history{h,a,b,_->h.added(CheckpointChange(a,b));assertEquals(setOf(a.name,b.name),h.pins)}}
    @Test fun undoMovesToRedo(){history{h,a,b,_->val c=CheckpointChange(a,b);h.added(c);h.didUndo(c);assertNull(h.undo);assertEquals(c,h.redo)}}
    @Test fun redoRestoresCommand(){history{h,a,b,_->val c=CheckpointChange(a,b);h.added(c);h.didUndo(c);h.didRedo(c);assertEquals(c,h.undo);assertNull(h.redo)}}
    @Test fun newEditInvalidatesRedo(){history{h,a,b,c->val x=CheckpointChange(a,b);h.added(x);h.didUndo(x);h.added(CheckpointChange(a,c));assertNull(h.redo);assertFalse(b.name in h.pins)}}
    @Test fun undoRetainsRequiredPins(){history{h,a,b,c->val x=CheckpointChange(a,b);val y=CheckpointChange(b,c);h.added(x);h.added(y);h.didUndo(y);assertEquals(setOf(a.name,b.name,c.name),h.pins)}}
    @Test fun countBoundKeepsLatest(){history{_,a,b,c->val h=CheckpointHistory(1);h.added(CheckpointChange(a,b));h.added(CheckpointChange(b,c));assertEquals(setOf(b.name,c.name),h.pins)}}
    @Test fun byteBudgetKeepsLatestCommand(){history{_,a,b,c->val h=CheckpointHistory(bytes=1);h.added(CheckpointChange(a,b));h.added(CheckpointChange(b,c));assertEquals(CheckpointChange(b,c),h.undo)}}
    @Test fun invalidHistoryBoundRejected(){try{CheckpointHistory(0);fail()}catch(_:IllegalArgumentException){}}
    @Test fun outOfOrderUndoRejected(){history{h,a,b,c->h.added(CheckpointChange(a,b));try{h.didUndo(CheckpointChange(b,c));fail()}catch(_:IllegalStateException){}}}
}
