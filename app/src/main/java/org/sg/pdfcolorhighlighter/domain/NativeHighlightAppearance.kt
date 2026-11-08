// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

/** Native horizontal appearance only. Exact glyph geometry is stored separately for editing.
 * Only explicitly selected rows/characters enter here; never use these bands as text endpoints. */
data class NativeAppearanceBand(val left:Float,val top:Float,val right:Float,val bottom:Float){
    val width:Float get()=right-left
    val height:Float get()=bottom-top
}
object NativeHighlightAppearance{
    private fun ellipsis(cp:Int)=cp==46 || cp==0x2026

    fun forSpan(span:SelectionSpan):List<GlyphQuad> {
        val glyphs=span.glyphs
        val slopes=glyphs.mapNotNull{g->
            val q=g.quad;val dx=q.ur.x-q.ul.x;val dy=q.ur.y-q.ul.y
            if(q.area>=.000001 && dx>0f && abs(dy)<=dx*.03f)dy/dx else null
        }.sorted()
        if(slopes.isEmpty())return span.quads
        val slope=slopes[slopes.size/2];val ux=1f/sqrt(1f+slope*slope);val uy=slope*ux
        fun project(p:TextPoint)=TextPoint(p.x*ux+p.y*uy,-p.x*uy+p.y*ux)
        fun inverse(p:TextPoint)=TextPoint(p.x*ux-p.y*uy,p.x*uy+p.y*ux)
        fun mapped(q:GlyphQuad,fn:(TextPoint)->TextPoint)=GlyphQuad(fn(q.ul),fn(q.ur),fn(q.ll),fn(q.lr))
        val projected=glyphs.map{it.copy(quad=mapped(it.quad,::project))}
        val normal=glyphs.map{g->
            val q=g.quad;val x=q.ur.x-q.ul.x;val y=q.ur.y-q.ul.y
            q.area<.000001 || (x>0f && abs(y)<=x*.03f && abs(y*ux-x*uy)<=sqrt(x*x+y*y)*.006f)
        }
        fun fits(a:TextBounds,n:TextBounds):Boolean{
            val h=max(a.height,n.height);val small=min(a.height,n.height)
            return small>0f && h/small<=1.75f && min(a.bottom,n.bottom)-max(a.top,n.top)>=small*.6f &&
                a.left<=n.right+2.5f*h && a.right>=n.left-2.5f*h
        }
        val result=mutableListOf<GlyphQuad>();val run=mutableListOf<TextGlyph>()
        fun flush(){
            for(r in build(run))result.add(mapped(GlyphQuad(TextPoint(r.left,r.top),TextPoint(r.right,r.top),
                TextPoint(r.left,r.bottom),TextPoint(r.right,r.bottom)),::inverse))
            run.clear()
        }
        for(i in glyphs.indices){
            val q=projected[i].quad;val box=q.bounds
            // An isolated composite accent/narrow glyph may have a misleading top-edge slope.
            // Require normal neighbors on BOTH sides in the same local row. Other rotations stay raw.
            val local=i>0 && i<glyphs.lastIndex && normal[i-1] && normal[i+1] &&
                fits(box,projected[i-1].quad.bounds) && fits(box,projected[i+1].quad.bounds) &&
                fits(projected[i-1].quad.bounds,projected[i+1].quad.bounds) &&
                box.width<=max(projected[i-1].quad.bounds.height,projected[i+1].quad.bounds.height)*1.75f &&
                abs(q.ur.y-q.ul.y)<=max(projected[i-1].quad.bounds.height,projected[i+1].quad.bounds.height)*.35f &&
                abs(q.lr.y-q.ll.y)<=max(projected[i-1].quad.bounds.height,projected[i+1].quad.bounds.height)*.35f
            if(normal[i] || local)run.add(projected[i])
            else{flush();if(glyphs[i].quad.area>=.000001)result.add(glyphs[i].quad)}
        }
        flush();return result.toList()
    }
    fun forQuads(quads:List<GlyphQuad>):List<GlyphQuad> {
        require(quads.isNotEmpty())
        return forSpan(SelectionSpan(TextAddress(0,0),TextAddress(0,quads.lastIndex),"","",quads.map{TextGlyph(65,it)}))
    }
    private fun build(glyphs:List<TextGlyph>):List<NativeAppearanceBand>{
        val result=mutableListOf<NativeAppearanceBand>()
        var active=false;var left=0f;var top=0f;var right=0f;var bottom=0f
        for((index,glyph) in glyphs.withIndex()){
            val b=glyph.quad.bounds;val width=b.width
            if(!b.left.isFinite() || !b.top.isFinite() || !b.right.isFinite() || !b.bottom.isFinite() || !width.isFinite() || width<=0f)continue
            var y0=b.top;var y1=b.bottom;var borrowed=false
            if(ellipsis(glyph.codePoint)){
                val h=bottom-top
                if(active && h>0f && b.top>=top-.15f*h && b.bottom<=bottom+.35f*h &&
                    b.left>=left-.15f*h && b.left-right<=2.5f*h){
                    y0=top;y1=bottom;borrowed=true
                }else{
                    for(nextIndex in index+1..min(glyphs.lastIndex,index+8)){
                        val next=glyphs[nextIndex]
                        if(next.whitespace || ellipsis(next.codePoint))continue
                        val n=next.quad.bounds;val nh=n.height
                        if(next.quad.area>=0.000001 && nh.isFinite() && nh>0f && n.width>0f &&
                            b.top>=n.top-.15f*nh && b.bottom<=n.bottom+.35f*nh &&
                            n.left>=b.left-.15f*nh && n.left-b.right<=1.5f*nh){
                            y0=n.top;y1=n.bottom;borrowed=true
                        }
                        break
                    }
                }
            }

            // A tiny positive-area inline fragment (for example this PDF's short dash) must not
            // reset a body-row band. Require containment plus a following full-height body glyph.
            val bodyHeight=bottom-top
            if(active && !borrowed && glyph.quad.area>=.000001 && bodyHeight>0f &&
                y1-y0<=bodyHeight*.35f && y0>=top-.05f*bodyHeight && y1<=bottom+.05f*bodyHeight &&
                b.left>=left-.15f*bodyHeight && b.left-right<=2.5f*bodyHeight){
                val next=glyphs.getOrNull(index+1)?.quad?.bounds
                if(next!=null && next.height>=bodyHeight*.7f && next.height<=bodyHeight*1.4f &&
                    abs(next.top-top)<=bodyHeight*.35f && abs(next.bottom-bottom)<=bodyHeight*.35f &&
                    next.left>=b.left-.15f*bodyHeight && next.left-b.right<=2.5f*bodyHeight){
                    y0=top;y1=bottom;borrowed=true
                }
            }
            val height=y1-y0
            if((!borrowed && glyph.quad.area<0.000001) || !height.isFinite() || height<=0f)continue
            val oldHeight=bottom-top;val h=if(active)max(oldHeight,height)else height
            val same=active && max(oldHeight,height)/min(oldHeight,height)<=1.75f && abs(top-y0)<=h*.35f && abs(bottom-y1)<=h*.35f
            val close=active && b.left-right<=2.5f*h && b.left>=left-.15f*h
            if(same && close){left=min(left,b.left);top=min(top,y0);right=max(right,b.right);bottom=max(bottom,y1)}
            else{if(active)result.add(NativeAppearanceBand(left,top,right,bottom));left=b.left;top=y0;right=b.right;bottom=y1;active=true}
        }
        if(active)result.add(NativeAppearanceBand(left,top,right,bottom))
        return result.toList()
    }
}
