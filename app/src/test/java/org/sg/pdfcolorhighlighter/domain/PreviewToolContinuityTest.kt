// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class PreviewToolContinuityTest{
    @Test fun pageNavigationKeepsEachColorSlot(){for(i in 0..5)assertEquals(i,PreviewContinuityPolicy.toolAfterPageChange(i,true))}
    @Test fun navigationWithoutColorStaysNavigation(){assertNull(PreviewContinuityPolicy.toolAfterPageChange(null,true))}
    @Test fun differentDocumentStartsNavigation(){for(i in 0..5)assertNull(PreviewContinuityPolicy.toolAfterPageChange(i,false))}
    @Test fun invalidToolCannotBecomeActive(){assertNull(PreviewContinuityPolicy.toolAfterPageChange(-1,true));assertNull(PreviewContinuityPolicy.toolAfterPageChange(6,true))}
}
