// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.data

import android.util.AtomicFile
import org.sg.pdfcolorhighlighter.domain.RecentDocument
import org.sg.pdfcolorhighlighter.domain.RecentDocumentsCodec
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/** One owning I/O worker must serialize all access. */
class RecentDocumentsStore(private val file: File) {
    private val atomic = AtomicFile(file)

    fun read(): List<RecentDocument> {
        val input = try {
            atomic.openRead()
        } catch (error: FileNotFoundException) {
            if (file.exists() || File(file.path + ".bak").exists()) throw error
            return emptyList()
        }
        return input.use { RecentDocumentsCodec.decode(it.readBounded()) }
    }

    fun write(entries: List<RecentDocument>) {
        val bytes = RecentDocumentsCodec.encode(entries)
        val output = atomic.startWrite()
        try {
            output.write(bytes)
            atomic.finishWrite(output)
        } catch (error: Exception) {
            atomic.failWrite(output)
            throw error
        }

        // Some platform failures are logged by AtomicFile instead of thrown.
        check(atomic.openRead().use { it.readBounded() }.contentEquals(bytes)) {
            "Recent snapshot read-back mismatch"
        }
    }

    private fun InputStream.readBounded(): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            check(result.size() + count <= RecentDocumentsCodec.MAX_BYTES) {
                "Recent snapshot too large"
            }
            result.write(buffer, 0, count)
        }
        return result.toByteArray()
    }
}
