// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.StructuredText
import org.sg.pdfcolorhighlighter.domain.*
import kotlin.math.abs

/** Call on the owning serialized native worker with doc.hasPermission(Document.PERMISSION_COPY).
 * The caller owns Page. This extractor owns/destroys StructuredText and returns only copied values.
 * Copied data is normalized into supported physical text rows after native cleanup.
 */
object MuPdfTextSnapshot {
    const val MAX_GLYPHS = 100000

    fun extract(page: Page, copyAllowed: Boolean): PageTextSnapshot {
        val b = page.bounds
        val bounds = TextBounds(b.x0, b.y0, b.x1, b.y1)
        if (!copyAllowed) return PageTextSnapshot(bounds, emptyList(), PageTextStatus.COPY_RESTRICTED)
        val text = page.toStructuredText()
        val snapshot = try {
            val result = mutableListOf<PageTextLine>()
            var total = 0L
            text.blocks.forEachIndexed { blockIndex, block ->
                block.lines.forEach { line ->
                    total += line.chars.size
                    if (total > MAX_GLYPHS) return PageTextSnapshot(bounds, emptyList(), PageTextStatus.TOO_COMPLEX)
                    var reliable = line.dir.x > 0.98f && abs(line.dir.y) < 0.05f
                    val glyphs = line.chars.mapNotNull { char ->
                        if (!Character.isValidCodePoint(char.c) || char.c in 0xD800..0xDFFF) {
                            reliable = false
                            null
                        } else {
                            if (char.bidi % 2 != 0 || (char.flags and
                                (StructuredText.CHAR_FLAGS_UNICODE_IS_CID or StructuredText.CHAR_FLAGS_UNICODE_IS_GID)) != 0) reliable = false
                            val q = char.quad
                            try {
                                TextGlyph(char.c, GlyphQuad(TextPoint(q.ul_x, q.ul_y), TextPoint(q.ur_x, q.ur_y),
                                    TextPoint(q.ll_x, q.ll_y), TextPoint(q.lr_x, q.lr_y)))
                            } catch (_: IllegalArgumentException) { reliable = false; null }
                        }
                    }
                    TextGeometryPolicy.splitRuns(glyphs).forEachIndexed { runIndex, run ->
                        if (run.isNotEmpty()) result.add(PageTextLine(blockIndex, runIndex, run.toList(), reliable))
                    }
                }
            }
            PageTextSnapshot(bounds, result.toList(), if (result.isEmpty()) PageTextStatus.EMPTY else PageTextStatus.READY)
        } finally { text.destroy() }
        return TextRowCoalescer.normalize(snapshot)
    }
}
