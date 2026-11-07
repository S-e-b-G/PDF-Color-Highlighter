// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class ReaderEditPoliciesTest {
    private fun box(x:Float=200f,y:Float=200f,w:Float=400f,h:Float=600f,d:Float=1f)=EndpointLensGeometry.window(TextPoint(x,y),w,h,d)
    @Test fun missingCurrentNoPrompt(){assertFalse(SavedContentPolicy.needsSave(null,null))}
    @Test fun unknownSavedNeedsPrompt(){assertTrue(SavedContentPolicy.needsSave("a",null))}
    @Test fun sameBytesNoPrompt(){assertFalse(SavedContentPolicy.needsSave("a","a"))}
    @Test fun changedBytesPrompt(){assertTrue(SavedContentPolicy.needsSave("b","a"))}
    @Test fun undoToSavedNoPrompt(){assertFalse(SavedContentPolicy.needsSave("saved","saved"))}
    @Test fun redoAwayFromSavedPrompt(){assertTrue(SavedContentPolicy.needsSave("redo","saved"))}
    @Test fun absentPageNoPrompt(){assertFalse(SavedContentPolicy.needsSave(null,"saved"))}
    @Test fun lensZoomIsEnlarged(){assertTrue(EndpointLensGeometry.ZOOM>1f)}
    @Test fun lensNormallyAbove(){assertTrue(checkNotNull(box()).bottom<200f)}
    @Test fun lensFallsBelowAtTop(){assertTrue(checkNotNull(box(y=0f)).top>0f)}
    @Test fun lensClampsLeft(){assertTrue(checkNotNull(box(x=0f)).left>=0f)}
    @Test fun lensClampsRight(){assertTrue(checkNotNull(box(x=400f)).right<=400f)}
    @Test fun lensClampsBottom(){assertTrue(checkNotNull(box(y=600f)).bottom<=600f)}
    @Test fun tinyViewportNoLens(){assertNull(box(w=12f,h=12f))}
    @Test fun zeroDensityNoLens(){assertNull(box(d=0f))}
    @Test fun negativeSizeNoLens(){assertNull(box(w=-1f))}
    @Test fun nonfiniteSizeNoLens(){assertNull(box(w=Float.NaN))}
    @Test fun nonfiniteDensityNoLens(){assertNull(box(d=Float.POSITIVE_INFINITY))}
    @Test fun densityScalesWindow(){assertEquals(352f,checkNotNull(box(w=800f,h=1200f,d=2f)).width,0.001f)}
    @Test fun lensNeverChangesEndpoint(){val p=TextPoint(0f,0f);EndpointLensGeometry.window(p,400f,600f,1f);assertEquals(TextPoint(0f,0f),p)}
}
