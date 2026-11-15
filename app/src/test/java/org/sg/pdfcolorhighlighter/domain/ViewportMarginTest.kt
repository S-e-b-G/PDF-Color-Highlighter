// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class ViewportMarginTest {
    private fun zoomed()=ViewportGeometry().apply{configure(1000f,1000f,800f,600f);zoomBy(3f,400f,300f)}
    @Test fun leftEdgeCanReachHorizontalMiddle(){val v=zoomed();v.pan(1e6f,0f);assertEquals(400f,v.placement.left,.001f)}
    @Test fun rightEdgeCanReachHorizontalMiddle(){val v=zoomed();v.pan(-1e6f,0f);assertEquals(400f,v.placement.left+v.placement.width,.001f)}
    @Test fun topEdgeCanReachVerticalMiddle(){val v=zoomed();v.pan(0f,1e6f);assertEquals(300f,v.placement.top,.001f)}
    @Test fun bottomEdgeCanReachVerticalMiddle(){val v=zoomed();v.pan(0f,-1e6f);assertEquals(300f,v.placement.top+v.placement.height,.001f)}
    @Test fun cornerKeepsHalfViewportOnBothAxes(){val v=zoomed();v.pan(1e6f,1e6f);assertEquals(400f,v.placement.left,.001f);assertEquals(300f,v.placement.top,.001f)}
    @Test fun repeatedPansCannotLoseLargePage(){val v=zoomed();repeat(50){v.pan(1e6f,-1e6f)};val p=v.placement;assertTrue(p.left<=400.001f && p.left+p.width>=399.999f);assertTrue(p.top<=300.001f && p.top+p.height>=299.999f)}
    @Test fun fitPageResetsExtraTravel(){val v=zoomed();v.pan(1e6f,-1e6f);v.fit();assertEquals(PageViewportState(),v.state)}
    @Test fun originalFitModeDoesNotGainOverscroll(){val v=ViewportGeometry().apply{configure(1000f,1000f,800f,600f)};v.pan(1e6f,1e6f);assertEquals(0f,v.placement.top,.001f);assertTrue(v.placement.left>=0f)}
    @Test fun restoreRetainsEdgeAtMiddle(){val v=zoomed();v.pan(1e6f,-1e6f);val r=ViewportGeometry().apply{configure(1000f,1000f,800f,600f,v.state)};assertEquals(v.placement,r.placement)}
    @Test fun resizeKeepsNormalizedCornerAnchor(){val v=zoomed();v.pan(1e6f,1e6f);v.configure(1000f,1000f,600f,900f,v.state);assertEquals(300f,v.placement.left,.001f);assertEquals(450f,v.placement.top,.001f)}
    @Test fun navigatingSameSizedPageKeepsExtraHorizontalPosition(){val v=zoomed();v.pan(1e6f,-1e6f);val r=ViewportGeometry().apply{configure(1000f,1000f,800f,600f,v.forPageNavigation())};assertEquals(v.placement.left,r.placement.left,.001f);assertEquals(0f,r.placement.top,.001f)}
    @Test fun nextPageUsesProportionalExtendedTravel(){val v=zoomed();v.pan(-1e6f,0f);val r=ViewportGeometry().apply{configure(500f,1000f,800f,600f,v.forPageNavigation())};assertEquals(400f,r.placement.left+r.placement.width,.001f)}
    @Test fun shortNavigatedPageAllowsVerticalDrag(){val v=ViewportGeometry().apply{configure(1200f,400f,800f,1000f,PageViewportState(2f,startAtTop=true))};v.pan(0f,200f);assertFalse(v.state.startAtTop);assertEquals(200f,v.placement.top,.001f)}
    @Test fun smallHorizontalAxisStillCentersUntilDragged(){val v=ViewportGeometry().apply{configure(200f,1000f,1000f,600f);zoomBy(1.5f,100f,200f)};assertEquals(410f,v.placement.left,.001f);v.pan(100f,0f);assertEquals(510f,v.placement.left,.001f)}
    @Test fun tinyPageAxisRemainsFullyVisible(){val v=ViewportGeometry().apply{configure(2000f,100f,800f,1000f);zoomBy(2f,400f,500f)};v.pan(0f,-1e6f);assertEquals(0f,v.placement.top,.001f);v.pan(0f,1e6f);assertEquals(1000f,v.placement.top+v.placement.height,.001f)}
    @Test fun recentCodecRoundTripsFiniteOffSheetVerticalAnchor(){val v=ViewportGeometry().apply{configure(2000f,100f,800f,1000f);zoomBy(2f,400f,500f);pan(0f,1e6f)};assertTrue(v.state.centerY<0f);val e=RecentDocument("content://synthetic/margin","Synthetic",0,0,1,v.state);assertEquals(listOf(e),RecentDocumentsCodec.decode(RecentDocumentsCodec.encode(listOf(e))))}
    @Test fun nonfiniteRecentAnchorStillRejects(){try{RecentDocumentsCodec.encode(listOf(RecentDocument("content://synthetic/margin","Synthetic",0,0,1,PageViewportState(2f,centerY=Float.NaN))));fail("Nonfinite anchor accepted")}catch(_:IllegalArgumentException){}}
    @Test fun coordinateRoundTripAtExtraTravel(){val v=zoomed();v.pan(1e6f,-1e6f);val p=v.pageToView(.1f,.9f);val q=v.viewToPage(p.first,p.second);assertEquals(.1f,q.first,.00001f);assertEquals(.9f,q.second,.00001f)}
    @Test fun pinchKeepsUnclampedFocalPointAfterPan(){val v=zoomed();v.pan(50f,-40f);val p=v.viewToPage(300f,200f);v.zoomBy(1.1f,300f,200f);val q=v.viewToPage(300f,200f);assertEquals(p.first,q.first,.00001f);assertEquals(p.second,q.second,.00001f)}
    @Test fun invalidPanDoesNotMoveAndZoomOutReboundsSafely(){val v=zoomed();v.pan(1e6f,-1e6f);val before=v.state;v.pan(Float.NaN,0f);assertEquals(before,v.state);v.zoomBy(.01f,400f,300f);assertEquals(1f,v.state.zoom,.00001f);val p=v.placement;assertTrue(p.left>=0f && p.top>=0f && p.left+p.width<=800.001f && p.top+p.height<=600.001f)}
}
