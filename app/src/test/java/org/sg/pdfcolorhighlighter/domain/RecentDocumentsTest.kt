// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class RecentDocumentsTest {
    private fun entry(id: Int) =
        RecentDocument("content://test/$id", "Document $id", id.toLong(), 0, 10)

    @Test fun openingPlacesEntryFirst() {
        assertEquals(listOf(entry(2), entry(1)),
            RecentDocuments.opened(listOf(entry(1)), entry(2)))
    }

    @Test fun reopeningDeduplicatesByExactUri() {
        val updated = entry(1).copy(displayName = "New name", lastOpenedMillis = 10)
        assertEquals(listOf(updated, entry(2)),
            RecentDocuments.opened(listOf(entry(2), entry(1)), updated))
    }

    @Test fun openingEvictsOldestAtFifty() {
        val result = RecentDocuments.opened((49 downTo 0).map { entry(it) }, entry(50))
        assertEquals(50, result.size)
        assertEquals(entry(50), result.first())
        assertEquals(entry(1), result.last())
    }

    @Test fun checkpointDoesNotChangeOrderOrOpenedTime() {
        val entries = listOf(entry(2), entry(1))
        val v = PageViewportState(3f, 0.7f, 0.8f, true, false, 0.2f)
        val result = RecentDocuments.location(entries, entry(1).uri, 5, v)
        assertEquals(entries.map { it.uri }, result.map { it.uri })
        assertEquals(1L, result.last().lastOpenedMillis)
        assertEquals(v, result.last().viewport)
        assertEquals(5, result.last().page)
    }

    @Test fun checkpointUnknownUriDoesNotCreateEntry() {
        assertEquals(listOf(entry(1)),
            RecentDocuments.location(listOf(entry(1)), "missing", 2, PageViewportState()))
    }

    @Test fun checkpointClampsPage() {
        assertEquals(9, RecentDocuments.location(
            listOf(entry(1)), entry(1).uri, 100, PageViewportState()).single().page)
        assertEquals(0, RecentDocuments.location(
            listOf(entry(1)), entry(1).uri, -1, PageViewportState()).single().page)
    }

    @Test fun removingOnlyTouchesExactUri() {
        assertEquals(listOf(entry(2)),
            RecentDocuments.remove(listOf(entry(1), entry(2)), entry(1).uri))
    }

    @Test fun repeatedOpensNeverDuplicateOrGrow() {
        var entries = emptyList<RecentDocument>()
        repeat(200) { entries = RecentDocuments.opened(entries, entry(it % 51)) }
        assertEquals(50, entries.size)
        assertEquals(50, entries.map { it.uri }.distinct().size)
    }
}
