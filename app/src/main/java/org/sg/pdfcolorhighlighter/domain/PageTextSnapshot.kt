// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

data class TextPoint(val x: Float, val y: Float) {
    init { require(x.isFinite() && y.isFinite()) }
}
data class TextBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init { require(listOf(left, top, right, bottom).all { it.isFinite() }); require(right >= left && bottom >= top) }
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
/** Clockwise/counterclockwise convex quadrilateral, stored in MuPDF UL,UR,LL,LR order. */
data class GlyphQuad(val ul: TextPoint, val ur: TextPoint, val ll: TextPoint, val lr: TextPoint) {
    private val polygon get() = listOf(ul, ur, lr, ll)
    val bounds: TextBounds = run {
        val p = polygon
        TextBounds(p.minOf { it.x }, p.minOf { it.y }, p.maxOf { it.x }, p.maxOf { it.y })
    }
    val area: Double = run {
        val p = polygon
        abs(p.indices.sumOf { i ->
            val a = p[i]; val b = p[(i + 1) % 4]
            a.x.toDouble() * b.y - a.y.toDouble() * b.x
        }) / 2.0
    }
    fun contains(point: TextPoint): Boolean {
        if (area < 0.000001) return false
        val p = polygon
        val crosses = p.indices.map { i ->
            val a = p[i]; val b = p[(i + 1) % 4]
            (b.x.toDouble() - a.x) * (point.y.toDouble() - a.y) -
                (b.y.toDouble() - a.y) * (point.x.toDouble() - a.x)
        }
        return crosses.all { it >= -0.00001 } || crosses.all { it <= 0.00001 }
    }
}

data class TextGlyph(val codePoint: Int, val quad: GlyphQuad, val unicodeReliable: Boolean = true,
                     val synthetic:Boolean = false, val sourceCodePoint:Int = codePoint) {
    init { require(Character.isValidCodePoint(codePoint) && codePoint !in 0xD800..0xDFFF) }
    init { require(Character.isValidCodePoint(sourceCodePoint) && sourceCodePoint !in 0xD800..0xDFFF) }
    val text: String = String(Character.toChars(codePoint))
    val whitespace: Boolean = Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
}
data class PageTextLine(val block: Int, val run: Int, val glyphs: List<TextGlyph>, val selectable: Boolean = true,
                        val spacesBefore: Set<Int> = emptySet(), val sourceFragments: Int = 1,
                        val horizontal:Boolean = true) {
    init {
        require(glyphs.isNotEmpty() && sourceFragments >= 1)
        require(spacesBefore.all { it > 0 && it < glyphs.size })
    }
    val spacingAccents:SpacingAccentLayout by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SpacingAccentPolicy.prepare(glyphs)
    }
    val text: String get() = buildString {
        glyphs.forEachIndexed { index,glyph -> if (SelectionUnicodePolicy.inferredSpaceBefore(this@PageTextLine, index)) append(' ');append(glyph.text) }
    }
    val bounds: TextBounds = TextBounds(glyphs.minOf { it.quad.bounds.left }, glyphs.minOf { it.quad.bounds.top },
        glyphs.maxOf { it.quad.bounds.right }, glyphs.maxOf { it.quad.bounds.bottom })
}
enum class PageTextStatus { READY, EMPTY, COPY_RESTRICTED, TOO_COMPLEX }
data class PageTextSnapshot(val pageBounds: TextBounds, val lines: List<PageTextLine>, val status: PageTextStatus)
data class TextAddress(val line: Int, val glyph: Int)
data class SelectionSpan(val start: TextAddress, val end: TextAddress, val visualText: String,
                         val quotation: String, val glyphs: List<TextGlyph>) {
    val quads: List<GlyphQuad> get() = glyphs.map { it.quad }.filter { it.area >= 0.000001 }
}
enum class SelectionRejection { NO_TEXT, COPY_RESTRICTED, TOO_COMPLEX, NO_GLYPH, WHITESPACE,
    UNSUPPORTED_TEXT, DIFFERENT_TEXT_FLOW, NO_GEOMETRY }
sealed interface TextSelectionAttempt {
    data class Selected(val span: SelectionSpan) : TextSelectionAttempt
    data class Rejected(val reason: SelectionRejection) : TextSelectionAttempt
}

object TextGeometryPolicy {
    /** Conservative inline column split. This is not general layout/paragraph inference. */
    fun splitRuns(glyphs: List<TextGlyph>): List<List<TextGlyph>> {
        if (glyphs.isEmpty()) return emptyList()
        val height = glyphs.map { it.quad.bounds.height }.filter { it > 0f }.sorted()
            .let { if (it.isEmpty()) 1f else it[it.size / 2] }
        val threshold = max(12f, height * 2.5f)
        val runs = mutableListOf<MutableList<TextGlyph>>()
        var run = mutableListOf<TextGlyph>()
        for (glyph in glyphs) {
            val previous = run.lastOrNull()
            val wideSpace = glyph.whitespace && glyph.quad.bounds.width > threshold
            val gap = previous != null && glyph.quad.bounds.left - previous.quad.bounds.right > threshold
            if ((wideSpace || gap) && run.isNotEmpty()) { runs.add(run); run = mutableListOf() }
            if (!wideSpace) run.add(glyph)
        }
        if (run.isNotEmpty()) runs.add(run)
        return runs.map { it.toList() }
    }

    /** Native IDs are hints, not paragraph boundaries. Crossing IDs uses a tighter gap. */
    fun sameFlow(previous: PageTextLine, next: PageTextLine,
                 a: TextBounds = previous.bounds, b: TextBounds = next.bounds): Boolean {
        if (!a.width.isFinite() || !b.width.isFinite() || a.width <= 0f || b.width <= 0f || a.height <= 0f || b.height <= 0f) return false
        val height = max(a.height, b.height); val small = min(a.height, b.height)
        if (!height.isFinite() || height / small > 1.75f) return false
        if (b.top <= a.top + small * 0.35f) return false
        val sameNativeRun = previous.block == next.block && previous.run == next.run
        val gapLimit = height * (if (sameNativeRun) 2.5f else 1.25f)
        if (b.top - a.bottom > gapLimit) return false
        val overlap = min(a.right, b.right) - max(a.left, b.left)
        return overlap >= min(a.width, b.width) * 0.2f || abs(a.left - b.left) <= height * 1.5f
    }
}
