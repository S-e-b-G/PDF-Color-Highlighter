// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.lang.ref.WeakReference
import kotlin.math.*

/** User-directed same-column row intervals, not a nearest-next-line continuation graph. */
object ColumnFlowRows {
    // Matches the native adapter's absolute page glyph budget; no paragraph or gap limit.
    const val MAX_LINES=100000
    private class Prepared(val owner:PageTextSnapshot) {
        val bounds=owner.lines.map{it.bounds}
        val rows=mutableListOf<MutableList<Int>>()
        val rowOf=IntArray(owner.lines.size){-1}
        init {
            // Rotated text is not a physical row of the horizontal reading flow. Keep it
            // in the snapshot, but do not let its tall bounds attach to ordinary body text.
            val usable=bounds.indices.filter{owner.lines[it].horizontal && ColumnFlowRows.valid(bounds[it])}.sortedWith(
                compareBy<Int>{ColumnFlowRows.center(bounds[it])}.thenBy{bounds[it].left}.thenBy{it})
            for(index in usable){
                val previous=rows.lastOrNull()
                if(previous!=null && ColumnFlowRows.sameRow(bounds[previous.first()],bounds[index]))previous.add(index)
                else rows.add(mutableListOf(index))
            }
            rows.forEachIndexed{row,members->members.sortWith(compareBy<Int>{bounds[it].left}.thenBy{it});members.forEach{rowOf[it]=row}}
        }
    }
    private var cached=WeakReference<Prepared>(null)
    @Synchronized private fun prepare(snapshot:PageTextSnapshot):Prepared {
        val old=cached.get();if(old!=null && old.owner===snapshot)return old
        return Prepared(snapshot).also{cached=WeakReference(it)}
    }
    private fun valid(b:TextBounds)=b.width.isFinite() && b.height.isFinite() && b.width>0f && b.height>0f
    private fun center(b:TextBounds)=(b.top.toDouble()+b.bottom)/2
    private fun sameRow(a:TextBounds,b:TextBounds):Boolean {
        val small=min(a.height,b.height)
        val overlap=min(a.bottom,b.bottom)-max(a.top,b.top)
        return overlap>=small*.5f && abs(center(a)-center(b))<=small*.4f
    }
    private fun intersects(b:TextBounds,left:Float,right:Float):Boolean {
        val overlap=min(b.right,right)-max(b.left,left)
        return overlap>0f && overlap>=min(b.width,right-left)*.2f
    }
    private fun inlineNeighbor(x:TextBounds,y:TextBounds):Boolean {
        val h=max(x.height,y.height)
        val gap=max(x.left,y.left)-min(x.right,y.right)
        // Geometry, not fonts/native block IDs: include compact detached prefixes and
        // tightly adjacent styled runs, without spanning a full-width column gutter.
        val compact=min(x.width,y.width)<=2f*h
        return gap<=h*(if(compact)3f else .35f)
    }
    fun path(snapshot:PageTextSnapshot,firstLine:Int,lastLine:Int):List<Int>? {
        if(snapshot.status!=PageTextStatus.READY || snapshot.lines.size>MAX_LINES)return null
        if(firstLine !in snapshot.lines.indices || lastLine !in snapshot.lines.indices)return null
        if(!snapshot.lines[firstLine].horizontal || !snapshot.lines[lastLine].horizontal)return null
        if(firstLine==lastLine)return listOf(firstLine)
        val p=prepare(snapshot);val first=p.bounds[firstLine];val last=p.bounds[lastLine]
        val a=p.rowOf[firstLine];val b=p.rowOf[lastLine]
        if(a<0 || b<0)return null
        var left=max(first.left,last.left);var right=min(first.right,last.right)
        if(right<=left){
            val gap=max(first.left,last.left)-min(first.right,last.right)
            val h=max(first.height,last.height)
            val compact=min(first.width,last.width)<=2f*h
            if(gap>h*(if(compact)3f else 1.5f))return null
            left=min(first.left,last.left);right=max(first.right,last.right)
        }
        val result=mutableListOf<Int>()
        for(row in min(a,b)..max(a,b)){
            val members=p.rows[row]
            val chosen=BooleanArray(members.size)
            val work=java.util.ArrayDeque<Int>()
            for(position in members.indices){
                val index=members[position]
                if(index==firstLine || index==lastLine || intersects(p.bounds[index],left,right)){
                    chosen[position]=true;work.addLast(position)
                }
            }
            // Linear adjacency closure; a long fragmented row must not cause quadratic work.
            while(work.isNotEmpty()){
                val position=work.removeFirst()
                for(next in intArrayOf(position-1,position+1)){
                    if(next in members.indices && !chosen[next] &&
                        inlineNeighbor(p.bounds[members[position]],p.bounds[members[next]])){
                        chosen[next]=true;work.addLast(next)
                    }
                }
            }
            for(position in members.indices)if(chosen[position])result.add(members[position])
        }
        return result.takeIf{firstLine in it && lastLine in it}
    }
}
