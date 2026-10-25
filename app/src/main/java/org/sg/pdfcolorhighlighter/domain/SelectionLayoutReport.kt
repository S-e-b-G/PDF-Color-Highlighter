// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Structural numeric metadata only: never glyph text/code points, quotations, URI or document name. */
data class SelectionLineReport(val index:Int,val block:Int,val run:Int,val bounds:TextBounds,
                               val glyphCount:Int,val selectable:Boolean)
data class SelectionLayoutReport(val status:PageTextStatus?,val rejection:SelectionRejection?,
                                 val pageBounds:TextBounds?,val totalLines:Int,val totalGlyphs:Long,
                                 val lines:List<SelectionLineReport>,val truncated:Boolean)
object SelectionLayoutDiagnostics {
    const val DEFAULT_LIMIT=200
    const val MAX_LIMIT=500
    fun build(snapshot:PageTextSnapshot?,rejection:SelectionRejection?,limit:Int=DEFAULT_LIMIT):SelectionLayoutReport {
        require(limit in 1..MAX_LIMIT)
        if(snapshot==null)return SelectionLayoutReport(null,rejection,null,0,0,emptyList(),false)
        val lines=snapshot.lines.take(limit).mapIndexed{index,line ->
            SelectionLineReport(index,line.block,line.run,line.bounds,line.glyphs.size,line.selectable)
        }
        return SelectionLayoutReport(snapshot.status,rejection,snapshot.pageBounds,snapshot.lines.size,
            snapshot.lines.sumOf{it.glyphs.size.toLong()},lines.toList(),snapshot.lines.size>limit)
    }
}
