// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** One committed copied span for the CURRENT snapshot only. Never durable annotation/recovery data. */
class PreviewRetentionState{
    private var owner:PageTextSnapshot?=null
    private var committed:SelectionSpan?=null
    fun commit(snapshot:PageTextSnapshot,span:SelectionSpan){owner=snapshot;committed=span}
    fun restore(snapshot:PageTextSnapshot?):SelectionSpan?=if(snapshot!=null && snapshot===owner)committed else null
    fun clear(){owner=null;committed=null}
}
object PreviewContinuityPolicy{
    fun toolAfterPageChange(current:Int?,sameDocument:Boolean):Int?=
        if(sameDocument && current!=null && current in 0..5)current else null
}
