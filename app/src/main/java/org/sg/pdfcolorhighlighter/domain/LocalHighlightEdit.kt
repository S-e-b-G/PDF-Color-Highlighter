// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.security.MessageDigest
import java.util.UUID

enum class LocalEditNotice { NONE, OTHER_SOURCE_VERSION, RECOVERY_UNVERIFIED }
data class HighlightEdit(val id:String,val page:Int,val slot:Int,val quotation:String,val quads:List<GlyphQuad>) {
    init {
        require(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(id))
        require(page>=0 && slot in 0..5 && quotation.isNotBlank() && quotation.length<=100000)
        require(quads.size in 1..25000 && quads.all{it.area>=.000001})
    }
    companion object {
        fun from(page:Int,slot:Int,span:SelectionSpan)=HighlightEdit(UUID.randomUUID().toString(),page,slot,span.quotation,span.quads.toList())
    }
}
object LocalEditIdentity {
    fun uriKey(uri:String):String {
        require(uri.isNotBlank())
        return MessageDigest.getInstance("SHA-256").digest(uri.toByteArray(Charsets.UTF_8))
            .joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
    }
    fun key(uri:String,baselineHash:String):String {
        require(Regex("[0-9a-f]{64}").matches(baselineHash))
        return uriKey(uri)+"-"+baselineHash
    }
}
/** In-memory creation history only; bounded by real quad count, not an unbounded native object list. */
class LocalHighlightHistory(private val quadBudget:Int=50000) {
    init { require(quadBudget>0) }
    private fun weight(edit:HighlightEdit)=edit.quads.size.toLong()+edit.quotation.length
    private val done=mutableListOf<HighlightEdit>()
    private val undone=mutableListOf<HighlightEdit>()
    val undo:HighlightEdit? get()=done.lastOrNull()
    val redo:HighlightEdit? get()=undone.lastOrNull()
    fun contains(id:String)=done.any{it.id==id}
    fun added(edit:HighlightEdit){
        if(contains(edit.id))return
        done.add(edit);undone.clear()
        while(done.size>1 && done.sumOf{weight(it)}>quadBudget.toLong())done.removeAt(0)
    }
    fun didUndo(edit:HighlightEdit){check(undo==edit);done.removeAt(done.lastIndex);undone.add(edit)}
    fun didRedo(edit:HighlightEdit){check(redo==edit);undone.removeAt(undone.lastIndex);done.add(edit)}
}
