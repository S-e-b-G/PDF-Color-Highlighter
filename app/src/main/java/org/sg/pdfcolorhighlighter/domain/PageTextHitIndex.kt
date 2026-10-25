// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Immutable copied geometry only. Bounded vertical buckets preserve original first-hit ordering. */
class PageTextHitIndex(private val owner:PageTextSnapshot){
    companion object {const val BUCKETS=128;const val MAX_LINES=20000;const val MAX_REFERENCES=200000L}
    private val bounds=owner.lines.map{it.bounds}
    private val page=owner.pageBounds
    private val buckets:Array<IntArray>?=buildBuckets()
    val usesBuckets:Boolean get()=buckets!=null
    val maximumBucketSize:Int get()=buckets?.maxOfOrNull{it.size} ?: 0
    fun owns(snapshot:PageTextSnapshot)=snapshot===owner
    private fun bucket(y:Float):Int=((y.toDouble()-page.top)/page.height*BUCKETS).toInt().coerceIn(0,BUCKETS-1)
    private fun buildBuckets():Array<IntArray>?{
        if(bounds.size>MAX_LINES || !page.height.isFinite() || page.height<=0f)return null
        var refs=0L
        for(b in bounds){refs+=bucket(b.bottom)-bucket(b.top)+1;if(refs>MAX_REFERENCES)return null}
        val rows=Array(BUCKETS){mutableListOf<Int>()}
        bounds.forEachIndexed{index,b->for(bin in bucket(b.top)..bucket(b.bottom))rows[bin].add(index)}
        return Array(BUCKETS){rows[it].toIntArray()}
    }
    fun hit(point:TextPoint):TextAddress?{
        if(owner.status!=PageTextStatus.READY)return null
        val candidates=if(buckets!=null && point.y>=page.top && point.y<=page.bottom)buckets[bucket(point.y)] else null
        if(candidates!=null){for(index in candidates)hitLine(index,point)?.let{return it}}
        else{for(index in bounds.indices)hitLine(index,point)?.let{return it}}
        return null
    }
    private fun hitLine(index:Int,point:TextPoint):TextAddress?{
        val b=bounds[index]
        if(point.x<b.left || point.x>b.right || point.y<b.top || point.y>b.bottom)return null
        val glyphs=owner.lines[index].glyphs
        for(glyph in glyphs.indices){
            val q=glyphs[glyph].quad;val box=q.bounds
            if(point.x>=box.left && point.x<=box.right && point.y>=box.top && point.y<=box.bottom && q.contains(point))return TextAddress(index,glyph)
        }
        return null
    }
}
