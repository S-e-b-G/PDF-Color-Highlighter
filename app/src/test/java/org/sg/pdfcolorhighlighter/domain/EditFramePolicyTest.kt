// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class EditFramePolicyTest{
    private val snapshot=PageTextSnapshot(TextBounds(0f,0f,100f,200f),emptyList(),PageTextStatus.EMPTY)
    private fun keep(a:Int=0,b:Int=0,w:Float=100f,nw:Float=100f,h:Float=200f,nh:Float=200f,copy:Boolean=true,s:PageTextSnapshot?=snapshot)=EditFramePolicy.retainText(a,b,w,nw,h,nh,copy,s)
    @Test fun editResolutionMatchesReaderFloor(){assertEquals(EditFramePolicy.READER_EDGE,EditFramePolicy.EDIT_EDGE)}
    @Test fun readerResolutionUnchanged(){assertEquals(1600,EditFramePolicy.READER_EDGE)}
    @Test fun previewBounded(){assertTrue(EditFramePolicy.EDIT_EDGE in 640..1600)}
    @Test fun samePageRetained(){assertTrue(keep())}
    @Test fun differentPageNotRetained(){assertFalse(keep(b=1))}
    @Test fun changedWidthNotRetained(){assertFalse(keep(nw=101f))}
    @Test fun changedHeightNotRetained(){assertFalse(keep(nh=201f))}
    @Test fun copyRestrictionNotRetained(){assertFalse(keep(copy=false))}
    @Test fun missingSnapshotNotRetained(){assertFalse(keep(s=null))}
    @Test fun snapshotUnchanged(){keep();assertEquals(PageTextStatus.EMPTY,snapshot.status);assertTrue(snapshot.lines.isEmpty())}
}
