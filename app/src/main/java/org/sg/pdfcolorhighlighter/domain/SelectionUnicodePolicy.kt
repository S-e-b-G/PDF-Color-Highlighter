// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Automatic extraction only. Never guess font mappings or repair manual Contents. */
object SelectionUnicodePolicy {
    fun continuation(cp: Int): Boolean {
        val type = Character.getType(cp)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            cp in 0x1F3FB..0x1F3FF
    }

    fun unsupported(glyph: TextGlyph): Boolean {
        val cp = glyph.codePoint
        return !glyph.unicodeReliable || cp == 0x200D || cp == 0xFFFD ||
            (cp < 32 && cp != 9) || cp == 127 ||
            cp in 0x1F1E6..0x1F1FF
    }

    /** Only inferred spaces can be suppressed, never actual whitespace glyphs. */
    fun attachedContinuation(glyphs: List<TextGlyph>, index: Int): Boolean {
        if (index !in 1 until glyphs.size) return false

        val current = glyphs[index]
        val previous = glyphs[index - 1]

        return current.unicodeReliable && previous.unicodeReliable &&
            continuation(current.codePoint) && !previous.whitespace &&
            (Character.isLetterOrDigit(previous.codePoint) ||
                continuation(previous.codePoint) ||
                previous.codePoint >= 0x1F000)
    }

    fun inferredSpaceBefore(line: PageTextLine, index: Int): Boolean =
        index in line.spacesBefore &&
            !attachedContinuation(line.glyphs, index)

    /** Diagnostic category, not a substitution table. */
    fun spacingAccent(cp: Int): Boolean =
        cp == 0x0060 || cp == 0x00B4 || cp == 0x00A8 ||
            cp == 0x00B8 || cp == 0x02C6 || cp == 0x02C7 ||
            cp == 0x02CA || cp == 0x02CB || cp == 0x02DC

    fun bullet(cp: Int): Boolean =
        cp == 0x2022 || cp == 0x25CF || cp == 0x25E6 ||
            cp == 0x25AA || cp == 0x2023 || cp == 0x2043
}
