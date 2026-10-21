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
)

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
                    val opened = Document.openDocument(localWorkspace.inputFile.absolutePath)
                    document = opened
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

    private fun render(requestedPage: Int): SmokeFrame {
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
            return SmokeFrame(bitmap, index, count, documentName, chars, properties, width, height)
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
