// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Stateless read-only selection over copied geometry. No native objects or annotation mutations. */
object PageTextSelector {
    fun hit(snapshot: PageTextSnapshot, point: TextPoint): TextAddress? {
        if (snapshot.status != PageTextStatus.READY) return null
        snapshot.lines.forEachIndexed { line, value ->
            value.glyphs.forEachIndexed { glyph, char ->
                if (char.quad.contains(point)) return TextAddress(line, glyph)
            }
        }
        return null // Never invent a nearest word for a touch in blank page space.
    }

    fun select(snapshot: PageTextSnapshot, first: TextPoint, second: TextPoint,
               snapToWords: Boolean = true): TextSelectionAttempt {
        fun reject(reason: SelectionRejection) = TextSelectionAttempt.Rejected(reason)
        when (snapshot.status) {
            PageTextStatus.COPY_RESTRICTED -> return reject(SelectionRejection.COPY_RESTRICTED)
            PageTextStatus.TOO_COMPLEX -> return reject(SelectionRejection.TOO_COMPLEX)
            PageTextStatus.EMPTY -> return reject(SelectionRejection.NO_TEXT)
            PageTextStatus.READY -> Unit
        }
        if (snapshot.lines.isEmpty()) return reject(SelectionRejection.NO_TEXT)
        val a = hit(snapshot, first) ?: return reject(SelectionRejection.NO_GLYPH)
        val b = hit(snapshot, second) ?: return reject(SelectionRejection.NO_GLYPH)
        val addresses = snapshot.lines.flatMapIndexed { line, value -> value.glyphs.indices.map { TextAddress(line, it) } }
        val ai = addresses.indexOf(a); val bi = addresses.indexOf(b)
        var start = minOf(ai, bi); var end = maxOf(ai, bi)
        fun glyph(index: Int): TextGlyph { val at = addresses[index]; return snapshot.lines[at.line].glyphs[at.glyph] }
        while (start <= end && glyph(start).whitespace) start++
        while (end >= start && glyph(end).whitespace) end--
        if (start > end) return reject(SelectionRejection.WHITESPACE)
        val firstLine = addresses[start].line; val lastLine = addresses[end].line
        val lines = snapshot.lines.subList(firstLine, lastLine + 1)
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
        while (start > 0 && addresses[start - 1].line == addresses[start].line && continuation(glyph(start).codePoint)) start--
        while (end + 1 < addresses.size && addresses[end + 1].line == addresses[end].line && continuation(glyph(end + 1).codePoint)) end++
        fun core(cp: Int) = Character.isLetterOrDigit(cp) || continuation(cp) || cp == '_'.code
        fun word(index: Int): Boolean {
            val cp = glyph(index).codePoint
            if (core(cp)) return true
            return (cp == 39 || cp == 0x2019) && index > 0 && index + 1 < addresses.size &&
                addresses[index - 1].line == addresses[index].line && addresses[index + 1].line == addresses[index].line &&
                core(glyph(index - 1).codePoint) && core(glyph(index + 1).codePoint)
        }
        if (snapToWords) {
            if (word(start)) while (start > 0 && addresses[start - 1].line == addresses[start].line && word(start - 1)) start--
            if (word(end)) while (end + 1 < addresses.size && addresses[end + 1].line == addresses[end].line && word(end + 1)) end++
        }
        val selected = (start..end).map { glyph(it) }
        if (selected.none { !it.whitespace && it.quad.area >= 0.000001 }) return reject(SelectionRejection.NO_GEOMETRY)
        val visual = linkedMapOf<Int, StringBuilder>()
        for (index in start..end) visual.getOrPut(addresses[index].line) { StringBuilder() }.append(glyph(index).text)
        val visualLines = visual.values.map { it.toString() }
        return TextSelectionAttempt.Selected(SelectionSpan(addresses[start], addresses[end],
            visualLines.joinToString("\n"), QuotationNormalizer.fromParagraphs(listOf(visualLines)), selected.toList()))
    }
}
