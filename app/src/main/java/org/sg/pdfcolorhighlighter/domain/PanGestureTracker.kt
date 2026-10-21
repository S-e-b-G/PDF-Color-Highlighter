// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Tracks one finger independently of scale-detector state. Coordinates are view pixels. */
class PanGestureTracker {
    var pointerId: Int = -1
        private set
    var hasDragged: Boolean = false
        private set
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f

    fun begin(id: Int, x: Float, y: Float, immediate: Boolean = false) {
        pointerId = id; downX = x; downY = y; lastX = x; lastY = y
        hasDragged = immediate
    }
    fun move(id: Int, x: Float, y: Float, slop: Float): Pair<Float, Float>? {
        if (id != pointerId || pointerId < 0 || !x.isFinite() || !y.isFinite()) return null
        if (!hasDragged) {
            val dx = x - downX; val dy = y - downY
            if (dx * dx + dy * dy <= slop * slop) return null
            hasDragged = true
        }
        val delta = x - lastX to y - lastY
        lastX = x; lastY = y
        return delta
    }
    fun rebase(id: Int, x: Float, y: Float) {
        begin(id, x, y, immediate = true)
    }
    fun end() { pointerId = -1; hasDragged = false }
}
