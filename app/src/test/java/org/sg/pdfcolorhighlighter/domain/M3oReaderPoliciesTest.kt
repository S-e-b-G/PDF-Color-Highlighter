// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class M3oReaderPoliciesTest {
    @Test fun cleanLeaves(){assertEquals(ReaderExitDecision.LEAVE,ReaderExitPolicy.decide(false,false))}
    @Test fun dirtyConfirms(){assertEquals(ReaderExitDecision.CONFIRM,ReaderExitPolicy.decide(true,false))}
    @Test fun cleanBusyWaits(){assertEquals(ReaderExitDecision.WAIT,ReaderExitPolicy.decide(false,true))}
    @Test fun dirtyBusyWaits(){assertEquals(ReaderExitDecision.WAIT,ReaderExitPolicy.decide(true,true))}
    @Test fun resolutionFloor(){assertEquals(1600,EditFramePolicy.editEdge(960,680))}
    @Test fun widthPreserved(){assertEquals(3000,EditFramePolicy.editEdge(3000,2000))}
    @Test fun heightPreserved(){assertEquals(4096,EditFramePolicy.editEdge(2200,4096))}
    @Test fun zeroStillFloor(){assertEquals(1600,EditFramePolicy.editEdge(0,0))}
    @Test fun exactFloor(){assertEquals(1600,EditFramePolicy.editEdge(1000,1600))}
}
