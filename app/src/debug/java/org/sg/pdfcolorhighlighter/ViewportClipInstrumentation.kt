// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import org.sg.pdfcolorhighlighter.domain.PageViewportState
import org.sg.pdfcolorhighlighter.ui.PdfViewportView

/** Synthetic software-Canvas check; not a Compose screenshot or device gesture test. */
class ViewportClipInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        var failure: Throwable? = null
        runOnMainSync {
            var page: Bitmap? = null
            var output: Bitmap? = null
            try {
                val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).also { page = it }
                source.eraseColor(Color.WHITE)
                val target = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also { output = it }
                target.eraseColor(Color.MAGENTA)
                val view = PdfViewportView(targetContext)
                view.layout(0, 0, 100, 100)
                view.show(source, PageViewportState(4f, 0.5f, 0.5f))
                val canvas = Canvas(target)
                val saved = canvas.save()
                try { canvas.translate(100f, 100f); view.draw(canvas) }
                finally { canvas.restoreToCount(saved) }
                val pixels = IntArray(300 * 300)
                target.getPixels(pixels, 0, 300, 0, 0, 300, 300)
                for (y in 0 until 300) for (x in 0 until 300) {
                    if (x !in 100 until 200 || y !in 100 until 200) {
                        check(pixels[y * 300 + x] == Color.MAGENTA) { "Drawing escaped viewport at $x,$y" }
                    }
                }
                check(target.getPixel(150, 150) == Color.WHITE) { "Page not drawn inside viewport" }
            } catch (error: Throwable) { failure = error }
            finally { page?.recycle(); output?.recycle() }
        }
        val error = failure
        finish(if (error == null) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            Bundle().apply {
                putString("result", if (error == null) "PASS: viewport canvas clipping" else "FAIL: viewport canvas clipping")
                if (error != null) putString("details", error.stackTraceToString())
            })
    }
}
