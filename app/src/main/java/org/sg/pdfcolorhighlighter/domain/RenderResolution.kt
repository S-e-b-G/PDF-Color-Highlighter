// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import kotlin.math.*

data class RenderResolution(val width: Int, val height: Int) {
    val reservedPixels: Long get() = (width.toLong() + 2) * (height.toLong() + 2)
}

/** Whole-page raster tiers; reserve two pixels per dimension for native bounds rounding. */
object RenderResolutionPolicy {
    const val MAX_DIMENSION = 4096
    const val MAX_PIXELS = 8L * 1024 * 1024
    private val tiers = intArrayOf(1600, 2048, 2560, 3072, 3584, 4094)

    fun plan(pageWidth: Float, pageHeight: Float, viewWidth: Int,
             viewHeight: Int, zoom: Float): RenderResolution {
        require(pageWidth.isFinite() && pageWidth > 0f && pageHeight.isFinite() && pageHeight > 0f)
        require(pageWidth / pageHeight in 0.01f..100f)
        require(viewWidth > 0 && viewHeight > 0 && zoom.isFinite() && zoom >= 1f)
        val w = pageWidth.toDouble(); val h = pageHeight.toDouble()
        val wanted = max(w, h) * min(viewWidth / w, viewHeight / h) * zoom.toDouble()
        val tier = tiers.firstOrNull { it.toDouble() >= wanted } ?: tiers.last()
        var high = min(tier / max(w, h), min(4094.0 / w, 4094.0 / h))
        var low = 0.0
        repeat(60) {
            val middle = (low + high) / 2
            val rw = max(1, floor(w * middle).toInt())
            val rh = max(1, floor(h * middle).toInt())
            if ((rw.toLong() + 2) * (rh.toLong() + 2) <= MAX_PIXELS) low = middle else high = middle
        }
        return RenderResolution(max(1, floor(w * low).toInt()), max(1, floor(h * low).toInt()))
    }

    fun needsUpgrade(plan: RenderResolution, currentWidth: Int, currentHeight: Int): Boolean =
        max(plan.width, plan.height).toDouble() > max(currentWidth, currentHeight).toDouble() * 1.08
}

/** Main-thread ticket gate; content, layout and gesture invalidations reject old deliveries. */
class RenderTicketGate {
    private var epoch = 0L
    fun invalidate() { epoch++ }
    fun ticket(): Long = epoch
    fun accepts(ticket: Long): Boolean = ticket == epoch
}
