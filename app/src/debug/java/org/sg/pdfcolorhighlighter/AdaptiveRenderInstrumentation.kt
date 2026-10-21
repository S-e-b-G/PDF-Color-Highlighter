// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import com.artifex.mupdf.fitz.Document
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.BoundedPageRenderer
import org.sg.pdfcolorhighlighter.ui.PdfViewportView
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Synthetic resolution and same-page pixel swap; not a hardware-gesture or heap-pressure test. */
class AdaptiveRenderInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val source = File(targetContext.cacheDir, "render-proof-${UUID.randomUUID()}.pdf")
        var preview: Bitmap? = null
        var detail: Bitmap? = null
        var failure: Throwable? = null
        try {
            val bytes = fixture()
            source.writeBytes(bytes)
            val doc = Document.openDocument(source.absolutePath)
            var logicalW = 0f; var logicalH = 0f
            try {
                val page = doc.loadPage(0)
                try {
                    val b = page.bounds
                    logicalW = b.x1 - b.x0; logicalH = b.y1 - b.y0
                    preview = BoundedPageRenderer.render(page,
                        RenderResolutionPolicy.plan(logicalW, logicalH, 600, 900, 1f))
                    detail = BoundedPageRenderer.render(page,
                        RenderResolutionPolicy.plan(logicalW, logicalH, 1200, 900, 4f))
                    check(checkNotNull(detail).width > checkNotNull(preview).width)
                    check(checkNotNull(detail).allocationByteCount.toLong() <= RenderResolutionPolicy.MAX_PIXELS * 4)
                } finally { page.destroy() }
            } finally { doc.destroy() }
            var uiFailure: Throwable? = null
            runOnMainSync {
                try {
                    val view = PdfViewportView(targetContext)
                    view.layout(0, 0, 600, 800)
                    var callbacks = 0
                    var lastState = PageViewportState()
                    view.onViewportChanged = { callbacks++; lastState = it }
                    view.show(checkNotNull(preview), PageViewportState(3f, 0.65f, 0.7f), logicalW, logicalH, 10L)
                    val before = lastState; val count = callbacks
                    view.show(checkNotNull(detail), PageViewportState(), logicalW, logicalH, 10L)
                    check(callbacks == count && lastState == before) { "Refinement changed reading geometry" }
                    check(view.pageNavigationState().zoom == before.zoom)
                    val output = Bitmap.createBitmap(620, 820, Bitmap.Config.ARGB_8888)
                    try {
                        output.eraseColor(Color.MAGENTA)
                        val canvas = Canvas(output)
                        val saved = canvas.save()
                        try { canvas.translate(10f, 10f); view.draw(canvas) }
                        finally { canvas.restoreToCount(saved) }
                        check(output.getPixel(0, 0) == Color.MAGENTA)
                        check(output.getPixel(619, 819) == Color.MAGENTA)
                    } finally { output.recycle() }
                } catch (error: Throwable) { uiFailure = error }
            }
            uiFailure?.let { throw it }
            check(source.readBytes().contentEquals(bytes)) { "Source changed during read-only rendering" }
        } catch (error: Throwable) { failure = error }
        finally { preview?.recycle(); detail?.recycle(); source.delete() }
        val error = failure
        finish(if (error == null) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("result", if (error == null) "PASS: adaptive rendering" else "FAIL: adaptive rendering")
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
