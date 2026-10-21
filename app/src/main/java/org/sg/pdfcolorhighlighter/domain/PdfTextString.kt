// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Codec for our app-owned quotation/sync hints, not arbitrary imported PDF strings. */
object PdfTextString {
    fun encode(text: String): ByteArray =
        byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + text.toByteArray(Charsets.UTF_16BE)

    fun decodeAppText(bytes: ByteArray): String {
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            require((bytes.size - 2) % 2 == 0) { "Invalid app UTF-16BE text length" }
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        // Compatibility with the unmarked UTF-8 hints in the original synthetic proof.
        return String(bytes, Charsets.UTF_8)
    }
}
