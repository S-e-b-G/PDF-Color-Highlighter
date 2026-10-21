// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.time.YearMonth

data class PdfDateParts(
    val year: Int, val month: Int?, val day: Int?, val hour: Int?, val minute: Int?, val second: Int?,
    val offsetMinutes: Int?,
)

/** Parse recorded precision; unknown/malformed dates return null so the UI can preserve the raw value. */
object PdfDateParser {
    private val pattern = Regex("""^(?:D:)?(\d{4})(\d{2})?(\d{2})?(\d{2})?(\d{2})?(\d{2})?(Z|[+-]\d{2}(?:\d{2}|'\d{2}')?)?$""")
    private val offsetPattern = Regex("""^([+-])(\d{2})(?:(\d{2})|'(\d{2})')?$""")
    fun parse(value: String?): PdfDateParts? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = pattern.matchEntire(raw) ?: return null
        val year = match.groupValues[1].toInt()
        if (year !in 1..9999) return null
        val fields = (2..6).map { match.groupValues[it].takeIf { s -> s.isNotEmpty() }?.toInt() }
        for (index in 1 until fields.size) if (fields[index] != null && fields[index - 1] == null) return null
        val month = fields[0]; val day = fields[1]; val hour = fields[2]
        val minute = fields[3]; val second = fields[4]
        if (month != null && month !in 1..12) return null
        if (day != null && (month == null || day !in 1..YearMonth.of(year, month).lengthOfMonth())) return null
        if (hour != null && hour !in 0..23) return null
        if (minute != null && minute !in 0..59) return null
        if (second != null && second !in 0..59) return null
        val zone = match.groupValues[7]
        val offset = when {
            zone.isEmpty() -> null
            zone == "Z" -> 0
            else -> {
                val z = offsetPattern.matchEntire(zone) ?: return null
                val h = z.groupValues[2].toInt()
                val m = (z.groupValues[3].ifEmpty { z.groupValues[4] }).ifEmpty { "0" }.toInt()
                if (h !in 0..23 || m !in 0..59) return null
                (h * 60 + m) * (if (z.groupValues[1] == "-") -1 else 1)
            }
        }
        return PdfDateParts(year, month, day, hour, minute, second, offset)
    }
}
