// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Native page coordinates <-> physical Android View pixels, independent of raster resolution. */
data class PageCoordinateMap(val page: TextBounds, val left: Float, val top: Float,
                             val width: Float, val height: Float) {
    init {
        require(listOf(left,top,width,height,page.width,page.height).all { it.isFinite() })
        require(width>0f && height>0f && page.width>0f && page.height>0f)
    }
    fun toPage(point: TextPoint)=TextPoint(page.left+(point.x-left)/width*page.width,
        page.top+(point.y-top)/height*page.height)
    fun toView(point: TextPoint)=TextPoint(left+(point.x-page.left)/page.width*width,
        top+(point.y-page.top)/page.height*height)
}
