// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class PdfDateParserTest {
    @Test fun parsesFullPositiveQuotedTimezone() {
        assertEquals(PdfDateParts(2026,10,18,11,50,23,150), PdfDateParser.parse("D:20261018115023+02'30'"))
    }
    @Test fun parsesUtc() { assertEquals(0, PdfDateParser.parse("D:20261018115023Z")?.offsetMinutes) }
    @Test fun parsesNegativeUnquotedTimezone() {
        assertEquals(-330, PdfDateParser.parse("D:20261018115023-0530")?.offsetMinutes)
    }
    @Test fun preservesYearOnlyPrecision() {
        assertEquals(PdfDateParts(2026,null,null,null,null,null,null), PdfDateParser.parse("D:2026"))
    }
    @Test fun acceptsPrefixOmissionAndPartialTime() {
        assertEquals(PdfDateParts(2026,10,18,11,null,null,null), PdfDateParser.parse("2026101811"))
    }
    @Test fun validatesLeapDay() {
        assertNotNull(PdfDateParser.parse("D:20240229"))
        assertNull(PdfDateParser.parse("D:20230229"))
    }
    @Test fun rejectsInvalidCalendarAndClockValues() {
        listOf("D:0000","D:20261301","D:20260431","D:20260101240000","D:20260101126000","D:20260101120060")
            .forEach { assertNull(PdfDateParser.parse(it)) }
    }
    @Test fun rejectsInvalidOrMalformedTimezone() {
        listOf("D:20261018+24'00'","D:20261018+02'60'","D:20261018+02'30","D:20261018+0")
            .forEach { assertNull(PdfDateParser.parse(it)) }
    }
    @Test fun missingOrUnsupportedValueReturnsNull() {
        listOf(null,""," ","not a date","2026-10-18T11:00:00Z").forEach { assertNull(PdfDateParser.parse(it)) }
    }
    @Test fun supportsHourOnlyTimezoneOffset() {
        assertEquals(120, PdfDateParser.parse("D:20261018+02")?.offsetMinutes)
    }
}
