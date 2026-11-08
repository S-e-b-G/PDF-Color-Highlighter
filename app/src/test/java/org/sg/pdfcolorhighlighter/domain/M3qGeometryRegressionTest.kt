// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class M3qGeometryRegressionTest{
    private fun cases():List<List<GlyphQuad>>{
        val lines=checkNotNull(javaClass.classLoader?.getResourceAsStream("m3q_geometry.txt")).bufferedReader().use{it.readLines()}
        return lines.filter{it.startsWith("SGQ1|")}.map{HighlightGeometryCodec.decode(it)}
    }
    private fun q(x:Float,y:Float=10f,w:Float=8f,h:Float=10f,dy:Float=0f)=GlyphQuad(TextPoint(x,y),TextPoint(x+w,y+dy),TextPoint(x,y+h),TextPoint(x+w,y+h+dy))
    @Test fun exactHeadingFixtureBecomesOneBand(){assertEquals(1,NativeHighlightAppearance.forQuads(cases()[0]).size)}
    @Test fun exactParagraphFixtureBecomesTenRows(){assertEquals(10,NativeHighlightAppearance.forQuads(cases()[1]).size)}
    @Test fun headingLogicalQuadsNotChanged(){val qs=cases()[0];val copy=qs.toList();NativeHighlightAppearance.forQuads(qs);assertEquals(copy,qs)}
    @Test fun paragraphLogicalQuadsNotChanged(){val qs=cases()[1];val copy=qs.toList();NativeHighlightAppearance.forQuads(qs);assertEquals(copy,qs)}
    @Test fun numericFixtureDimensionsStable(){assertEquals(listOf(38,645),cases().map{it.size})}
    @Test fun isolatedCompositeDoesNotDisableWholeRun(){assertEquals(1,NativeHighlightAppearance.forQuads(listOf(q(0f),q(8f,9f,w=3f,dy=1f),q(11f))).size)}
    @Test fun narrowSlightDeviationDoesNotDisableWholeRun(){assertEquals(1,NativeHighlightAppearance.forQuads(listOf(q(0f),q(8f,w=1f,dy=-.06f),q(9f))).size)}
    @Test fun substantialMixedRotationStaysRaw(){val tilted=q(8f,w=8f,dy=7f);val result=NativeHighlightAppearance.forQuads(listOf(q(0f),tilted,q(16f)));assertTrue(result.contains(tilted));assertEquals(3,result.size)}
    @Test fun entireRotatedRunPreserved(){val qs=listOf(q(0f,dy=2f),q(8f,dy=2f));assertEquals(qs,NativeHighlightAppearance.forQuads(qs))}
    @Test fun thinPositiveInlineFragmentBorrowsRow(){assertEquals(1,NativeHighlightAppearance.forQuads(listOf(q(0f),q(8f,16f,w=3f,h=.8f),q(11f))).size)}
    @Test fun separateRowsStillSeparate(){assertEquals(2,NativeHighlightAppearance.forQuads(listOf(q(0f),q(0f,30f))).size)}
    @Test fun largeColumnGutterStillSeparate(){assertEquals(2,NativeHighlightAppearance.forQuads(listOf(q(0f),q(100f))).size)}
}
