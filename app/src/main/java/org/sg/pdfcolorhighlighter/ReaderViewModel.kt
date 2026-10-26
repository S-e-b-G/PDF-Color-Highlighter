// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import org.sg.pdfcolorhighlighter.data.RecentDocumentsStore
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.File
import java.util.concurrent.Executors

/**
 * Owns the read-only native session across configuration changes.
 * Main-thread entry points. Never retains an Activity, View or picker.
 */
class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val session = SmokePdfSession(application)
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val store = RecentDocumentsStore(
        File(application.filesDir, "reader-recents-v1.bin")
    )
    private val selectionWorker=Executors.newSingleThreadExecutor()
    private val selectionQueue=SelectionPreviewQueue()
    private val previewRetention=PreviewRetentionState()
    var textSnapshot by mutableStateOf<PageTextSnapshot?>(null)
        private set
    var textLoading by mutableStateOf(false)
        private set
    var textFailed by mutableStateOf(false)
        private set
    var previewSlot by mutableStateOf<Int?>(null)
        private set
    var previewSpan by mutableStateOf<SelectionSpan?>(null)
        private set
    var previewCommitted by mutableStateOf(false)
        private set
    var previewRejection by mutableStateOf<SelectionRejection?>(null)
        private set
    private var cleared = false
    private var writesAllowed = false
    private var dirty = false
    private var writeSequence = 0L
    private var opening = false
    private var initialRestoreRequested = false
    private var pendingRestore: Triple<String, Int, PageViewportState>? = null
    private var pendingViewport = PageViewportState()
    private val debounce = Runnable { flushLocation() }
    private val periodic = Runnable { flushLocation() }
    private val refinement = Runnable { requestRefinement() }
    private val renderGate = RenderTicketGate()
    private var renderWidth = 0
    private var renderHeight = 0
    private var renderZoom = 1f
    private var rendering = false
    private var gestureActive = false
    private var refinementBlocked = false
    var pageWidth by mutableStateOf(1f)
        private set
    var pageHeight by mutableStateOf(1f)
        private set
    var frameSerial by mutableStateOf(0L)
        private set
    var renderWarning by mutableStateOf(false)
        private set

    var ready by mutableStateOf(false)
        private set
    var recents by mutableStateOf<List<RecentDocument>>(emptyList())
        private set
    var unavailableUri by mutableStateOf<String?>(null)
        private set
    var storageWarning by mutableStateOf(false)
        private set
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set
    var documentName by mutableStateOf<String?>(null)
        private set
    var properties by mutableStateOf<DocumentProperties?>(null)
        private set
    var page by mutableIntStateOf(0)
        private set
    var pageCount by mutableIntStateOf(0)
        private set
    var viewportState by mutableStateOf(PageViewportState())
        private set
    var loading by mutableStateOf(false)
        private set
    var errorRes by mutableStateOf<Int?>(null)
        private set
    var grantWarning by mutableStateOf(false)
        private set
    var activeUri: String? = null
        private set

    init {
        io.execute {
            val result = runCatching { store.read() }
            main.post {
                if (!cleared) {
                    result.fold(
                        onSuccess = {
                            recents = it
                            writesAllowed = true
                        },
                        onFailure = {
                            // Preserve unreadable/unsupported state; do not replace with empty data.
                            storageWarning = true
                        },
                    )
                    ready = true
                    pendingRestore?.let { (uri, index, state) ->
                        pendingRestore = null
                        open(uri, index, state)
                    }
                }
            }
        }
    }

    fun restoreActivity(uri: String, index: Int, state: PageViewportState) {
        if (initialRestoreRequested || activeUri != null || loading) return
        initialRestoreRequested = true
        if (!ready) pendingRestore = Triple(uri, index, state)
        else open(uri, index, state)
    }

    fun openDocument(uri: String) {
        if (!ready || loading) return
        val entry = recents.firstOrNull { it.uri == uri }
        open(uri, entry?.page ?: 0, entry?.viewport ?: PageViewportState())
    }

    private fun open(uri: String, index: Int, state: PageViewportState) {
        if (uri == activeUri && bitmap != null && !loading) return
        resetPageText()
        stopRefinement()
        checkpoint()
        activeUri = uri
        pendingViewport = state
        viewportState = state
        bitmap = null
        documentName = null
        properties = null
        pageCount = 0
        loading = true
        opening = true
        errorRes = null
        grantWarning = try {
            !app.contentResolver.persistedUriPermissions.any {
                it.uri.toString() == uri && it.isReadPermission
            }
        } catch (_: SecurityException) {
            true
        }
        session.open(uri.toUri(), index, ::accept)
    }

    fun navigate(index: Int, navigationState: PageViewportState) {
        if (loading || bitmap == null || index !in 0 until pageCount || index == page) return
        resetPageText(preserveTool=true)
        stopRefinement()
        checkpoint()
        pendingViewport = navigationState
        opening = false
        loading = true
        errorRes = null
        session.renderPage(index, ::accept)
    }

    private fun accept(result: Result<SmokeFrame>) {
        if (cleared) {
            result.getOrNull()?.bitmap?.recycle()
            return
        }
        loading = false
        result.fold(
            onSuccess = { frame ->
                pageWidth = frame.pageWidth
                pageHeight = frame.pageHeight
                frameSerial++
                refinementBlocked = false
                renderWarning = false
                viewportState = pendingViewport
                bitmap = frame.bitmap
                page = frame.page
                pageCount = frame.pageCount
                documentName = frame.name?.takeIf { it.isNotBlank() }
                    ?: app.getString(R.string.document_fallback)
                properties = frame.properties
                val uri = checkNotNull(activeUri)
                if (opening) {
                    recents = RecentDocuments.opened(
                        recents,
                        RecentDocument(
                            uri, checkNotNull(documentName),
                            System.currentTimeMillis().coerceAtLeast(0),
                            page, pageCount, viewportState,
                        ),
                    )
                    if (unavailableUri == uri) unavailableUri = null
                } else {
                    recents = RecentDocuments.location(recents, uri, page, viewportState)
                }
                opening = false
                markDirty()
                flushLocation()
                scheduleRefinement()
                loadPageText()
            },
            onFailure = { error ->
                errorRes = when (error) {
                    is PasswordRequired -> R.string.error_password
                    is SmokeSizeLimit -> R.string.error_limit
                    is LinkageError -> R.string.error_native
                    else -> if (opening) R.string.reader_recent_unavailable
                            else R.string.error_open
                }
                if (opening) {
                    unavailableUri = activeUri
                    activeUri = null
                }
                opening = false
            },
        )
    }

    fun viewportChanged(state: PageViewportState) {
        if (loading || bitmap == null || activeUri == null) return
        viewportState = state
        val updated = RecentDocuments.location(
            recents, checkNotNull(activeUri), page, state
        )
        if (updated != recents) {
            recents = updated
            markDirty()
        }
    }


    fun renderViewport(width: Int, height: Int, zoom: Float) {
        if (width == renderWidth && height == renderHeight && zoom == renderZoom) return
        renderWidth = width; renderHeight = height; renderZoom = zoom
        stopRefinement()
        scheduleRefinement()
    }

    fun renderGesture(active: Boolean) {
        if (gestureActive == active) return
        gestureActive = active
        if (active) stopRefinement() else scheduleRefinement()
    }

    fun releaseRenderViewport() {
        renderWidth = 0; renderHeight = 0; gestureActive = false
        stopRefinement()
    }

    private fun stopRefinement() {
        main.removeCallbacks(refinement)
        renderGate.invalidate()
        session.cancelRefinement()
    }

    private fun scheduleRefinement() {
        main.removeCallbacks(refinement)
        if (!cleared && !loading && !gestureActive && !refinementBlocked &&
            bitmap != null && renderWidth > 0 && renderHeight > 0) {
            main.postDelayed(refinement, 250L)
        }
    }

    private fun requestRefinement() {
        if (cleared || loading || gestureActive || refinementBlocked || rendering ||
            renderWidth <= 0 || renderHeight <= 0) return
        val previous = bitmap ?: return
        val target = RenderResolutionPolicy.plan(pageWidth, pageHeight, renderWidth, renderHeight, renderZoom)
        if (!RenderResolutionPolicy.needsUpgrade(target, previous.width, previous.height)) return
        val ticket = renderGate.ticket()
        rendering = true
        session.refinePage(page, target) { result ->
            rendering = false
            val detail = result.getOrNull()
            if (cleared || loading || gestureActive || !renderGate.accepts(ticket)) {
                detail?.recycle() // Never published to a UI/render thread.
            } else if (detail != null) {
                bitmap = detail // Pixels only: keep viewport, page serial and reading state.
            } else if (result.isFailure) {
                refinementBlocked = true
                renderWarning = true // Keep preview; retry on another page/open, not an infinite loop.
            }
            if (!cleared) scheduleRefinement()
        }
    }


    private fun resetPageText(preserveTool:Boolean=false) {
        val tool=PreviewContinuityPolicy.toolAfterPageChange(previewSlot,preserveTool)
        previewRetention.clear();previewSlot=tool
        cancelPreviewSelection()
        textSnapshot=null;textLoading=false;textFailed=false
    }
    private fun loadPageText() {
        textLoading=true;textFailed=false
        val serial=frameSerial;val uri=activeUri;val index=page
        session.extractText(index) { result ->
            if (cleared || loading || serial!=frameSerial || uri!=activeUri || index!=page) return@extractText
            textLoading=false
            result.fold(onSuccess={ textSnapshot=it },onFailure={ textFailed=true;textSnapshot=null })
        }
    }
    fun choosePreviewSlot(slot: Int) {
        require(slot in 0..5)
        val snapshot=textSnapshot ?: return
        if (loading || snapshot.status!=PageTextStatus.READY) return
        if (previewSlot==slot) stopPreview() else previewSlot=slot
    }
    fun cancelPreviewSelection() {
        selectionQueue.cancel()
        previewSpan=previewRetention.restore(textSnapshot)
        previewCommitted=previewSpan!=null;previewRejection=null
    }
    fun stopPreview() { previewSlot=null;previewRetention.clear();cancelPreviewSelection() }
    fun previewSelection(first:TextPoint,last:TextPoint,finished:Boolean) {
        offerPreview(first,last,finished,true,null)
    }
    fun previewAdjustedSelection(moving:TextPoint,fixed:TextAddress,finished:Boolean) {
        offerPreview(moving,moving,finished,false,fixed)
    }
    private fun offerPreview(first:TextPoint,last:TextPoint,finished:Boolean,words:Boolean,fixed:TextAddress?) {
        if(cleared || loading || previewSlot==null)return
        val snapshot=textSnapshot ?: return
        if(previewCommitted){if(words)previewSpan=null;previewCommitted=false}
        selectionQueue.offer(snapshot,first,last,finished,words,fixed)?.let{dispatchPreview(it)}
    }
    private fun dispatchPreview(request: PreviewSelectionRequest) {
        selectionWorker.execute {
            val result: Result<TextSelectionAttempt> =try { Result.success(request.fixedAddress?.let{PageTextSelector.adjust(request.snapshot,request.first,it)} ?: PageTextSelector.select(request.snapshot,request.first,request.last,request.snapToWords)) }
                catch(error:Exception) { Result.failure(error) }
                catch(error:OutOfMemoryError) { Result.failure(error) }
            main.post {
                if (cleared) return@post
                if (!loading && previewSlot!=null && request.snapshot===textSnapshot && selectionQueue.acceptsFeedback(request,result.getOrNull() is TextSelectionAttempt.Selected)) {
                    result.fold(onSuccess={ selection ->
                        when(selection) {
                            is TextSelectionAttempt.Selected -> {
                                if(SelectionPreviewQueue.allows(selection.span)) {
                                    previewSpan=selection.span;previewCommitted=request.finished;previewRejection=null
                                    if(request.finished)previewRetention.commit(request.snapshot,selection.span)
                                } else {
                                    if(request.finished)previewRetention.clear()
                                    previewSpan=null;previewCommitted=false;previewRejection=SelectionRejection.TOO_COMPLEX
                                }
                            }
                            is TextSelectionAttempt.Rejected -> {
                                if(request.finished)previewRetention.clear()
                                previewSpan=null;previewCommitted=false;previewRejection=selection.reason
                            }
                        }
                    },onFailure={
                        if(request.finished)previewRetention.clear()
                        cancelPreviewSelection();previewRejection=SelectionRejection.TOO_COMPLEX
                    })
                }
                selectionQueue.complete(request)?.let { dispatchPreview(it) }
            }
        }
    }

    fun removeRecent(uri: String) {
        if (!ready || loading) return
        recents = RecentDocuments.remove(recents, uri)
        if (unavailableUri == uri) unavailableUri = null
        // Never delete PDF/recovery files or revoke potentially shared grants here.
        markDirty()
        flushLocation()
    }

    private fun markDirty() {
        if (!writesAllowed) return
        if (!dirty) main.postDelayed(periodic, 5000L)
        dirty = true
        main.removeCallbacks(debounce)
        main.postDelayed(debounce, 750L)
    }

    fun checkpoint() {
        flushLocation()
    }

    private fun flushLocation() {
        main.removeCallbacks(debounce)
        main.removeCallbacks(periodic)
        if (!dirty || !writesAllowed) return
        dirty = false
        val snapshot = recents.toList()
        val sequence = ++writeSequence
        io.execute {
            val result = runCatching { store.write(snapshot) }
            main.post {
                if (!cleared && sequence == writeSequence) {
                    storageWarning = result.isFailure
                    if (result.isFailure) dirty = true
                }
            }
        }
    }

    fun persistPickedGrant(uri: android.net.Uri, flags: Int) {
        val read = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (read != 0 && flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
            try {
                app.contentResolver.takePersistableUriPermission(uri, read)
            } catch (_: SecurityException) {
                // Temporary access may still allow this session to open the document.
            }
        }
    }

    override fun onCleared() {
        stopPreview()
        selectionWorker.shutdown()
        stopRefinement()
        flushLocation()
        cleared = true
        main.removeCallbacksAndMessages(null)
        session.close()
        io.shutdown()
        super.onCleared()
    }
}
