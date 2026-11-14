// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

/** Bounded fallback for an explicit gesture between two coherent horizontal LTR columns.
 * Existing same-column flow is always tried first. Never invent missing glyphs or skip a third lane.
 */
object ColumnContinuationRows {
    const val MAX_LINES=2048
    private fun overlap(a:TextBounds,b:TextBounds)=min(a.right,b.right)-max(a.left,b.left)>min(a.width,b.width)*.2f
    private fun rtl(line:PageTextLine)=line.glyphs.any{g->
        val d=Character.getDirectionality(g.codePoint)
        d==Character.DIRECTIONALITY_RIGHT_TO_LEFT || d==Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
            d==Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || d==Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
    }
    private fun component(s:PageTextSnapshot,ids:List<Int>,seed:Int,h:Float):List<Int> {
        val sorted=ids.sortedWith(compareBy<Int>{s.lines[it].bounds.top}.thenBy{s.lines[it].bounds.left})
        val groups=mutableListOf<MutableList<Int>>();var bottom=Float.NEGATIVE_INFINITY
        for(i in sorted){val r=s.lines[i].bounds
            if(groups.isEmpty() || r.top-bottom>h*6f)groups.add(mutableListOf())
            groups.last().add(i);bottom=if(groups.last().size==1)r.bottom else max(bottom,r.bottom)
        }
        return groups.firstOrNull{seed in it}.orEmpty()
    }
    private fun rows(s:PageTextSnapshot,ids:List<Int>,h:Float):Int {
        var n=0;var y=Float.NEGATIVE_INFINITY
        for(i in ids.sortedBy{s.lines[it].bounds.top}){val top=s.lines[i].bounds.top
            if(n==0 || top-y>h*.5f){n++;y=top}
        }
        return n
    }
    fun path(s:PageTextSnapshot,a:Int,b:Int):List<Int>? {
        if(s.status!=PageTextStatus.READY || s.lines.size>MAX_LINES || a !in s.lines.indices || b !in s.lines.indices || a==b)return null
        val li=if(s.lines[a].bounds.left<s.lines[b].bounds.left)a else b
        val ri=if(li==a)b else a
        val l=s.lines[li].bounds;val r=s.lines[ri].bounds;val h=max(l.height,r.height)
        if(!h.isFinite() || h<=0f || !l.width.isFinite() || !r.width.isFinite() || l.width<=0f || r.width<=0f || r.left-l.right<3f*h)return null
        if(!s.lines[li].horizontal || !s.lines[ri].horizontal || rtl(s.lines[li]) || rtl(s.lines[ri]))return null
        val left=mutableListOf<Int>();val right=mutableListOf<Int>()
        for(i in s.lines.indices){val line=s.lines[i];val q=line.bounds
            if(!line.horizontal || q.height<=0f || q.width<=0f || max(q.height,h)/min(q.height,h)>1.8f)continue
            val x=overlap(q,l);val y=overlap(q,r)
            if(x && !y)left.add(i)
            if(y && !x)right.add(i)
        }
        val lc=component(s,left,li,h);val rc=component(s,right,ri,h)
        if(rows(s,lc,h)<3 || rows(s,rc,h)<3)return null
        val lb=lc.map{s.lines[it].bounds};val rb=rc.map{s.lines[it].bounds}
        val gutterLeft=lb.maxOf{it.right};val gutterRight=rb.minOf{it.left}
        if(gutterRight-gutterLeft<3f*h)return null
        val lt=lb.minOf{it.top};val botL=lb.maxOf{it.bottom};val rt=rb.minOf{it.top};val botR=rb.maxOf{it.bottom}
        if(min(botL,botR)-max(lt,rt)<max(2f*h,min(botL-lt,botR-rt)*.5f))return null
        val top=min(lt,rt);val bottom=max(botL,botR)
        val known=(lc+rc).toHashSet()
        // A heading, caption, marginal fragment, rotated run or third column inside this band
        // requires a reading-order decision we cannot justify. Outside-band material is untouched.
        for(i in s.lines.indices){val q=s.lines[i].bounds
            if(q.bottom>top && q.top<bottom && i !in known)return null
        }
        if(known.any{rtl(s.lines[it])})return null
        fun lane(ids:List<Int>):List<Int>? {
            val first=ids.minWithOrNull(compareBy<Int>{s.lines[it].bounds.top}.thenBy{s.lines[it].bounds.left}) ?: return null
            val last=ids.maxWithOrNull(compareBy<Int>{s.lines[it].bounds.top}.thenBy{s.lines[it].bounds.right}) ?: return null
            val result=ColumnFlowRows.path(s,first,last) ?: return null
            return result.takeIf{it.toSet()==ids.toSet()}
        }
        val lp=lane(lc) ?: return null;val rp=lane(rc) ?: return null
        return (lp+rp).takeIf{li in it && ri in it && it.distinct().size==it.size}
    }
}
