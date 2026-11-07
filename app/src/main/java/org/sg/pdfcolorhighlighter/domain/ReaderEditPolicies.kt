// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

object SavedContentPolicy {
    fun needsSave(current:String?,saved:String?):Boolean = current!=null && current!=saved
}
object EndpointLensGeometry {
    const val ZOOM=1.8f
    fun window(point:TextPoint,width:Float,height:Float,density:Float):TextBounds? {
        if(!listOf(point.x,point.y,width,height,density).all{it.isFinite()} || density<=0f)return null
        val margin=6f*density
        val w=minOf(176f*density,width-2f*margin)
        val h=minOf(64f*density,height-2f*margin)
        if(w<24f*density || h<20f*density)return null
        val gap=24f*density
        val left=(point.x-w/2f).coerceIn(margin,maxOf(margin,width-margin-w))
        val above=point.y-gap-h
        val top=(if(above>=margin)above else point.y+gap).coerceIn(margin,maxOf(margin,height-margin-h))
        return TextBounds(left,top,left+w,top+h)
    }
}
