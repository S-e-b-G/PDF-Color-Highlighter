// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

data class RecentDocument(
    val uri: String,
    val displayName: String,
    val lastOpenedMillis: Long,
    val page: Int,
    val pageCount: Int,
    val viewport: PageViewportState = PageViewportState(),
)

object RecentDocuments {
    const val LIMIT = 50

    fun opened(entries: List<RecentDocument>, entry: RecentDocument): List<RecentDocument> =
        (listOf(entry) + entries.filterNot { it.uri == entry.uri }).take(LIMIT)

    fun location(
        entries: List<RecentDocument>,
        uri: String,
        page: Int,
        viewport: PageViewportState,
    ): List<RecentDocument> = entries.map {
        if (it.uri == uri) {
            it.copy(page = page.coerceIn(0, it.pageCount - 1), viewport = viewport)
        } else it
    }

    fun remove(entries: List<RecentDocument>, uri: String): List<RecentDocument> =
        entries.filterNot { it.uri == uri }
}

/** Reading metadata only; never PDF contents, annotations or recovery data. */
object RecentDocumentsCodec {
    const val MAX_BYTES = 8 * 1024 * 1024
    private const val MAGIC = 0x53475244
    private const val VERSION = 1
    private const val MAX_STRING_BYTES = 65536

    fun encode(entries: List<RecentDocument>): ByteArray {
        require(entries.size <= RecentDocuments.LIMIT)
        require(entries.map { it.uri }.distinct().size == entries.size)

        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(VERSION)
            out.writeInt(entries.size)
            entries.forEach { entry ->
                validate(entry)
                writeString(out, entry.uri)
                writeString(out, entry.displayName)
                out.writeLong(entry.lastOpenedMillis)
                out.writeInt(entry.page)
                out.writeInt(entry.pageCount)
                val v = entry.viewport
                out.writeFloat(v.zoom)
                out.writeFloat(v.centerX)
                out.writeFloat(v.centerY)
                out.writeBoolean(v.freeHorizontalPan)
                out.writeBoolean(v.startAtTop)
                out.writeBoolean(v.navigationHorizontalFraction != null)
                v.navigationHorizontalFraction?.let { out.writeFloat(it) }
            }
        }
        val payload = buffer.toByteArray()
        require(payload.size + 32 <= MAX_BYTES)
        return payload + MessageDigest.getInstance("SHA-256").digest(payload)
    }

    fun decode(bytes: ByteArray): List<RecentDocument> {
        require(bytes.size in 44..MAX_BYTES)
        val payload = bytes.copyOfRange(0, bytes.size - 32)
        val expected = MessageDigest.getInstance("SHA-256").digest(payload)
        require(MessageDigest.isEqual(expected, bytes.copyOfRange(bytes.size - 32, bytes.size))) {
            "Recent snapshot checksum"
        }

        return DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readInt() == MAGIC) { "Recent snapshot magic" }
            require(input.readInt() == VERSION) { "Unsupported recent snapshot version" }
            val count = input.readInt()
            require(count in 0..RecentDocuments.LIMIT)

            val entries = List(count) {
                val uri = readString(input)
                val name = readString(input)
                val opened = input.readLong()
                val page = input.readInt()
                val pages = input.readInt()
                val zoom = input.readFloat()
                val x = input.readFloat()
                val y = input.readFloat()
                val free = input.readBoolean()
                val top = input.readBoolean()
                val fraction = if (input.readBoolean()) input.readFloat() else null
                RecentDocument(
                    uri, name, opened, page, pages,
                    PageViewportState(zoom, x, y, free, top, fraction),
                ).also { validate(it) }
            }
            require(entries.map { it.uri }.distinct().size == entries.size)
            require(input.available() == 0) { "Trailing recent snapshot data" }
            entries
        }
    }

    private fun validate(entry: RecentDocument) {
        require(entry.uri.isNotBlank() && entry.displayName.isNotBlank())
        require(entry.lastOpenedMillis >= 0)
        require(entry.pageCount > 0 && entry.page in 0 until entry.pageCount)
        val v = entry.viewport
        require(v.zoom.isFinite() && v.zoom >= 1f)
        require(v.centerX.isFinite())
        require(v.centerY.isFinite() && v.centerY in 0f..1f)
        require(
            v.navigationHorizontalFraction == null ||
                (v.navigationHorizontalFraction.isFinite() &&
                    v.navigationHorizontalFraction in 0f..1f)
        )
    }

    private fun writeString(out: DataOutputStream, value: String) {
        val encoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
        val encoded = encoder.encode(CharBuffer.wrap(value))
        require(encoded.remaining() <= MAX_STRING_BYTES)
        val bytes = ByteArray(encoded.remaining()).also { encoded.get(it) }
        out.writeInt(bytes.size)
        out.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val size = input.readInt()
        require(size in 0..MAX_STRING_BYTES && size <= input.available())
        val bytes = ByteArray(size).also { input.readFully(it) }
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    }
}
