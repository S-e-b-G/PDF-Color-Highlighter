// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.lang.ref.WeakReference

/** Stateless read-only selection over copied geometry. No native objects or annotation mutations. */
object PageTextSelector {
    private var prepared=WeakReference<PageTextHitIndex>(null)
    @Synchronized private fun indexFor(snapshot:PageTextSnapshot):PageTextHitIndex{
        val existing=prepared.get()
        if(existing!=null && existing.owns(snapshot))return existing
        return PageTextHitIndex(snapshot).also{prepared=WeakReference(it)}
    }
    fun hit(snapshot: PageTextSnapshot, point: TextPoint): TextAddress? {
        if (snapshot.status != PageTextStatus.READY) return null
        return indexFor(snapshot).hit(point) // Exact glyph hits; margin assistance stays in TextFlowResolver.
    }

    private fun statusFailure(snapshot:PageTextSnapshot):TextSelectionAttempt.Rejected? {
        val reason=when {
            snapshot.status==PageTextStatus.COPY_RESTRICTED -> SelectionRejection.COPY_RESTRICTED
            snapshot.status==PageTextStatus.TOO_COMPLEX -> SelectionRejection.TOO_COMPLEX
            snapshot.status==PageTextStatus.EMPTY || snapshot.lines.isEmpty() -> SelectionRejection.NO_TEXT
            else -> null
        }
        return reason?.let{TextSelectionAttempt.Rejected(it)}
    }
    fun select(snapshot: PageTextSnapshot, first: TextPoint, second: TextPoint,
               snapToWords: Boolean = true): TextSelectionAttempt {
        statusFailure(snapshot)?.let{return it}
        val a=TextFlowResolver.endpoint(snapshot,first) ?: return TextSelectionAttempt.Rejected(SelectionRejection.NO_GLYPH)
        val b=TextFlowResolver.endpoint(snapshot,second) ?: return TextSelectionAttempt.Rejected(SelectionRejection.NO_GLYPH)
        return range(snapshot,a,b,snapToWords)
    }
    /** Exact fixed logical endpoint preserves combining/zero-area punctuation at the opposite boundary. */
    fun adjust(snapshot:PageTextSnapshot,moving:TextPoint,fixed:TextAddress):TextSelectionAttempt {
        statusFailure(snapshot)?.let{return it}
        if(fixed.line !in snapshot.lines.indices || fixed.glyph !in snapshot.lines[fixed.line].glyphs.indices)
            return TextSelectionAttempt.Rejected(SelectionRejection.NO_GLYPH)
        val at=TextFlowResolver.endpoint(snapshot,moving) ?: return TextSelectionAttempt.Rejected(SelectionRejection.NO_GLYPH)
        return range(snapshot,at,fixed,false)
    }
    private fun range(snapshot:PageTextSnapshot,a:TextAddress,b:TextAddress,snapToWords:Boolean):TextSelectionAttempt {
        fun reject(reason:SelectionRejection)=TextSelectionAttempt.Rejected(reason)
        val top = if (snapshot.lines[a.line].bounds.top <= snapshot.lines[b.line].bounds.top) a.line else b.line
        val bottom = if (top == a.line) b.line else a.line
        val path = TextFlowResolver.path(snapshot,top,bottom) ?: return reject(SelectionRejection.DIFFERENT_TEXT_FLOW)
        val addresses = path.flatMap { line -> snapshot.lines[line].glyphs.indices.map { TextAddress(line,it) } }
        val ai = addresses.indexOf(a); val bi = addresses.indexOf(b)
        var start = minOf(ai, bi); var end = maxOf(ai, bi)
        fun glyph(index: Int): TextGlyph { val at = addresses[index]; return snapshot.lines[at.line].glyphs[at.glyph] }
        fun boundary(left:Int,right:Int):Boolean = addresses[left].line != addresses[right].line ||
            addresses[right].glyph in snapshot.lines[addresses[right].line].spacesBefore
        while (start <= end && glyph(start).whitespace) start++
        while (end >= start && glyph(end).whitespace) end--
        if (start > end) return reject(SelectionRejection.WHITESPACE)
        val selectedLines = addresses.subList(start,end+1).map { it.line }.distinct()
        val lines = selectedLines.map { snapshot.lines[it] }
        if (lines.any { !it.selectable || it.glyphs.any { g -> g.codePoint == 0x200D || g.codePoint == 0xFFFD || (g.codePoint < 32 && g.codePoint != 9) || g.codePoint == 127 || g.codePoint in 0x1F1E6..0x1F1FF } }) {
            return reject(SelectionRejection.UNSUPPORTED_TEXT)
        }
        if (lines.zipWithNext().any { (left, right) -> !TextGeometryPolicy.sameFlow(left, right) }) {
            return reject(SelectionRejection.DIFFERENT_TEXT_FLOW)
        }
        fun continuation(cp: Int): Boolean {
            val type = Character.getType(cp)
            return type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt() || cp in 0x1F3FB..0x1F3FF
        }
        // Character adjustment must not separate combining marks or emoji modifiers from their base.
        while (start > 0 && !boundary(start-1,start) && continuation(glyph(start).codePoint)) start--
        while (end + 1 < addresses.size && !boundary(end,end+1) && continuation(glyph(end + 1).codePoint)) end++
        fun core(cp: Int) = Character.isLetterOrDigit(cp) || continuation(cp) || cp == '_'.code
        fun word(index: Int): Boolean {
            val cp = glyph(index).codePoint
            if (core(cp)) return true
            return (cp == 39 || cp == 0x2019) && index > 0 && index + 1 < addresses.size &&
                !boundary(index-1,index) && !boundary(index,index+1) &&
                core(glyph(index - 1).codePoint) && core(glyph(index + 1).codePoint)
        }
        if (snapToWords) {
            if (word(start)) while (start > 0 && !boundary(start-1,start) && word(start - 1)) start--
            if (word(end)) while (end + 1 < addresses.size && !boundary(end,end+1) && word(end + 1)) end++
        }
        val selected = (start..end).map { glyph(it) }
        if (selected.none { !it.whitespace && it.quad.area >= 0.000001 }) return reject(SelectionRejection.NO_GEOMETRY)
        val visual = linkedMapOf<Int, StringBuilder>()
        for (index in start..end) {
            val at=addresses[index]
            val row=visual.getOrPut(at.line) { StringBuilder() }
            if (row.isNotEmpty() && at.glyph in snapshot.lines[at.line].spacesBefore) row.append(' ')
            row.append(glyph(index).text)
        }
        val visualLines = visual.values.map { it.toString() }
        return TextSelectionAttempt.Selected(SelectionSpan(addresses[start], addresses[end],
            visualLines.joinToString("\n"), QuotationNormalizer.fromParagraphs(listOf(visualLines)), selected.toList()))
    }
}
