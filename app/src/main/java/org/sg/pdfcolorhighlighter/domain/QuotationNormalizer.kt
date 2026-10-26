// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.text.Normalizer

/** Normalizes automatic quotations from explicitly identified paragraphs.
 * This class does not infer paragraphs or reading order from arbitrary PDF layouts.
 * Never pass manually edited Contents through this normalizer.
 */
object QuotationNormalizer {
    fun fromParagraphs(paragraphs: List<List<String>>): String = paragraphs.mapNotNull { visualLines ->
        val lines = visualLines.map { line ->
            require('\n' !in line && '\r' !in line) { "Expected individual visual lines" }
            Normalizer.normalize(line.trim { it == ' ' || it == '\t' }, Normalizer.Form.NFC)
        }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) null else lines.drop(1).fold(lines.first(), ::joinLines)
    }.joinToString("\n\n")

    private fun joinLines(left: String, right: String): String {
        val trailingHyphen = left.endsWith('-') || left.endsWith('\u00AD')
        val joinsWord = trailingHyphen && left.length > 1 && right.isNotEmpty() &&
            Character.isLetter(left.codePointBefore(left.length - 1)) &&
            Character.isLetter(right.codePointAt(0))
        return when {
            joinsWord && left.last() == '\u00AD' -> left.dropLast(1) + right
            joinsWord -> left + right // Keep a visible hyphen; do not guess dehyphenation.
            left.endsWith('\u00A0') || left.endsWith('\u202F') ||
                right.startsWith('\u00A0') || right.startsWith('\u202F') -> left + right
            else -> "$left $right"
        }
    }
}
