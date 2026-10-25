// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

/** UI-only paint band, intentionally not a TextGlyph/GlyphQuad or PDF annotation representation. */
data class PreviewPaintBand(val left:Float,val top:Float,val right:Float,val bottom:Float){
    val width:Float get()=right-left
    val height:Float get()=bottom-top
}
object PreviewPaintGeometry{
    fun bands(span:SelectionSpan):List<PreviewPaintBand>{
        val result=mutableListOf<PreviewPaintBand>()
        var current:PreviewPaintBand?=null
        for(glyph in span.glyphs){
            if(glyph.quad.area<0.000001)continue
            val b=glyph.quad.bounds
            if(!listOf(b.left,b.top,b.right,b.bottom,b.width,b.height).all{it.isFinite()} || b.width<=0f || b.height<=0f)continue
            val next=PreviewPaintBand(b.left,b.top,b.right,b.bottom)
            val old=current
            val h=if(old!=null)max(old.height,next.height)else next.height
            val sameRow=old!=null && max(old.height,next.height)/min(old.height,next.height)<=1.75f &&
                abs(old.top-next.top)<=h*0.35f && abs(old.bottom-next.bottom)<=h*0.35f
            val smallGap=old!=null && next.left-old.right<=1.5f*h && next.left>=old.left-0.15f*h
            if(old!=null && sameRow && smallGap){
                current=PreviewPaintBand(min(old.left,next.left),min(old.top,next.top),max(old.right,next.right),max(old.bottom,next.bottom))
            }else{
                if(old!=null)result.add(old)
                current=next
            }
        }
        current?.let{result.add(it)}
        return result.toList()
    }
}
