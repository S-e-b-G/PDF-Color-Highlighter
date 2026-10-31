// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

/** Rejoins a detached parenthesized year in an identifier heading, not arbitrary columns/cells. */
object InlineReferenceRows {
    const val MAX_LINES=2048
    private val year=Regex("""^\([0-9]{4}(?:[-–/][0-9]{2,4})?\)[.,;:]?$""")
    private data class Candidate(val left:Int,val right:Int)
    private fun text(line:PageTextLine)=line.text.replace('\u00A0',' ').replace('\u202F',' ').trim()
    private fun usable(line:PageTextLine)=line.selectable && line.sourceFragments==1 &&
        line.bounds.width.isFinite() && line.bounds.height.isFinite() &&
        line.bounds.width>0f && line.bounds.height>0f && line.glyphs.none{SelectionUnicodePolicy.unsupported(it)}
    private fun height(a:TextBounds,b:TextBounds)=max(a.height,b.height)
    private fun sameBaseline(a:TextBounds,b:TextBounds):Boolean {
        val h=height(a,b);val small=min(a.height,b.height)
        return small>0f && h/small<=1.35f && abs(a.top-b.top)<=.15f*h && abs(a.bottom-b.bottom)<=.15f*h
    }
    fun normalize(snapshot:PageTextSnapshot):PageTextSnapshot {
        if(snapshot.status!=PageTextStatus.READY || snapshot.lines.size !in 3..MAX_LINES)return snapshot
        val lines=snapshot.lines
        val bounds=lines.map{it.bounds}
        val eligible=lines.map{usable(it)}
        val dates=lines.mapIndexed{i,l->eligible[i] && l.glyphs.size<=24 && year.matches(text(l))}
        val groups=lines.indices.groupBy{lines[it].block to lines[it].run}
        val proposed=mutableListOf<Candidate>()
        for(group in groups.values){
            if(group.size<3)continue
            val dateIndices=group.filter{dates[it]}
            if(dateIndices.isEmpty())continue
            for(left in group){
                if(!eligible[left] || dates[left] || lines[left].glyphs.size>128 ||
                    lines[left].glyphs.none{Character.isDigit(it.codePoint)})continue
                val a=bounds[left]
                val candidates=dateIndices.filter{right->
                    val b=bounds[right];val h=height(a,b);val gap=b.left-a.right
                    right!=left && sameBaseline(a,b) && gap>=.15f*h && gap<=4f*h
                }
                if(candidates.size!=1)continue
                val right=candidates.single();val b=bounds[right];val h=height(a,b)
                // One immediate full-width continuation belongs to the SAME native block/run.
                // A bare heading or similarly aligned two-column/table row is insufficient.
                val bodies=group.filter{other->
                    if(other==left || other==right || !eligible[other] || dates[other])false
                    else{
                        val c=bounds[other];val small=min(h,c.height);val big=max(h,c.height)
                        small>0f && big/small<=1.35f && c.top>max(a.top,b.top)+.6f*h &&
                            c.top-max(a.bottom,b.bottom) in (-.1f*h)..(1.5f*h) &&
                            c.left>=a.left-2f*h && c.left<=a.left+.35f*h &&
                            c.right>=b.right-.35f*h && c.width>=(b.right-a.left)*.9f &&
                            lines[other].glyphs.any{Character.isLetter(it.codePoint)}
                    }
                }.sortedBy{bounds[it].top}
                if(bodies.isEmpty())continue
                val first=bounds[bodies.first()]
                if(bodies.drop(1).any{abs(bounds[it].top-first.top)<=.2f*h})continue
                proposed.add(Candidate(left,right))
            }
        }
        val uses=mutableMapOf<Int,Int>()
        for(c in proposed){uses[c.left]=(uses[c.left] ?: 0)+1;uses[c.right]=(uses[c.right] ?: 0)+1}
        val replacements=mutableMapOf<Int,PageTextLine>();val removed=mutableSetOf<Int>()
        for(c in proposed){
            if(uses[c.left]!=1 || uses[c.right]!=1)continue
            val left=lines[c.left];val right=lines[c.right];val offset=left.glyphs.size
            val spaces=left.spacesBefore.toMutableSet()
            spaces.addAll(right.spacesBefore.map{it+offset})
            if(!left.glyphs.last().whitespace && !right.glyphs.first().whitespace)spaces.add(offset)
            val lead=min(c.left,c.right)
            replacements[lead]=PageTextLine(left.block,left.run,left.glyphs+right.glyphs,true,
                spaces.toSet(),left.sourceFragments+right.sourceFragments)
            removed.add(if(lead==c.left)c.right else c.left)
        }
        if(replacements.isEmpty())return snapshot
        return snapshot.copy(lines=lines.indices.filter{it !in removed}.map{replacements[it] ?: lines[it]})
    }
}
