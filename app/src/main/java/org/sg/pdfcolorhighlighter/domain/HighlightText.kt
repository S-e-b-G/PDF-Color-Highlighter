// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

enum class TextMode { AUTOMATIC, MANUAL }

data class HighlightText(
    val extractedQuotation: String,
    val contents: String,
    val mode: TextMode,
) {
    companion object {
        fun created(quotation: String) =
            HighlightText(quotation, quotation, TextMode.AUTOMATIC)
        fun imported(contents: String) =
            HighlightText("", contents, TextMode.MANUAL)
    }
    fun commitContentsEdit(draft: String): HighlightText =
        if (draft == contents) this else copy(contents = draft, mode = TextMode.MANUAL)
    fun boundariesChanged(quotation: String): HighlightText = copy(
        extractedQuotation = quotation,
        contents = if (mode == TextMode.AUTOMATIC) quotation else contents,
    )
    fun replaceWithExtractedQuotation(): HighlightText = copy(
        contents = extractedQuotation, mode = TextMode.AUTOMATIC,
    )
}
