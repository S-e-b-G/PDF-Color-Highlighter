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
    private val history=CheckpointHistory()
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

    private fun remember(action:()->Unit){
        val head=ready();val before=currentFile()
        action()
        history.added(CheckpointChange(before,currentFile(),head.sha256,checkNotNull(revision).sha256))
    }
    fun add(edit:HighlightEdit){
        ready()
        val pdf=com.artifex.mupdf.fitz.Document.openDocument(currentFile().absolutePath)
        val exists=try{NativeHighlightEdits.list(pdf as com.artifex.mupdf.fitz.PDFDocument,edit.page).any{it.edit.id==edit.id}}finally{pdf.destroy()}
        if(exists)return
        remember{transact(edit.id){NativeHighlightEdits.add(it,edit)}}
    }
    fun replace(before:ManagedHighlight,after:ManagedHighlight?,operation:String){
        remember{transact(operation){NativeHighlightEdits.replace(it,before,after)}}
    }

    /** One checkpoint and one Undo command for the explicit app-owned page appearance repair. */
    fun replaceMany(changes:List<Pair<ManagedHighlight,ManagedHighlight>>,operation:String){
        if(changes.isEmpty())return
        require(changes.map{it.first.edit.id}.distinct().size==changes.size)
        require(changes.all{it.first.edit.page==changes.first().first.edit.page &&
            it.first.edit.id==it.second.edit.id && it.first.edit.quads==it.second.edit.quads &&
            it.first.contents==it.second.contents && it.first.mode==it.second.mode})
        remember{transact(operation){pdf->changes.forEach{(before,after)->NativeHighlightEdits.replace(pdf,before,after)}}}
    }

    fun inspectDocumentRepair(checkCurrent:()->Unit={}):DocumentAppearancePlan{
        checkCurrent();val file=fileForSave();val base=contentHash
        val doc=com.artifex.mupdf.fitz.Document.openDocument(file.absolutePath)
        try{
            require(doc is com.artifex.mupdf.fitz.PDFDocument && doc.hasPermission(com.artifex.mupdf.fitz.Document.PERMISSION_COPY))
            val count=doc.countPages();val items=mutableListOf<ManagedHighlight>()
            for(page in 0 until count){
                checkCurrent();items.addAll(NativeHighlightEdits.list(doc,page))
                require(items.size<=DocumentAppearanceRepair.MAX_ITEMS){"Document repair review item limit exceeded"}
            }
            checkCurrent();return DocumentAppearanceRepair.plan(base,count,items)
        }finally{doc.destroy()}
    }
    fun applyDocumentRepair(plan:DocumentAppearancePlan,operation:String){
        DocumentAppearanceRepair.verify(plan,contentHash)
        if(plan.changes.isEmpty())return
        remember{transact(operation){pdf->
            require(pdf.countPages()==plan.totalPages)
            for((before,after)in plan.changes)NativeHighlightEdits.replace(pdf,before,after)
        }}
    }
    private fun restore(file:File,expectedSha256:String){
        require(CheckpointFingerprint.of(file)==expectedSha256){"Undo checkpoint fingerprint mismatch"}
        val expected=ready();val operation=UUID.randomUUID().toString()
        val committed=try{
            store.commit(expected,operation,{target->file.copyTo(target,overwrite=false);require(CheckpointFingerprint.of(target)==expectedSha256)},NativePdfEditTransaction::validateEditable)
        }catch(error:Exception){
            val recovered=runCatching{store.recover()}.getOrNull()
            if(recovered?.operationId==operation)recovered else throw error
        }
        revision=committed
    }
    fun undo(){val command=checkNotNull(history.undo);restore(command.before,command.beforeSha256);history.didUndo(command)}
    fun redo(){val command=checkNotNull(history.redo);restore(command.after,command.afterSha256);history.didRedo(command)}
    val contentHash:String get()=revision?.sha256 ?: baselineHash
    val sourceOwnerHash:String get()=baselineHash
    fun fileForSave():File{ready();return currentFile()}
    fun currentFile():File=store.currentFile(checkNotNull(revision))
    /** Only after the reader has swapped to the new head and closed its old native document. */
    fun maintain(){runCatching{store.pruneOld(history.pins)}}
}
