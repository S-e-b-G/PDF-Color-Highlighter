// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import java.io.File
import java.util.UUID
import org.sg.pdfcolorhighlighter.domain.*

/** Worker-confined editing backend. Source fingerprint separates externally changed originals.
 * Reopening the SAME URI/bytes recovers committed native highlights. No automatic adoption of
 * another source revision or deletion of unverified recovery material. Undo/redo is session-only.
 */
class LocalHighlightWorkspace(root:File,uri:String,private val baseline:File,private val baselineHash:String) {
    private val directory=File(root,LocalEditIdentity.key(uri,baselineHash))
    private val uriPrefix=LocalEditIdentity.uriKey(uri)+"-"
    private val otherVersions=root.listFiles().orEmpty().any{it.isDirectory && Regex("[0-9a-f]{64}-[0-9a-f]{64}").matches(it.name) && it.name.startsWith(uriPrefix) && it.name!=directory.name}
    private val store=DurablePdfRevisionStore(directory)
    private var revision:PdfRevision?=null
    private val history=LocalHighlightHistory()
    var notice:LocalEditNotice=LocalEditNotice.NONE
        private set
    val sequence:Long get()=revision?.sequence ?: 0L
    val canUndo:Boolean get()=history.undo!=null
    val canRedo:Boolean get()=history.redo!=null
    fun recoverExisting():File? {
        if(!directory.exists()){
            if(otherVersions)notice=LocalEditNotice.OTHER_SOURCE_VERSION
            return null
        }
        try {
            val head=store.recover()
            require(head.baselineSha256==baselineHash){"Source/checkpoint fingerprint mismatch"}
            NativePdfEditTransaction.validateEditable(store.currentFile(head))
            revision=head;return store.currentFile(head)
        }catch(error:Exception){notice=LocalEditNotice.RECOVERY_UNVERIFIED;throw error}
    }
    private fun ready():PdfRevision {
        check(notice==LocalEditNotice.NONE){"Local recovery/source revision needs review before editing"}
        revision?.let{return it}
        if(directory.exists()){
            recoverExisting()
            return checkNotNull(revision)
        }
        val head=store.initialize(baseline,NativePdfEditTransaction::validateEditable)
        require(head.baselineSha256==baselineHash)
        revision=head;return head
    }
    private fun transact(operation:String,mutation:(com.artifex.mupdf.fitz.PDFDocument)->Unit){
        val expected=ready()
        val committed=try{NativePdfEditTransaction.commit(store,expected,operation,mutation)}catch(error:Exception){
            // Resolve a possible published-but-unacknowledged commit before reporting failure.
            val recovered=runCatching{store.recover()}.getOrNull()
            if(recovered?.operationId==operation)recovered else throw error
        }
        revision=committed
    }
    fun add(edit:HighlightEdit){
        if(history.contains(edit.id))return
        transact(edit.id){NativeHighlightEdits.add(it,edit)}
        history.added(edit)
    }
    fun undo(){
        val edit=checkNotNull(history.undo)
        transact(UUID.randomUUID().toString()){NativeHighlightEdits.remove(it,edit)}
        history.didUndo(edit)
    }
    fun redo(){
        val edit=checkNotNull(history.redo)
        transact(UUID.randomUUID().toString()){NativeHighlightEdits.add(it,edit)}
        history.didRedo(edit)
    }
    fun currentFile():File=store.currentFile(checkNotNull(revision))
    /** Only after the reader has swapped to the new head and closed its old native document. */
    fun maintain(){runCatching{store.pruneOld()}}
}
