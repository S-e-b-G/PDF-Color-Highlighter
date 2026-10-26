// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Structural metadata plus bounded exceptional-symbol identifiers, never ordinary text or font IDs. */
data class SelectionLineReport(val index:Int,val block:Int,val run:Int,val bounds:TextBounds,
                               val glyphCount:Int,val selectable:Boolean,
                               val unreliableGlyphs:Int=0,val unsupportedGlyphs:Int=0,
                               val combiningMarks:Int=0,val spacingAccents:Int=0,
                               val inferredClusterSplits:Int=0,val bulletGlyphs:Int=0,
                               val spacingPairs:Int=0,val exceptionalSymbols:String="")
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
            SelectionLineReport(index,line.block,line.run,line.bounds,line.glyphs.size,line.selectable,
                line.glyphs.count{!it.unicodeReliable},
                line.glyphs.count{SelectionUnicodePolicy.unsupported(it)},
                line.glyphs.count{SelectionUnicodePolicy.continuation(it.codePoint)},
                line.glyphs.count{SelectionUnicodePolicy.spacingAccent(it.codePoint)},
                line.spacesBefore.count{SelectionUnicodePolicy.attachedContinuation(line.glyphs,it)},
                line.glyphs.count{it.unicodeReliable && SelectionUnicodePolicy.bullet(it.codePoint)},
                line.spacingAccents.pairs.size,ExceptionalGlyphDiagnostics.describe(line))
        }
        return SelectionLayoutReport(snapshot.status,rejection,snapshot.pageBounds,snapshot.lines.size,
            snapshot.lines.sumOf{it.glyphs.size.toLong()},lines.toList(),snapshot.lines.size>limit)
    }
}
