// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

/** Endpoint assistance is confined to a text-line band on the sheet, never arbitrary blank page space. */
object TextFlowResolver {
    const val MAX_FLOW_LINES = 2048
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

    /** Top-to-bottom continuation graph: source-array order cannot force an unrelated column into a range. */
    fun path(snapshot: PageTextSnapshot, firstLine: Int, lastLine: Int): List<Int>? {
        if (firstLine == lastLine) return listOf(firstLine)
        val bounds = snapshot.lines.map { it.bounds }
        val first = bounds[firstLine]
        val last = bounds[lastLine]
        if (last.top <= first.top + min(first.height,last.height) * 0.35f) return null
        val between = snapshot.lines.indices.filter { index ->
            val top = bounds[index].top
            index != firstLine && top > first.top && top <= last.top + 0.001f
        }
        if (between.size > MAX_FLOW_LINES) return null
        val ordered = between.sortedWith(compareBy<Int> { bounds[it].top }
            .thenBy { bounds[it].left }.thenBy { it })
        val result = mutableListOf(firstLine)
        val visited = mutableSetOf(firstLine)
        while (result.last() != lastLine) {
            val currentIndex = result.last()
            val current = snapshot.lines[currentIndex]
            val currentBounds = bounds[currentIndex]
            val candidates = ordered.asSequence().takeWhile { bounds[it].top <= currentBounds.bottom + currentBounds.height * 5f }
                .filter { index -> index !in visited &&
                    TextGeometryPolicy.sameFlow(current,snapshot.lines[index],currentBounds,bounds[index]) }.toList()
            val next = candidates.firstOrNull() ?: return null
            val nextLine = snapshot.lines[next]
            // Multiple fragments at the next row are ambiguous: don't choose an unrelated branch.
            if (candidates.drop(1).any { other ->
                abs(bounds[other].top - bounds[next].top) <=
                    min(bounds[other].height,bounds[next].height) * 0.45f
            }) return null
            if (nextLine.glyphs.all { it.whitespace }) return null
            result.add(next);visited.add(next)
            if (result.size > MAX_FLOW_LINES) return null
        }
        return result.toList()
    }
}
