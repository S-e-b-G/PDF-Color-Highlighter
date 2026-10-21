// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

data class PreviewSelectionRequest(val revision: Long, val snapshot: PageTextSnapshot,
                                   val first: TextPoint, val last: TextPoint, val finished: Boolean)
/** Main-thread scheduler: at most one running calculation and one replaceable pending request. */
class SelectionPreviewQueue {
    companion object {
        const val MAX_VISIBLE_GLYPHS=5000
        fun allows(span: SelectionSpan)=span.glyphs.size<=MAX_VISIBLE_GLYPHS
    }
    private var revision=0L
    private var running: Long?=null
    private var pending: PreviewSelectionRequest?=null
    fun offer(snapshot: PageTextSnapshot, first: TextPoint, last: TextPoint,
              finished: Boolean): PreviewSelectionRequest? {
        revision++
        pending=PreviewSelectionRequest(revision,snapshot,first,last,finished)
        return take()
    }
    fun accepts(request: PreviewSelectionRequest)=request.revision==revision
    fun cancel() { revision++;pending=null }
    fun complete(request: PreviewSelectionRequest): PreviewSelectionRequest? {
        check(running==request.revision) { "Unexpected selection completion" }
        running=null
        return take()
    }
    private fun take(): PreviewSelectionRequest? {
        if(running!=null) return null
        val request=pending ?: return null
        pending=null;running=request.revision
        return request
    }
}
