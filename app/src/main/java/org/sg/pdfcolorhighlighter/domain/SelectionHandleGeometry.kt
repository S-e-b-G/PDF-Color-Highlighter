// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

enum class SelectionHandleSide{START,END}
data class SelectionHandle(val side:SelectionHandleSide,val anchor:TextPoint,val center:TextPoint,
                           val nativeCenter:TextPoint,val visible:Boolean)
data class SelectionHandles(val start:SelectionHandle,val end:SelectionHandle,val radius:Float)
object SelectionHandleGeometry{
    private fun center(q:GlyphQuad)=TextPoint(((q.ul.x.toDouble()+q.ur.x+q.ll.x+q.lr.x)/4).toFloat(),
        ((q.ul.y.toDouble()+q.ur.y+q.ll.y+q.lr.y)/4).toFloat())
    fun layout(span:SelectionSpan,mapping:PageCoordinateMap,density:Float,width:Float,height:Float):SelectionHandles?{
        if(!density.isFinite() || density<=0f || !width.isFinite() || !height.isFinite())return null
        val radius=7f*density
        if(width<2*radius || height<2*radius || span.glyphs.isEmpty() || span.glyphs.none{it.quad.area>=0.000001})return null
        fun handle(g:TextGlyph,side:SelectionHandleSide):SelectionHandle{
            val q=g.quad;val start=side==SelectionHandleSide.START
            val anchor=mapping.toView(if(start)q.ul else q.lr)
            val x=anchor.x+(if(start)-radius else radius)
            val y=anchor.y+(if(start)-12f*density else 12f*density)
            val onScreen=anchor.x>=-radius && anchor.x<=width+radius && anchor.y>=-radius && anchor.y<=height+radius
            // Never clamp the start circle down onto selected text when there is no room above.
            val visible=onScreen && (!start || y-radius>=0f)
            return SelectionHandle(side,anchor,TextPoint(x.coerceIn(radius,width-radius),y.coerceIn(radius,height-radius)),center(q),visible)
        }
        return SelectionHandles(handle(span.glyphs.first(),SelectionHandleSide.START),handle(span.glyphs.last(),SelectionHandleSide.END),radius)
    }
    fun hit(handles:SelectionHandles,point:TextPoint,density:Float):SelectionHandleSide?{
        if(!density.isFinite() || density<=0f)return null
        val limit=24f*density
        return listOf(handles.start,handles.end).filter{it.visible}.map{h->
            val dx=point.x.toDouble()-h.center.x;val dy=point.y.toDouble()-h.center.y
            h.side to dx*dx+dy*dy
        }.filter{it.second<=limit.toDouble()*limit}.minByOrNull{it.second}?.first
    }
}
