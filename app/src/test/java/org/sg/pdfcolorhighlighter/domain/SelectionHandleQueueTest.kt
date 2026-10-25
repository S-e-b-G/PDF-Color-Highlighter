// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class SelectionHandleQueueTest{
    private val s=PageTextSnapshot(TextBounds(0f,0f,10f,10f),emptyList(),PageTextStatus.EMPTY)
    private val p=TextPoint(1f,1f);private val fixed=TextAddress(2,4)
    @Test fun ordinaryDragKeepsDefaultWordMode(){val r=SelectionPreviewQueue().offer(s,p,p,false)!!;assertTrue(r.snapToWords);assertNull(r.fixedAddress)}
    @Test fun adjustmentCarriesExactFixedEndpoint(){val r=SelectionPreviewQueue().offer(s,p,p,false,false,fixed)!!;assertFalse(r.snapToWords);assertEquals(fixed,r.fixedAddress)}
    @Test fun latestHandleReleaseKeepsItsModeAndAddress(){val q=SelectionPreviewQueue();val r=q.offer(s,p,p,false)!!;q.offer(s,p,p,true,false,fixed);val last=q.complete(r)!!;assertTrue(last.finished);assertFalse(last.snapToWords);assertEquals(fixed,last.fixedAddress)}
    @Test fun cancelInvalidatesHandleProgress(){val q=SelectionPreviewQueue();val r=q.offer(s,p,p,false,false,fixed)!!;q.cancel();assertFalse(q.acceptsFeedback(r,true))}
}
