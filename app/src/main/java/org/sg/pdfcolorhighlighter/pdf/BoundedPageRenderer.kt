// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import android.graphics.Bitmap
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.RectI
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import org.sg.pdfcolorhighlighter.domain.RenderResolution
import org.sg.pdfcolorhighlighter.domain.RenderResolutionPolicy

/** Serialized native owner only. Validate actual transformed bounds BEFORE allocation. */
object BoundedPageRenderer {
    fun render(page: Page, target: RenderResolution): Bitmap {
        require(target.width in 1..4094 && target.height in 1..4094)
        require(target.reservedPixels <= RenderResolutionPolicy.MAX_PIXELS)
        val transform = AndroidDrawDevice.fitPage(page, target.width, target.height)
        val box = RectI(page.bounds.transform(transform))
        val width = box.x1.toLong() - box.x0.toLong()
        val height = box.y1.toLong() - box.y0.toLong()
        require(width in 1L..4096L && height in 1L..4096L)
        require(width * height <= RenderResolutionPolicy.MAX_PIXELS)
        val bitmap = AndroidDrawDevice.drawPage(page, transform)
        if (bitmap.width.toLong() != width || bitmap.height.toLong() != height) {
            bitmap.recycle()
            error("Native render bounds changed unexpectedly")
        }
        return bitmap
    }
}
