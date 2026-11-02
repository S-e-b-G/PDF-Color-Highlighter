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
    val managedHighlights:List<org.sg.pdfcolorhighlighter.domain.ManagedHighlight> = emptyList(),
    val editAllowed:Boolean=false,val localRevision:Long=0L,
    val canUndoLocal:Boolean=false,val canRedoLocal:Boolean=false,
    val editNotice:org.sg.pdfcolorhighlighter.domain.LocalEditNotice=org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE,
)
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
    private var displayedLocalRevision=0L
    private var localHighlights:LocalHighlightWorkspace?=null
    private var localEditNotice=org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE

    fun open(uri: Uri, page: Int, callback: (Result<SmokeFrame>) -> Unit) {
        if (closed.get()) return
        cancelRefinement()
        val request = generation.incrementAndGet()
        worker.execute {
            perform(request, callback) {
                disposeDocument()
                try {
                    sourceUri = uri
                    documentName = app.contentResolver.query(uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                    val localWorkspace = try {
                        workspaceStore.create(uri.toString(), documentName) {
                            app.contentResolver.openInputStream(uri)
                                ?: throw IllegalStateException("No input stream")
                        }
                    } catch (_: WorkspaceSizeLimit) { throw SmokeSizeLimit() }
                    workspace = localWorkspace
                    sourceSizeBytes = localWorkspace.baseline.fingerprint.sizeBytes
                    val editing=LocalHighlightWorkspace(java.io.File(app.noBackupFilesDir,"reader-highlight-checkpoints-v1"),
                        uri.toString(),localWorkspace.inputFile,localWorkspace.baseline.fingerprint.sha256)
                    localHighlights=editing
                    val recovered=try{editing.recoverExisting()}catch(_:Exception){null}
                    localEditNotice=editing.notice
                    val input=recovered ?: localWorkspace.inputFile
                    sourceSizeBytes=input.length()
                    val opened = Document.openDocument(input.absolutePath)
                    document = opened
                    displayedLocalRevision=editing.sequence
                    if (opened.needsPassword()) throw PasswordRequired()
                    require(opened.isPDF()) { "Not a PDF document" }
                    val frame = render(page)
                    try {
                        workspaceStore.accept(localWorkspace)
                    } catch (e: Exception) {
                        frame.bitmap.recycle()
                        throw e
                    }
                    // Maintenance failure must not invalidate an already rendered read-only document.
                    val retained = try { workspaceStore.pruneReadOnly(localWorkspace) } catch (_: Exception) { false }
                    if (!retained) android.util.Log.w("PdfWorkspace", "Read-only baseline retention cleanup incomplete")
                    frame
                } catch (e: Exception) {
                    val failed = workspace
                    disposeDocument()
                    try { workspaceStore.discardCandidate(failed) } catch (_: Exception) { }
                    throw e
                } catch (e: LinkageError) {
                    val failed = workspace
                    disposeDocument()
                    try { workspaceStore.discardCandidate(failed) } catch (_: Exception) { }
                    throw e
                }
            }
        }
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

    private fun render(requestedPage: Int): SmokeFrame {
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
            val chars = if (doc.hasPermission(Document.PERMISSION_COPY)) {
                val text = page.toStructuredText()
                try {
                    val value = text.asText()
                    value.codePointCount(0, value.length)
                } finally { text.destroy() }
            } else null
            val bitmap = AndroidDrawDevice.drawPageFit(page, 1600, 1600)
            val editing=localHighlights
            val editable=try{
                doc is com.artifex.mupdf.fitz.PDFDocument && !doc.needsPassword() && doc.isUnencryptedPDF() &&
                    doc.countSignatures()==0 && doc.hasPermission(Document.PERMISSION_COPY) &&
                    doc.hasPermission(Document.PERMISSION_ANNOTATE) &&
                    localEditNotice==org.sg.pdfcolorhighlighter.domain.LocalEditNotice.NONE
            }catch(_:Exception){false}
            return SmokeFrame(bitmap,index,count,documentName,chars,properties,width,height,
                if(editable && doc is com.artifex.mupdf.fitz.PDFDocument)NativeHighlightEdits.list(doc,index)else emptyList(),
                editable,editing?.sequence ?: 0L,editing?.canUndo ?: false,editing?.canRedo ?: false,localEditNotice)
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
        worker.execute {
            if(closed.get() || request!=generation.get())return@execute
            val result:Result<LocalEditOutcome> =try{
                check(sourceUri?.toString()==uri){"Document changed before edit"}
                val editing=checkNotNull(localHighlights)
                action(editing) // Durable commit completes BEFORE any live-document change.
                val frame=try{
                    syncLocalReader()
                    val updated=render(page)
                    editing.maintain() // Previous native readers are now closed; current/previous slots stay.
                    updated
                }catch(_:Exception){null}
                 catch(_:OutOfMemoryError){null}
                Result.success(LocalEditOutcome(frame,frame==null,editing.sequence,editing.canUndo,editing.canRedo))
            }catch(error:Exception){Result.failure(error)}
             catch(error:LinkageError){Result.failure(error)}
             catch(error:OutOfMemoryError){Result.failure(error)}
            main.post {
                if(closed.get() || request!=generation.get())result.getOrNull()?.frame?.bitmap?.recycle()
                else callback(result)
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

    private fun syncLocalReader(){
        val editing=localHighlights ?: return
        if(editing.sequence==displayedLocalRevision)return
        val file=editing.currentFile()
        val next=Document.openDocument(file.absolutePath)
        val previous=document
        document=next;displayedLocalRevision=editing.sequence
        documentProperties=null;sourceSizeBytes=file.length()
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