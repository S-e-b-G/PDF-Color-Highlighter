// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Copied provenance only: no native handle and no annotation-index identity. */
data class ImportedAnnotationRef(val name:String,val signature:String){
    init{require(name.isNotBlank() && name.length<=1024 && '\u0000' !in name);require(Regex("[0-9a-f]{64}").matches(signature))}
    fun id(page:Int,type:String):String{
        require(page>=0 && type in setOf("Highlight","Text"))
        return java.util.UUID.nameUUIDFromBytes(("SGCH-import-v1\u0000$type\u0000$page\u0000$name").toByteArray(Charsets.UTF_8)).toString()
    }
}
object ImportedHighlightGeometry{
    fun span(snapshot:PageTextSnapshot,paint:List<GlyphQuad>):SelectionSpan?{
        if(snapshot.status!=PageTextStatus.READY || paint.isEmpty() || paint.size>25000)return null
        fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2f,(b.top+b.bottom)/2f)}
        val ordered=snapshot.lines.flatMap{it.glyphs}.filter{it.quad.area>=.000001}
        // Called for selection, never permit unbounded glyph-by-band work on the UI thread.
        if(ordered.size.toLong()*paint.size.toLong()>1_000_000L)return null
        val accepted=ordered.filter{g->paint.any{it.contains(center(g))}}
        if(accepted.isEmpty())return null
        val first=accepted.firstOrNull{paint.first().contains(center(it))} ?: return null
        val last=accepted.lastOrNull{paint.last().contains(center(it))} ?: return null
        val attempt=PageTextSelector.select(snapshot,center(first),center(last),false) as? TextSelectionAttempt.Selected ?: return null
        val visible=attempt.span.glyphs.filter{!it.whitespace && it.quad.area>=.000001}.toSet()
        val expected=accepted.filter{!it.whitespace}.toSet()
        if(visible!=expected || paint.any{q->accepted.none{q.contains(center(it))}})return null
        return attempt.span
    }
}
