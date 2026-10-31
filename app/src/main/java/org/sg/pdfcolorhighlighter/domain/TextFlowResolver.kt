// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

/** Endpoint assistance is confined to a text-line band on the sheet, never arbitrary blank page space. */
object TextFlowResolver {
    const val MAX_FLOW_LINES = ColumnFlowRows.MAX_LINES
    private data class Candidate(val index: Int, val score: Double)

    fun endpoint(snapshot: PageTextSnapshot, point: TextPoint): TextAddress? {
        if (snapshot.status != PageTextStatus.READY) return null
        val page = snapshot.pageBounds
        if (point.x < page.left || point.x > page.right || point.y < page.top || point.y > page.bottom) return null
        PageTextSelector.hit(snapshot, point)?.let { return it }
        val candidates = snapshot.lines.mapIndexedNotNull { index, line ->
            if (line.glyphs.none { !it.whitespace }) return@mapIndexedNotNull null
            val b = line.bounds
            if (!b.width.isFinite() || b.width <= 0f || !b.height.isFinite() || b.height <= 0f) return@mapIndexedNotNull null
            val dy = max(0f, max(b.top - point.y, point.y - b.bottom))
            if (dy > min(5f, b.height * 0.35f)) return@mapIndexedNotNull null
            val dx = max(0f, max(b.left - point.x, point.x - b.right))
            if (b.right < page.left || b.left > page.right) return@mapIndexedNotNull null
            val center = (b.top.toDouble() + b.bottom) / 2
            val cy = point.y - center
            val score = dx.toDouble() * dx + cy * cy * 0.25
            if (!score.isFinite()) null else Candidate(index,score)
        }.sortedWith(compareBy<Candidate> { it.score }.thenBy { it.index })
        val chosen = candidates.firstOrNull() ?: return null
        if (candidates.size > 1 && abs(candidates[1].score - chosen.score) <= 0.0001) return null
        val glyphs = snapshot.lines[chosen.index].glyphs
        val left = glyphs.indexOfFirst { !it.whitespace }
        val right = glyphs.indexOfLast { !it.whitespace }
        if (point.x <= glyphs[left].quad.bounds.left) return TextAddress(chosen.index, left)
        if (point.x >= glyphs[right].quad.bounds.right) return TextAddress(chosen.index, right)
        val nearest = glyphs.indices.minByOrNull { index ->
            val b = glyphs[index].quad.bounds
            max(0f, max(b.left - point.x, point.x - b.right))
        } ?: return null
        return TextAddress(chosen.index, nearest)
    }

    /** Paragraphs, native IDs, font changes and vertical gaps are not selection boundaries. */
    fun path(snapshot:PageTextSnapshot,firstLine:Int,lastLine:Int):List<Int>? =
        ColumnFlowRows.path(snapshot,firstLine,lastLine)
}
