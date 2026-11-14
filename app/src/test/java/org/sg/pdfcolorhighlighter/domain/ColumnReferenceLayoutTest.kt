// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class ColumnReferenceLayoutTest {
    private fun line(text:String,x:Float,y:Float,w:Float=243f,h:Float=7.33f,horizontal:Boolean=true):PageTextLine {
        val d=w/text.length
        return PageTextLine(0,0,text.mapIndexed{i,c->val a=x+i*d
            TextGlyph(c.code,GlyphQuad(TextPoint(a,y),TextPoint(a+d,y),TextPoint(a,y+h),TextPoint(a+d,y+h)))},horizontal=horizontal)
    }
    private fun page(heading:Boolean=false,margin:Boolean=false,footer:Boolean=false):PageTextSnapshot {
        val ls=mutableListOf<PageTextLine>()
        if(heading)ls.add(line("LARGE LEFT TITLE",40.48f,222.26f,196.46f,14.76f))
        ls.addAll((0..5).map{line("LEFT $it body words",40.48f,257.23f+it*8.56f)})
        ls.addAll((0..5).map{line("RIGHT $it body words",310.45f,252.23f+it*8.79f)})
        if(margin)ls.add(line("VERTICAL NOTICE",15.54f,264.42f,8.12f,444.24f,false))
        if(footer){ls.add(line("LEFT FOOTER",40.48f,344.2f,55.63f,9.15f));ls.add(line("CENTER FOOTER",197.29f,345f,199.37f,6.41f))}
        return PageTextSnapshot(TextBounds(0f,0f,595.28f,841.89f),ls,PageTextStatus.READY)
    }
    private fun target(s:PageTextSnapshot,prefix:String)=s.lines.indexOfFirst{it.text.startsWith(prefix)}
    private fun path(s:PageTextSnapshot)=ColumnContinuationRows.path(s,target(s,"LEFT 4"),target(s,"RIGHT 1"))
    private fun span(s:PageTextSnapshot):SelectionSpan {
        fun center(g:TextGlyph):TextPoint {val q=g.quad.bounds;return TextPoint((q.left+q.right)/2,(q.top+q.bottom)/2)}
        return (PageTextSelector.select(s,center(s.lines[target(s,"LEFT 4")].glyphs.first()),center(s.lines[target(s,"RIGHT 1")].glyphs.last()),false) as TextSelectionAttempt.Selected).span
    }
    @Test fun narrowButClearBodyGutterIsAllowed(){assertNotNull(path(page()))}
    @Test fun distantRotatedMarginDoesNotBlock(){assertNotNull(path(page(margin=true)))}
    @Test fun fontSizeChangeInsideOneLaneIsAllowed(){assertNotNull(path(page(heading=true)))}
    @Test fun headingEndpointUsesBodyScale(){val s=page(true);assertNotNull(ColumnContinuationRows.path(s,0,target(s,"RIGHT 1")))}
    @Test fun footerNotConnectedToBody(){val s=page(footer=true);val p=checkNotNull(path(s));assertTrue(p.none{s.lines[it].text.contains("FOOTER")})}
    @Test fun marginIsNeverIncludedInSelection(){assertFalse(span(page(margin=true)).visualText.contains("NOTICE"))}
    @Test fun reversePathKeepsColumnOrder(){val s=page(true,true);assertEquals(path(s),ColumnContinuationRows.path(s,target(s,"RIGHT 1"),target(s,"LEFT 4")))}
    @Test fun rotatedTextIntrudingIntoBodyStillRejects(){val s=page();assertNull(path(s.copy(lines=s.lines+line("INTRUSION",55f,270f,8f,80f,false))))}
    @Test fun thirdLaneInGutterStillRejects(){val s=page();assertNull(path(s.copy(lines=s.lines+line("SIDE",290f,270f,12f,7f))))}
    @Test fun spanningRowInBandStillRejects(){val s=page();assertNull(path(s.copy(lines=s.lines+line("SPANNER",40f,275f,510f,7f))))}
    @Test fun insufficientColumnsStillReject(){val s=page();val ls=s.lines.filter{it.text.startsWith("LEFT 4") || it.text.startsWith("RIGHT 1")};assertNull(ColumnContinuationRows.path(s.copy(lines=ls),0,1))}
    @Test fun actualSuffixPrefixGeometryIsPreserved(){val s=page(true,true);val r=span(s);val wanted=listOf("LEFT 4","LEFT 5","RIGHT 0","RIGHT 1").flatMap{p->s.lines[target(s,p)].glyphs};assertEquals(wanted,r.glyphs);assertTrue(r.quads.none{it.bounds.left<290 && it.bounds.right>300})}
}
