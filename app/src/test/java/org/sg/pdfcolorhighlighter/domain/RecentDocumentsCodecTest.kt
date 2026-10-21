// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.security.MessageDigest

class RecentDocumentsCodecTest {
    private val entry = RecentDocument(
        "content://test/one", "Élève — cœur 📚", 12345, 4, 20,
        PageViewportState(12f, -0.25f, 0.8f, true, true, 0.75f)
    )

    private fun rejects(block: () -> Unit) {
        try {
            block()
            fail("Invalid snapshot accepted")
        } catch (_: Exception) { }
    }

    private fun changedHeader(offset: Int, value: Int): ByteArray {
        val bytes = RecentDocumentsCodec.encode(listOf(entry))
        val payload = bytes.copyOfRange(0, bytes.size - 32)
        ByteBuffer.wrap(payload).putInt(offset, value)
        return payload + MessageDigest.getInstance("SHA-256").digest(payload)
    }

    @Test fun emptySnapshotRoundTrips() {
        assertEquals(emptyList<RecentDocument>(),
            RecentDocumentsCodec.decode(RecentDocumentsCodec.encode(emptyList())))
    }

    @Test fun unicodeAndAllViewportHintsRoundTrip() {
        assertEquals(listOf(entry),
            RecentDocumentsCodec.decode(RecentDocumentsCodec.encode(listOf(entry))))
    }

    @Test fun nullFractionAndDefaultFlagsRoundTrip() {
        val item = entry.copy(viewport = PageViewportState())
        assertEquals(listOf(item),
            RecentDocumentsCodec.decode(RecentDocumentsCodec.encode(listOf(item))))
    }

    @Test fun fiftyEntriesRoundTrip() {
        val entries = (0 until 50).map { entry.copy(uri = "content://test/$it") }
        assertEquals(entries,
            RecentDocumentsCodec.decode(RecentDocumentsCodec.encode(entries)))
    }

    @Test fun moreThanFiftyIsRejected() {
        rejects {
            RecentDocumentsCodec.encode(
                (0 until 51).map { entry.copy(uri = "content://test/$it") })
        }
        rejects { RecentDocumentsCodec.decode(changedHeader(8, 51)) }
    }

    @Test fun duplicateIdentityIsRejected() {
        rejects { RecentDocumentsCodec.encode(listOf(entry, entry)) }
    }

    @Test fun checksumDamageIsRejected() {
        val bytes = RecentDocumentsCodec.encode(listOf(entry))
        bytes[15] = (bytes[15].toInt() xor 1).toByte()
        rejects { RecentDocumentsCodec.decode(bytes) }
    }

    @Test fun truncationIsRejected() {
        val bytes = RecentDocumentsCodec.encode(listOf(entry))
        for (size in listOf(0, 8, 43, bytes.size - 1)) {
            rejects { RecentDocumentsCodec.decode(bytes.copyOf(size)) }
        }
    }

    @Test fun unsupportedVersionAndMagicAreRejected() {
        rejects { RecentDocumentsCodec.decode(changedHeader(4, 99)) }
        rejects { RecentDocumentsCodec.decode(changedHeader(0, 0)) }
    }

    @Test fun invalidCountsAndStringLengthsAreRejected() {
        rejects { RecentDocumentsCodec.decode(changedHeader(8, -1)) }
        rejects { RecentDocumentsCodec.decode(changedHeader(12, -1)) }
        rejects { RecentDocumentsCodec.decode(changedHeader(12, Int.MAX_VALUE)) }
        rejects {
            RecentDocumentsCodec.encode(listOf(entry.copy(displayName = "x".repeat(65537))))
        }
    }

    @Test fun invalidLocationsAndNonFiniteValuesAreRejected() {
        rejects { RecentDocumentsCodec.encode(listOf(entry.copy(page = -1))) }
        rejects { RecentDocumentsCodec.encode(listOf(entry.copy(page = 20))) }
        rejects {
            RecentDocumentsCodec.encode(
                listOf(entry.copy(viewport = PageViewportState(Float.NaN))))
        }
        rejects {
            RecentDocumentsCodec.encode(listOf(entry.copy(
                viewport = PageViewportState(centerX = Float.POSITIVE_INFINITY))))
        }
        rejects {
            RecentDocumentsCodec.encode(listOf(entry.copy(
                viewport = PageViewportState(navigationHorizontalFraction = -0.1f))))
        }
    }

    @Test fun trailingPayloadAndMalformedUnicodeAreRejected() {
        val bytes = RecentDocumentsCodec.encode(listOf(entry))
        val payload = bytes.copyOfRange(0, bytes.size - 32) + byteArrayOf(0)
        rejects {
            RecentDocumentsCodec.decode(
                payload + MessageDigest.getInstance("SHA-256").digest(payload))
        }
        rejects {
            RecentDocumentsCodec.encode(listOf(entry.copy(displayName = "\uD800")))
        }
    }
}
