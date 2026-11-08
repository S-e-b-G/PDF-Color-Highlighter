// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import org.sg.pdfcolorhighlighter.domain.DocumentProperties
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PasswordRequired : Exception()
class SmokeSizeLimit : Exception()

data class SmokeFrame(
    val bitmap: Bitmap,
    val page: Int,
    val pageCount: Int,
    val name: String?,
    val textCodePoints: Int?,
    val properties: DocumentProperties,
    val pageWidth: Float,
    val pageHeight: Float,
    val sourceDetached:Boolean=false,val sourceSaveBlocked:Boolean=false,
    val managedHighlights:List<org.sg.pdfcolorhighlighter.domain.ManagedHighlight> = emptyList(),
    val editAllowed:Boolean=false,val localRevision:Long=0L,
    val canUndoLocal:Boolean=false,val canRedoLocal:Boolean=false,
    val editNotice:org.sg.pdfcolorhighlighter.domain.LocalEditNotice=org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE,
    val contentHash:String?=null,val sourceHash:String?=null,
)
data class SourceReplacementChallenge(val sha256:String,val name:String?)
data class LocalEditOutcome(val frame:SmokeFrame?,val refreshFailed:Boolean,val sequence:Long,val canUndo:Boolean,val canRedo:Boolean)

class SmokePdfSession(context: Context) {
    private val app = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean(false)
    private val generation = AtomicInteger(0)
    private val refinementGeneration = AtomicInteger(0)
    private var document: Document? = null
    private val workspaceStore = ReadOnlyWorkspaceStore(java.io.File(app.noBackupFilesDir, "reader-workspaces"))
    private var workspace: ReadOnlyWorkspace? = null
    private var documentName: String? = null
    private var sourceUri: Uri? = null
    private var sourceSizeBytes = 0L
    private var documentProperties: DocumentProperties? = null
    private val editRoot=java.io.File(app.noBackupFilesDir,"reader-highlight-checkpoints-v1")
    private val sourceSaves=SourceSaveJournal(java.io.File(app.noBackupFilesDir,"source-save-journal-v1"))
    private var expectedSourceHash:String?=null
    private var sourceDetached=false
    private var sourceSaveBlocked=false
    private var cachedCountPage=-1
    private var cachedCharCount:Int?=null
    @Volatile private var retainedEditEdge=org.sg.pdfcolorhighlighter.domain.EditFramePolicy.EDIT_EDGE
    fun retainEditResolution(width:Int,height:Int){
        retainedEditEdge=org.sg.pdfcolorhighlighter.domain.EditFramePolicy.editEdge(width,height)
    }
    private var displayedLocalRevision=0L
    private var localHighlights:LocalHighlightWorkspace?=null
    private var localEditNotice=org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE


    private fun recoveryCopy(uri:Uri):Pair<java.io.File,String>{
        val prefix=org.sg.pdfcolorhighlighter.domain.LocalEditIdentity.uriKey(uri.toString())+"-"
        val dirs=editRoot.listFiles().orEmpty().filter{it.isDirectory && it.name.startsWith(prefix) &&
            Regex("[0-9a-f]{64}-[0-9a-f]{64}").matches(it.name)}
        require(dirs.size==1){"No unique local recovery namespace"}
        val dir=dirs.single();require(dir.canonicalFile.parentFile==editRoot.canonicalFile)
        val owner=dir.name.substringAfterLast('-');val store=DurablePdfRevisionStore(dir);val head=store.recover()
        require(head.baselineSha256==owner)
        return store.currentFile(head).also{NativePdfEditTransaction.validateEditable(it)} to owner
    }
    fun open(uri:Uri,page:Int,callback:(Result<SmokeFrame>)->Unit,recoveryOnly:Boolean=false){
        if(closed.get())return
        cancelRefinement();val request=generation.incrementAndGet()
        worker.execute{perform(request,callback){
            disposeDocument()
            fun load(recovery:Boolean):SmokeFrame{
                sourceUri=uri;val copy=if(recovery)recoveryCopy(uri)else null
                sourceDetached=recovery;sourceSaveBlocked=recovery
                documentName=if(recovery)app.getString(org.sg.pdfcolorhighlighter.R.string.reader_source_recovered_name)else
                    app.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->if(c.moveToFirst())c.getString(0)else null}
                val local=try{workspaceStore.create(uri.toString(),documentName){
                    copy?.first?.inputStream() ?: (app.contentResolver.openInputStream(uri) ?: error("No source input"))
                }}catch(_:WorkspaceSizeLimit){throw SmokeSizeLimit()}
                workspace=local;expectedSourceHash=if(recovery)null else local.baseline.fingerprint.sha256
                val link=if(recovery)null else sourceSaves.ownerFor(uri.toString(),local.baseline.fingerprint.sha256)
                val owner=copy?.second ?: link ?: local.baseline.fingerprint.sha256
                if(copy!=null || link!=null)check(java.io.File(editRoot,
                    org.sg.pdfcolorhighlighter.domain.LocalEditIdentity.key(uri.toString(),owner)).isDirectory)
                val editing=LocalHighlightWorkspace(editRoot,uri.toString(),local.inputFile,owner);localHighlights=editing
                val recovered=try{editing.recoverExisting()}catch(error:Exception){if(recovery)throw error else null}
                localEditNotice=editing.notice;val input=recovered ?: local.inputFile;sourceSizeBytes=input.length()
                val opened=Document.openDocument(input.absolutePath);document=opened;displayedLocalRevision=editing.sequence
                if(opened.needsPassword())throw PasswordRequired()
                require(opened.isPDF()){"Not a PDF document"}
                val frame=render(page)
                try{workspaceStore.accept(local)}catch(error:Exception){frame.bitmap.recycle();throw error}
                runCatching{workspaceStore.pruneReadOnly(local)};return frame
            }
            try{
                if(recoveryOnly)load(true)else try{load(false)}catch(error:Exception){
                    if(error is PasswordRequired || error is SmokeSizeLimit)throw error
                    val failed=workspace;disposeDocument();runCatching{workspaceStore.discardCandidate(failed)}
                    try{load(true)}catch(_:Exception){throw error}
                }
            }catch(error:Exception){val failed=workspace;disposeDocument();runCatching{workspaceStore.discardCandidate(failed)};throw error}
             catch(error:LinkageError){val failed=workspace;disposeDocument();runCatching{workspaceStore.discardCandidate(failed)};throw error}
        }}
    }

    fun renderPage(page: Int, callback: (Result<SmokeFrame>) -> Unit) {
        if (closed.get()) return
        cancelRefinement()
        val request = generation.incrementAndGet()
        worker.execute { perform(request, callback) { render(page) } }
    }


    fun cancelRefinement() { refinementGeneration.incrementAndGet() }

    /** A refinement must never invalidate an open/page-navigation request. */
    fun refinePage(index: Int, target: org.sg.pdfcolorhighlighter.domain.RenderResolution,
                   callback: (Result<Bitmap?>) -> Unit) {
        if (closed.get()) { callback(Result.success(null)); return }
        val contentRequest = generation.get()
        val detailRequest = refinementGeneration.incrementAndGet()
        fun isCurrent() = !closed.get() && contentRequest == generation.get() &&
            detailRequest == refinementGeneration.get()
        worker.execute {
            val result: Result<Bitmap?> = try {
                if (!isCurrent()) Result.success(null) else {
                    val doc = checkNotNull(document)
                    require(index in 0 until doc.countPages())
                    val nativePage = doc.loadPage(index)
                    try { Result.success(BoundedPageRenderer.render(nativePage, target)) }
                    finally { nativePage.destroy() }
                }
            } catch (e: Exception) { Result.failure(e) }
              catch (e: LinkageError) { Result.failure(e) }
              catch (e: OutOfMemoryError) { Result.failure(e) }
            main.post {
                if (!isCurrent()) {
                    result.getOrNull()?.recycle()
                    callback(Result.success(null))
                } else callback(result)
            }
        }
    }


    /** Copy-only snapshot on the same native worker. Does not change navigation/refinement generations. */
    fun extractText(index: Int, callback: (Result<org.sg.pdfcolorhighlighter.domain.PageTextSnapshot>) -> Unit) {
        if (closed.get()) return
        val contentRequest=generation.get()
        worker.execute {
            if (closed.get() || contentRequest!=generation.get()) return@execute
            val result: Result<org.sg.pdfcolorhighlighter.domain.PageTextSnapshot> =try {
                val doc=checkNotNull(document)
                require(index in 0 until doc.countPages())
                val nativePage=doc.loadPage(index)
                try { Result.success(MuPdfTextSnapshot.extract(nativePage,doc.hasPermission(Document.PERMISSION_COPY))) }
                finally { nativePage.destroy() }
            } catch(error:Exception) { Result.failure(error) }
              catch(error:LinkageError) { Result.failure(error) }
              catch(error:OutOfMemoryError) { Result.failure(error) }
            main.post {
                if (!closed.get() && contentRequest==generation.get()) callback(result)
            }
        }
    }

    private fun render(requestedPage: Int,editPreview:Boolean=false): SmokeFrame {
        val renderStarted=android.os.SystemClock.elapsedRealtime()
        syncLocalReader()
        val doc = checkNotNull(document)
        val count = doc.countPages()
        require(count > 0) { "Empty document" }
        val properties = documentProperties ?: readProperties(doc, count).also { documentProperties = it }
        val index = requestedPage.coerceIn(0, count - 1)
        val page = doc.loadPage(index)
        try {
            val bounds = page.bounds
            val width = bounds.x1 - bounds.x0
            val height = bounds.y1 - bounds.y0
            require(width.isFinite() && height.isFinite() && width > 0 && height > 0)
            require(width / height in 0.01f..100f) { "Unsupported smoke-test page ratio" }
            val countStarted=android.os.SystemClock.elapsedRealtime()
            val chars = if(editPreview && cachedCountPage==index)cachedCharCount else if (doc.hasPermission(Document.PERMISSION_COPY)) {
                val text = page.toStructuredText()
                try {
                    val value = text.asText()
                    value.codePointCount(0, value.length)
                } finally { text.destroy() }
            } else null
            cachedCountPage=index;cachedCharCount=chars
            val rasterStarted=android.os.SystemClock.elapsedRealtime()
            val edge=if(editPreview)retainedEditEdge else org.sg.pdfcolorhighlighter.domain.EditFramePolicy.READER_EDGE
            val bitmap = AndroidDrawDevice.drawPageFit(page,edge,edge)
            val rasterFinished=android.os.SystemClock.elapsedRealtime()
            try{
            val editing=localHighlights
            val editable=try{
                doc is com.artifex.mupdf.fitz.PDFDocument && !doc.needsPassword() && doc.isUnencryptedPDF() &&
                    doc.countSignatures()==0 && doc.hasPermission(Document.PERMISSION_COPY) &&
                    doc.hasPermission(Document.PERMISSION_ANNOTATE) &&
                    localEditNotice==org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE
            }catch(_:Exception){false}
            val highlights=if(editable && doc is com.artifex.mupdf.fitz.PDFDocument)NativeHighlightEdits.list(doc,index)else emptyList()
            if(editPreview)android.util.Log.i("PdfEditDetail","beforeCountMs=${countStarted-renderStarted} countMs=${rasterStarted-countStarted} rasterMs=${rasterFinished-rasterStarted} listMs=${android.os.SystemClock.elapsedRealtime()-rasterFinished} edge=$edge")
            return SmokeFrame(bitmap,index,count,documentName,chars,properties,width,height,sourceDetached,sourceSaveBlocked,
                highlights,
                editable,editing?.sequence ?: 0L,editing?.canUndo ?: false,editing?.canRedo ?: false,localEditNotice,editing?.contentHash,expectedSourceHash)
            }catch(error:Throwable){bitmap.recycle();throw error}
        } finally { page.destroy() }
    }

    /** All native metadata access uses this same serialized worker as render/open/disposal. */
    private fun readProperties(doc: Document, pages: Int): DocumentProperties {
        fun metadata(key: String): String? = try {
            doc.getMetaData(key)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
        val uri = checkNotNull(sourceUri)
        val providerModified = try {
            if (!DocumentsContract.isDocumentUri(app, uri)) null else {
                app.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                    null, null, null)?.use { cursor ->
                    val column = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    if (cursor.moveToFirst() && column >= 0 && !cursor.isNull(column)) {
                        cursor.getLong(column)
                    } else null
                }
            }
        } catch (_: Exception) { null }
        return DocumentProperties(
            displayName = documentName, sourceUri = uri.toString(),
            sizeBytes = sourceSizeBytes, pageCount = pages,
            title = metadata(Document.META_INFO_TITLE), author = metadata(Document.META_INFO_AUTHOR),
            subject = metadata(Document.META_INFO_SUBJECT), keywords = metadata(Document.META_INFO_KEYWORDS),
            creator = metadata(Document.META_INFO_CREATOR), producer = metadata(Document.META_INFO_PRODUCER),
            format = metadata(Document.META_FORMAT), creationDate = metadata(Document.META_INFO_CREATIONDATE),
            modificationDate = metadata(Document.META_INFO_MODIFICATIONDATE), sourceModifiedMillis = providerModified,
        )
    }

    private fun perform(request: Int, callback: (Result<SmokeFrame>) -> Unit,
                        operation: () -> SmokeFrame) {
        if (closed.get() || request != generation.get()) return
        val result = try { Result.success(operation()) }
        catch (e: Exception) { Result.failure(e) }
        catch (e: LinkageError) { Result.failure(e) }
        main.post {
            if (closed.get() || request != generation.get()) {
                result.getOrNull()?.bitmap?.recycle()
            } else callback(result)
        }
    }

    /** Only this serialized native worker may publish a local edit and replace the live reader. */
    fun createLocalHighlight(edit:org.sg.pdfcolorhighlighter.domain.HighlightEdit,uri:String,
                             callback:(Result<LocalEditOutcome>)->Unit){
        localEdit(edit.page,uri,{it.add(edit)},callback)
    }

    fun replaceLocalHighlight(before:org.sg.pdfcolorhighlighter.domain.ManagedHighlight,
                              after:org.sg.pdfcolorhighlighter.domain.ManagedHighlight?,operation:String,uri:String,
                              callback:(Result<LocalEditOutcome>)->Unit){
        localEdit(before.edit.page,uri,{it.replace(before,after,operation)},callback)
    }
    fun undoLocalHighlight(page:Int,uri:String,callback:(Result<LocalEditOutcome>)->Unit){
        localEdit(page,uri,{it.undo()},callback)
    }
    fun redoLocalHighlight(page:Int,uri:String,callback:(Result<LocalEditOutcome>)->Unit){
        localEdit(page,uri,{it.redo()},callback)
    }
    private fun localEdit(page:Int,uri:String,action:(LocalHighlightWorkspace)->Unit,
                          callback:(Result<LocalEditOutcome>)->Unit){
        if(closed.get())return
        cancelRefinement()
        val request=generation.incrementAndGet()
        val requestedAt=android.os.SystemClock.elapsedRealtime()
        worker.execute {
            if(closed.get() || request!=generation.get())return@execute
            val started=android.os.SystemClock.elapsedRealtime()
            val queueMs=started-requestedAt
            var commitMs=0L;var renderMs=0L
            var maintenance:LocalHighlightWorkspace?=null
            val result:Result<LocalEditOutcome> =try{
                check(sourceUri?.toString()==uri){"Document changed before edit"}
                val editing=checkNotNull(localHighlights)
                action(editing) // Durable commit completes BEFORE any live-document change.
                commitMs=android.os.SystemClock.elapsedRealtime()-started
                val renderStarted=android.os.SystemClock.elapsedRealtime()
                val frame=try{
                    syncLocalReader()
                    val updated=render(page,editPreview=true)
                    maintenance=editing // Defer pruning/extra verification until feedback is delivered.
                    updated
                }catch(_:Exception){null}
                 catch(_:OutOfMemoryError){null}
                renderMs=android.os.SystemClock.elapsedRealtime()-renderStarted
                Result.success(LocalEditOutcome(frame,frame==null,editing.sequence,editing.canUndo,editing.canRedo))
            }catch(error:Exception){Result.failure(error)}
             catch(error:LinkageError){Result.failure(error)}
             catch(error:OutOfMemoryError){Result.failure(error)}
            main.post {
                if(closed.get() || request!=generation.get())result.getOrNull()?.frame?.bitmap?.recycle()
                else callback(result)
                if(!closed.get())maintenance?.let{workspace->runCatching{worker.execute{
                    val cleanupStarted=android.os.SystemClock.elapsedRealtime()
                    workspace.maintain()
                    android.util.Log.i("PdfEditTiming","queueMs=$queueMs commitMs=$commitMs renderMs=$renderMs cleanupMs=${android.os.SystemClock.elapsedRealtime()-cleanupStarted}")
                }}}
            }
        }
    }


    /** Called only for a fresh ACTION_CREATE_DOCUMENT result; does not change the open source. */
    fun exportNewDocument(target:Uri,expectedUri:String,callback:(Result<VerifiedDocumentExport.Receipt>)->Unit){
        if(closed.get()){callback(Result.failure(IllegalStateException("Session closed")));return}
        val request=generation.get()
        worker.execute {
            val result=runCatching {
                check(!closed.get() && request==generation.get() && sourceUri?.toString()==expectedUri)
                require(target.scheme=="content" && target.toString()!=expectedUri){"Not a new document"}
                check(localEditNotice==org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE)
                val doc=checkNotNull(document)
                check(doc.hasPermission(Document.PERMISSION_COPY)){"Copy permission required"}
                val editing=checkNotNull(localHighlights)
                val source=if(editing.sequence>0)editing.currentFile() else checkNotNull(workspace).inputFile
                // Refuse a nonempty target even if a broken picker/provider returns an existing file.
                app.contentResolver.openInputStream(target)?.use { check(it.read()==-1){"Target is not empty"} }
                    ?: error("Cannot inspect new target")
                VerifiedDocumentExport.copy(source,
                    {app.contentResolver.openOutputStream(target,"wt") ?: error("No export output")},
                    {app.contentResolver.openInputStream(target) ?: error("No export read-back")})
            }
            main.post { if(!closed.get())callback(result) }
        }
    }



    /** Read-only challenge: validate a fresh copy of CURRENT external bytes before approval. */
    fun inspectReplacement(expectedUri:String,callback:(Result<SourceReplacementChallenge>)->Unit){
        if(closed.get())return
        val request=generation.get()
        worker.execute{
            val result=runCatching{
                check(!closed.get() && request==generation.get() && sourceUri?.toString()==expectedUri)
                val uri=checkNotNull(sourceUri)
                val candidate=workspaceStore.create(expectedUri,null){app.contentResolver.openInputStream(uri) ?: error("Cannot read source")}
                try{
                    NativePdfEditTransaction.validateEditable(candidate.inputFile)
                    val name=app.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->if(c.moveToFirst())c.getString(0)else null}
                    SourceReplacementChallenge(candidate.baseline.fingerprint.sha256,name?.takeIf{it.isNotBlank()})
                }finally{workspaceStore.discardCandidate(candidate)}
            }
            main.post{if(!closed.get() && request==generation.get())callback(result)}
        }
    }
    fun saveSource(expectedUri:String,callback:(Result<VerifiedDocumentExport.Receipt>)->Unit,replacementHash:String?=null){
        if(closed.get())return
        cancelRefinement();val request=generation.get()
        worker.execute{
            val result=runCatching{
                check(!closed.get() && request==generation.get() && sourceUri?.toString()==expectedUri)
                check(localEditNotice==org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE)
                check((!sourceDetached && !sourceSaveBlocked) || replacementHash!=null)
                val uri=checkNotNull(sourceUri);require(uri.scheme=="content"){"SAF content URI required"}
                check(app.checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==android.content.pm.PackageManager.PERMISSION_GRANTED){"Write permission required"}
                if(DocumentsContract.isDocumentUri(app,uri))app.contentResolver.query(uri,arrayOf(DocumentsContract.Document.COLUMN_FLAGS),null,null,null)?.use{c->
                    if(c.moveToFirst())require((c.getInt(0) and DocumentsContract.Document.FLAG_SUPPORTS_WRITE)!=0){"Provider is read-only"}}
                check(checkNotNull(document).hasPermission(Document.PERMISSION_COPY)){"Copy permission required"}
                val editing=checkNotNull(localHighlights)
                sourceSaves.save(expectedUri,replacementHash ?: checkNotNull(expectedSourceHash),editing.sourceOwnerHash,editing.fileForSave(),
                    {app.contentResolver.openInputStream(uri) ?: error("No source input")},
                    {app.contentResolver.openOutputStream(uri,"wt") ?: error("No source output")},NativePdfEditTransaction::validateEditable)
                    .also{
                        expectedSourceHash=it.sha256;sourceDetached=false;sourceSaveBlocked=false
                        documentName=runCatching{app.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->if(c.moveToFirst())c.getString(0)else null}}.getOrNull() ?: documentName
                        documentProperties=documentProperties?.copy(displayName=documentName,sourceModifiedMillis=null)
                    }
            }
            if(result.isFailure){sourceSaveBlocked=true;documentProperties=documentProperties?.copy(sourceModifiedMillis=null)}
            main.post{if(!closed.get() && request==generation.get())callback(result)}
        }
    }

    private fun syncLocalReader(){
        val editing=localHighlights ?: return
        if(editing.sequence==displayedLocalRevision)return
        val file=editing.currentFile()
        val next=Document.openDocument(file.absolutePath)
        val previous=document
        document=next;displayedLocalRevision=editing.sequence
        sourceSizeBytes=file.length();documentProperties=documentProperties?.copy(sizeBytes=sourceSizeBytes)
        previous?.destroy()
    }

    private fun disposeDocument() {
        try { document?.destroy() }
        finally {
            document = null
            workspaceStore.release(workspace)
            workspace = null
            documentName = null
            sourceUri = null
            sourceSizeBytes = 0L
            documentProperties = null
            expectedSourceHash=null;sourceDetached=false;sourceSaveBlocked=false;cachedCountPage=-1;cachedCharCount=null
            localHighlights=null;displayedLocalRevision=0L;localEditNotice=org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE
        }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        cancelRefinement()
        generation.incrementAndGet()
        worker.execute { disposeDocument() }
        worker.shutdown()
    }
}