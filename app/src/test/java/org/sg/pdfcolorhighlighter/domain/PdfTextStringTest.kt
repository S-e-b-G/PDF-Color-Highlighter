// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class PdfTextStringTest {
    @Test fun encodedTextHasUtf16BigEndianMarker() {
        assertArrayEquals(byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0, 65), PdfTextString.encode("A"))
    }
    @Test fun frenchAccentsAndPunctuationRoundTrip() {
        val value = "élève, été, cœur — naïveté"
        assertEquals(value, PdfTextString.decodeAppText(PdfTextString.encode(value)))
    }
    @Test fun supplementaryCharacterRoundTrips() {
        val value = "Test \uD83D\uDE00"
        assertEquals(value, PdfTextString.decodeAppText(PdfTextString.encode(value)))
    }
    @Test fun emptyTextRoundTrips() {
        assertEquals("", PdfTextString.decodeAppText(PdfTextString.encode("")))
    }
    @Test fun readsEarlierFixtureUtf8Hints() {
        val value = "élève, cœur"
        assertEquals(value, PdfTextString.decodeAppText(value.toByteArray(Charsets.UTF_8)))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsOddUtf16ByteCount() {
        PdfTextString.decodeAppText(byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0))
    }
}
