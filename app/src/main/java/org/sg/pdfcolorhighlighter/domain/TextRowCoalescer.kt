// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

/** Supported same-baseline fragments only. No synthetic glyph/quad or cross-block/column merge. */
object TextRowCoalescer {
    const val MAX_LINES=2048
    private fun sane(b:TextBounds)=listOf(b.left,b.top,b.right,b.bottom,b.width,b.height).all{it.isFinite()} && b.width>0f && b.height>0f
    private fun heightCompatible(a:TextBounds,b:TextBounds)=sane(a) && sane(b) && max(a.height,b.height)/min(a.height,b.height)<=1.35f
    private fun baseline(a:TextBounds,b:TextBounds):Boolean {
        if(!heightCompatible(a,b))return false
        val tolerance=max(a.height,b.height)*0.2f
        return abs(a.top-b.top)<=tolerance && abs(a.bottom-b.bottom)<=tolerance
    }
    private fun unsafeStart(line:PageTextLine):Boolean {
        val cp=line.glyphs.firstOrNull{!it.whitespace}?.codePoint ?: return false
        val type=Character.getType(cp)
        return type==Character.NON_SPACING_MARK.toInt() || type==Character.COMBINING_SPACING_MARK.toInt() ||
            type==Character.ENCLOSING_MARK.toInt() || cp==0x200D || cp in 0x1F3FB..0x1F3FF
    }
    fun normalize(snapshot:PageTextSnapshot):PageTextSnapshot {
        if(snapshot.status!=PageTextStatus.READY || snapshot.lines.size>MAX_LINES || snapshot.lines.size<2)return snapshot
        val lines=snapshot.lines;val bounds=lines.map{it.bounds}
        val replacements=mutableMapOf<Int,PageTextLine>();val removed=mutableSetOf<Int>()
        val groups=lines.indices.groupBy{lines[it].block to lines[it].run}
        for(group in groups.values){
            val rows=mutableListOf<MutableList<Int>>()
            for(index in group.sortedWith(compareBy<Int>{bounds[it].top}.thenBy{bounds[it].left})){
                val row=rows.lastOrNull()
                if(row!=null && baseline(bounds[row.first()],bounds[index]))row.add(index)
                else rows.add(mutableListOf(index))
            }
            for(row in rows){
                if(row.size<2)continue
                val chunks=mutableListOf<MutableList<Int>>()
                for(index in row.sortedBy{bounds[it].left}){
                    val chunk=chunks.lastOrNull()
                    val previous=chunk?.lastOrNull()
                    val h=if(previous!=null)max(bounds[previous].height,bounds[index].height)else 1f
                    val gap=if(previous!=null)bounds[index].left-bounds[previous].right else 0f
                    if(chunk!=null && previous!=null && gap>=-0.15f*h && gap<=1.25f*h)chunk.add(index)
                    else chunks.add(mutableListOf(index))
                }
                for(chunk in chunks){
                    if(chunk.size<2 || chunk.any{!lines[it].selectable || lines[it].sourceFragments!=1 || unsafeStart(lines[it]) || !sane(bounds[it])})continue
                    val left=chunk.minOf{bounds[it].left};val right=chunk.maxOf{bounds[it].right}
                    val top=chunk.minOf{bounds[it].top};val bottom=chunk.maxOf{bounds[it].bottom}
                    val box=TextBounds(left,top,right,bottom);val h=box.height
                    val supportTops=mutableListOf<Float>()
                    for(other in group){
                        if(other in row || !lines[other].selectable || lines[other].sourceFragments!=1 ||
                            lines[other].spacesBefore.isNotEmpty() || lines[other].glyphs.all{it.whitespace})continue
                        val b=bounds[other]
                        if(!heightCompatible(box,b) || abs(b.top-top)<=0.4f*h || abs(b.top-top)>3.5f*h)continue
                        if(b.left>left+0.35f*h || b.right<right-0.35f*h || b.width<box.width*0.9f)continue
                        if(supportTops.none{abs(it-b.top)<=0.4f*h})supportTops.add(b.top)
                        if(supportTops.size>=2)break
                    }
                    if(supportTops.size<2)continue // One broad heading must not justify joining table/column cells.
                    val glyphs=mutableListOf<TextGlyph>();val spaces=mutableSetOf<Int>()
                    for((position,index) in chunk.withIndex()){
                        val line=lines[index];val offset=glyphs.size
                        if(position>0){
                            val gap=bounds[index].left-bounds[chunk[position-1]].right
                            if(gap>=0.15f*h && !glyphs.last().whitespace && !line.glyphs.first().whitespace)spaces.add(offset)
                        }
                        spaces.addAll(line.spacesBefore.map{it+offset})
                        glyphs.addAll(line.glyphs) // Actual immutable glyph objects/geometry, never a made-up space quad.
                    }
                    val lead=chunk.minOrNull()!!
                    replacements[lead]=PageTextLine(lines[lead].block,lines[lead].run,glyphs.toList(),true,
                        spaces.toSet(),chunk.sumOf{lines[it].sourceFragments})
                    removed.addAll(chunk.filter{it!=lead})
                }
            }
        }
        if(replacements.isEmpty())return snapshot
        return snapshot.copy(lines=lines.indices.filter{it !in removed}.map{replacements[it] ?: lines[it]})
    }
}
