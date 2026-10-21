// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

object PageJumpInput {
    /** Returns a validated 1-based page number, or null. Never silently clamps user input. */
    fun parse(text: String, total: Int): Int? {
        if (total < 1) return null
        val value = text.trim()
        if (value.isEmpty() || value.any { it !in '0'..'9' }) return null
        return value.toIntOrNull()?.takeIf { it in 1..total }
    }
}
