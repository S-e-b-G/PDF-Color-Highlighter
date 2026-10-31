// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class SelectionPreviewQueueTest {
    private val snapshot=PageTextSnapshot(TextBounds(0f,0f,100f,100f),emptyList(),PageTextStatus.EMPTY)
    private val point=TextPoint(1f,1f)
    private fun offer(q:SelectionPreviewQueue,done:Boolean=false)=q.offer(snapshot,point,point,done)
    @Test fun firstRequestStartsImmediately() { val q=SelectionPreviewQueue();assertNotNull(offer(q)) }
    @Test fun secondRequestWaitsForRunningOne() { val q=SelectionPreviewQueue();offer(q);assertNull(offer(q)) }
    @Test fun onlyLatestPendingRequestSurvives() { val q=SelectionPreviewQueue();val first=offer(q)!!;repeat(100){offer(q)};val last=q.complete(first)!!;assertTrue(q.accepts(last));assertTrue(last.revision>first.revision+1) }
    @Test fun obsoleteResultIsRejected() { val q=SelectionPreviewQueue();val first=offer(q)!!;offer(q);assertFalse(q.accepts(first)) }
    @Test fun cancellationRejectsRunningAndDropsPending() { val q=SelectionPreviewQueue();val first=offer(q)!!;offer(q);q.cancel();assertFalse(q.accepts(first));assertNull(q.complete(first)) }
    @Test fun releaseEventIsNotLostBehindMoves() { val q=SelectionPreviewQueue();val first=offer(q)!!;offer(q);offer(q,true);assertTrue(q.complete(first)!!.finished) }
    @Test fun newGestureAfterCancelWaitsSafelyForOldWorker() { val q=SelectionPreviewQueue();val first=offer(q)!!;q.cancel();assertNull(offer(q,true));assertTrue(q.accepts(q.complete(first)!!)) }
    @Test fun wrongCompletionIsRejected() { val q=SelectionPreviewQueue();val first=offer(q)!!;try{q.complete(first.copy(revision=99));fail("Wrong completion accepted")}catch(_:IllegalStateException){} }
    @Test fun largeOverlayIsRefusedNotTruncated() {
        val g=TextGlyph(65,GlyphQuad(TextPoint(0f,0f),TextPoint(1f,0f),TextPoint(0f,1f),TextPoint(1f,1f)))
        fun span(n:Int)=SelectionSpan(TextAddress(0,0),TextAddress(0,0),"A","A",List(n){g})
        assertTrue(SelectionPreviewQueue.allows(span(25000)));assertFalse(SelectionPreviewQueue.allows(span(25001)))
    }
}
