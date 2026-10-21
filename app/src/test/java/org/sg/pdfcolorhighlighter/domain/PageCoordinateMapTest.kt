// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class PageCoordinateMapTest {
    private val bounds=TextBounds(10f,20f,110f,220f)
    private fun close(a:TextPoint,b:TextPoint) { assertEquals(a.x,b.x,0.001f);assertEquals(a.y,b.y,0.001f) }
    private fun bad(block:()->Unit) { try { block();fail("Invalid transform accepted") } catch(_:IllegalArgumentException) {} }
    @Test fun nativeOriginMapsToSheetCorner() { close(TextPoint(5f,7f),PageCoordinateMap(bounds,5f,7f,200f,400f).toView(TextPoint(10f,20f))) }
    @Test fun sheetCornerMapsBackToNativeOrigin() { close(TextPoint(10f,20f),PageCoordinateMap(bounds,5f,7f,200f,400f).toPage(TextPoint(5f,7f))) }
    @Test fun zoomAndPanAreBothApplied() { close(TextPoint(110f,207f),PageCoordinateMap(bounds,-90f,-193f,400f,800f).toView(TextPoint(60f,120f))) }
    @Test fun negativeNativeOriginIsSupported() { val m=PageCoordinateMap(TextBounds(-20f,-40f,80f,160f),10f,30f,200f,400f);close(TextPoint(10f,30f),m.toView(TextPoint(-20f,-40f))) }
    @Test fun allCornersRoundTrip() { val m=PageCoordinateMap(bounds,-13f,91f,300f,600f);for(x in listOf(10f,110f))for(y in listOf(20f,220f))close(TextPoint(x,y),m.toPage(m.toView(TextPoint(x,y)))) }
    @Test fun interiorPointsRoundTrip() { val m=PageCoordinateMap(bounds,-200f,-350f,700f,1400f);repeat(100){val p=TextPoint(10f+it,20f+it*2);close(p,m.toPage(m.toView(p)))} }
    @Test fun outsideTouchesAreNotClampedToText() { val m=PageCoordinateMap(bounds,0f,0f,100f,200f);assertTrue(m.toPage(TextPoint(-10f,-10f)).x<bounds.left) }
    @Test fun nonuniformPhysicalSizeStillRoundTrips() { val m=PageCoordinateMap(bounds,0f,0f,700f,300f);close(TextPoint(45f,67f),m.toPage(m.toView(TextPoint(45f,67f)))) }
    @Test fun orientationUsesNewViewportWithoutChangingNativePoint() { val p=TextPoint(60f,120f);for(m in listOf(PageCoordinateMap(bounds,0f,0f,100f,200f),PageCoordinateMap(bounds,300f,20f,400f,800f)))close(p,m.toPage(m.toView(p))) }
    @Test fun zeroSizeIsRejected() { bad { PageCoordinateMap(bounds,0f,0f,0f,10f) };bad{PageCoordinateMap(TextBounds(1f,1f,1f,2f),0f,0f,10f,10f)} }
    @Test fun nonFiniteSizeIsRejected() { bad{PageCoordinateMap(bounds,Float.NaN,0f,10f,10f)};bad{PageCoordinateMap(bounds,0f,0f,Float.POSITIVE_INFINITY,10f)} }
    @Test fun mappingHasNoBitmapResolutionDependency() { assertEquals(PageCoordinateMap(bounds,1f,2f,300f,600f),PageCoordinateMap(bounds,1f,2f,300f,600f)) }
}
