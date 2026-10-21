// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class PageJumpInputTest {
    @Test fun acceptsFirstAndLastPage() {
        assertEquals(1, PageJumpInput.parse("1", 21))
        assertEquals(21, PageJumpInput.parse("21", 21))
    }
    @Test fun trimsOuterWhitespaceAndAcceptsLeadingZeros() {
        assertEquals(2, PageJumpInput.parse(" 002 ", 21))
    }
    @Test fun rejectsOutOfRangePages() {
        listOf("0", "-1", "22").forEach { assertNull(PageJumpInput.parse(it, 21)) }
    }
    @Test fun rejectsEmptyAndNonnumericInput() {
        listOf("", " ", "abc", "1.5", "+2", "1e1").forEach { assertNull(PageJumpInput.parse(it, 21)) }
    }
    @Test fun rejectsIntegerOverflow() {
        assertNull(PageJumpInput.parse("2147483648", Int.MAX_VALUE))
    }
    @Test fun acceptsMaximumIntWhenDocumentHasThatManyPages() {
        assertEquals(Int.MAX_VALUE, PageJumpInput.parse("2147483647", Int.MAX_VALUE))
    }
    @Test fun requiresPositiveDocumentPageCount() {
        assertNull(PageJumpInput.parse("1", 0))
        assertNull(PageJumpInput.parse("1", -1))
    }
}
