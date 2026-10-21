// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Isolated synthetic proof: durable source, native load/render, compare and reopen. */
class WorkspaceInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val root = File(targetContext.noBackupFilesDir, "workspace-proof-${UUID.randomUUID()}")
        val source = File(targetContext.cacheDir, "workspace-input-${UUID.randomUUID()}.pdf")
        var failure: Throwable? = null
        try {
            val bytes = fixture()
            source.writeBytes(bytes)
            val store = ReadOnlyWorkspaceStore(root)
            val workspace = store.create("content://synthetic/workspace", "Synthetic workspace.pdf") { source.inputStream() }
            val document = Document.openDocument(workspace.inputFile.absolutePath)
            try {
                check(document.isPDF() && document.countPages() == 1)
                val page = document.loadPage(0)
                try {
                    val bitmap = AndroidDrawDevice.drawPageFit(page, 300, 300)
                    try { check(bitmap.width > 0 && bitmap.height > 0) } finally { bitmap.recycle() }
                } finally { page.destroy() }
            } finally { document.destroy() }
            store.accept(workspace)
            store.release(workspace)
            check(ReadOnlyWorkspaceStore(root).inspect(workspace.directory).baseline == workspace.baseline)
            check(source.readBytes().contentEquals(bytes)) { "Input fixture changed" }
            check(store.compareSource(workspace) { source.inputStream() } == SourceComparison.SAME)
            val altered = bytes.copyOf().also { it[it.lastIndex] = 0 }
            check(store.compareSource(workspace) { ByteArrayInputStream(altered) } == SourceComparison.DIFFERENT)
            check(store.compareSource(workspace) { throw java.io.IOException("Synthetic unavailable") } == SourceComparison.UNAVAILABLE)
            repeat(3) {
                val next = store.create("content://synthetic/$it", null) { source.inputStream() }
                store.accept(next); store.release(next); check(store.pruneReadOnly(next))
            }
            check(root.listFiles().orEmpty().count { it.isDirectory } == 2)
        } catch (error: Throwable) { failure = error }
        finally { source.delete(); root.deleteRecursively() }
        val error = failure
        finish(if (error == null) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("result", if (error == null) "PASS: durable workspace" else "FAIL: durable workspace")
            if (error != null) putString("details", error.stackTraceToString())
        })
    }

    private fun fixture(): ByteArray {
        val content = "BT /F1 14 Tf 50 750 Td (Synthetic durable workspace) Tj ET\n"
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Length ${content.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${content}endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        )
        val bytes = ByteArrayOutputStream()
        fun append(value: String) { bytes.write(value.toByteArray(Charsets.US_ASCII)) }
        append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEachIndexed { index, value -> offsets.add(bytes.size()); append("${index + 1} 0 obj\n$value\nendobj\n") }
        val xref = bytes.size()
        append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { append(it.toString().padStart(10, '0') + " 00000 n \n") }
        append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return bytes.toByteArray()
    }
}
