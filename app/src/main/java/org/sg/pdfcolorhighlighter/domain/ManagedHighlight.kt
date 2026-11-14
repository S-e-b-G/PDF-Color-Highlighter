// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Copied app-managed native state only; never a native object. */
data class ManagedHighlight(val edit:HighlightEdit,val contents:String,val mode:TextMode) {
    fun resized(span:SelectionSpan)=copy(edit=edit.copy(quotation=span.quotation,quads=span.quads,appearance=NativeHighlightAppearance.forSpan(span)),
        contents=if(mode==TextMode.AUTOMATIC)span.quotation else contents)
}
object ManagedHighlightSelection {
    /** Never guess which overlapping annotation the user intended. */
    fun hits(items:List<ManagedHighlight>,point:TextPoint)=items.filter{item->item.edit.quads.any{it.contains(point)}}
    fun span(snapshot:PageTextSnapshot,item:ManagedHighlight):SelectionSpan? {


        fun bucket(q:GlyphQuad):Pair<Int,Int>{val b=q.bounds;return kotlin.math.floor((b.left+b.right)/2).toInt() to kotlin.math.floor((b.top+b.bottom)/2).toInt()}
        fun close(a:GlyphQuad,b:GlyphQuad):Boolean {
            val aa=listOf(a.ul,a.ur,a.ll,a.lr);val bb=listOf(b.ul,b.ur,b.ll,b.lr)
            return aa.indices.all{kotlin.math.abs(aa[it].x-bb[it].x)<=.03f && kotlin.math.abs(aa[it].y-bb[it].y)<=.03f}
        }

        val bounds=item.edit.quads.map{it.bounds}
        val envelope=TextBounds(bounds.minOf{it.left}-.03f,bounds.minOf{it.top}-.03f,
            bounds.maxOf{it.right}+.03f,bounds.maxOf{it.bottom}+.03f)
        val buckets=item.edit.quads.withIndex().groupBy{bucket(it.value)}
        val matched=mutableSetOf<Int>()
        val accepted=mutableSetOf<TextGlyph>()
        val matches=mutableListOf<Pair<TextAddress,TextGlyph>>()

        for((l,line) in snapshot.lines.withIndex()){
            val lb=line.bounds
            if(lb.right<envelope.left || lb.left>envelope.right || lb.bottom<envelope.top || lb.top>envelope.bottom)continue
            for((g,glyph)in line.glyphs.withIndex()){
            if(glyph.quad.area<.000001)continue
            val b=bucket(glyph.quad)
            val candidates=(-1..1).flatMap{dx->(-1..1).flatMap{dy->buckets[b.first+dx to b.second+dy].orEmpty()}}
                .filter{close(it.value,glyph.quad)}
            if(candidates.isNotEmpty()){
                matched.addAll(candidates.map{it.index});accepted.add(glyph);matches.add(TextAddress(l,g) to glyph)
            }
        }
        }
        if(matches.isEmpty())return null
        fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
        // Stored glyph-quad order is reading order; native extraction may interleave columns.
        val begin=matches.firstOrNull{close(it.second.quad,item.edit.quads.first())} ?: return null
        val finish=matches.lastOrNull{close(it.second.quad,item.edit.quads.last())} ?: return null
        val attempt=PageTextSelector.select(snapshot,center(begin.second),center(finish.second),false)
        val span=(attempt as? TextSelectionAttempt.Selected)?.span ?: return null
        // Sparse/disjoint or unsupported native ranges must not become a broader editable span.
        if(span.glyphs.filter{it.quad.area>=.000001}.any{it !in accepted})return null
        if(matched.size!=item.edit.quads.size)return null
        val selected=span.quads.groupBy{bucket(it)}
        if(item.edit.quads.any{q->val key=bucket(q)
            (-1..1).none{dx->(-1..1).any{dy->selected[key.first+dx to key.second+dy].orEmpty().any{close(it,q)}}}
        })return null
        return span
    }
}
