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
        if(span.glyphs.any{g->g.quad.area>=.000001 && (abs(g.quad.ul.y-g.quad.ur.y)>.03f || abs(g.quad.ll.y-g.quad.lr.y)>.03f)})return span.quads
        return build(span.glyphs).map{b->
        GlyphQuad(TextPoint(b.left,b.top),TextPoint(b.right,b.top),TextPoint(b.left,b.bottom),TextPoint(b.right,b.bottom))
    }
    }
    fun forQuads(quads:List<GlyphQuad>):List<GlyphQuad> {
        require(quads.isNotEmpty())
        if(quads.any{kotlin.math.abs(it.ul.y-it.ur.y)>.03f || kotlin.math.abs(it.ll.y-it.lr.y)>.03f})return quads
        return forSpan(SelectionSpan(TextAddress(0,0),TextAddress(0,quads.lastIndex),"","",
        quads.map{TextGlyph(65,it)}))
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
