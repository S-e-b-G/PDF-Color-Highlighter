// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class SelectionProgressFeedbackTest{
    private val snapshot=PageTextSnapshot(TextBounds(0f,0f,100f,100f),emptyList(),PageTextStatus.EMPTY)
    private val point=TextPoint(1f,1f)
    private fun offer(q:SelectionPreviewQueue,end:Boolean=false)=q.offer(snapshot,point,point,end)
    @Test fun inOrderMoveCanPublishWhileNewerMoveWaits(){val q=SelectionPreviewQueue();val first=offer(q)!!;offer(q);assertFalse(q.accepts(first));assertTrue(q.acceptsProgress(first));assertTrue(q.acceptsFeedback(first,true));assertFalse(q.acceptsFeedback(first,false))}
    @Test fun cancelBlocksOldProgress(){val q=SelectionPreviewQueue();val first=offer(q)!!;q.cancel();assertFalse(q.acceptsProgress(first))}
    @Test fun pendingReleaseBlocksObsoleteMove(){val q=SelectionPreviewQueue();val first=offer(q)!!;offer(q,true);assertFalse(q.acceptsProgress(first))}
    @Test fun finalRequestIsNeverTreatedAsIntermediate(){val q=SelectionPreviewQueue();val end=offer(q,true)!!;assertFalse(q.acceptsProgress(end));assertTrue(q.accepts(end))}
    @Test fun newEpochDoesNotReviveOldMove(){val q=SelectionPreviewQueue();val old=offer(q)!!;q.cancel();offer(q);assertFalse(q.acceptsProgress(old))}
    @Test fun completedWorkStartsOnlyLatestPendingMove(){val q=SelectionPreviewQueue();val first=offer(q)!!;repeat(100){offer(q)};val next=q.complete(first)!!;assertTrue(q.accepts(next));assertTrue(q.acceptsProgress(next))}
    @Test fun latestReleaseRemainsStrictlyAccepted(){val q=SelectionPreviewQueue();val first=offer(q)!!;offer(q);offer(q,true);val end=q.complete(first)!!;assertTrue(end.finished);assertTrue(q.accepts(end));assertFalse(q.acceptsProgress(end))}
    @Test fun olderFinalCannotPublishAfterNewInput(){val q=SelectionPreviewQueue();val end=offer(q,true)!!;offer(q);assertFalse(q.accepts(end));assertFalse(q.acceptsProgress(end))}
}
