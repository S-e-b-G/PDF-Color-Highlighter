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
 * Owns the reader and local editing session across configuration changes.
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
    var editBusy by mutableStateOf(false)
        private set
    var editAllowed by mutableStateOf(false)
        private set
    var localRevision by mutableStateOf(0L)
        private set
    var localCanUndo by mutableStateOf(false)
        private set
    var localCanRedo by mutableStateOf(false)
        private set
    var localEditNotice by mutableStateOf(LocalEditNotice.NONE)
        private set
    var localEditError by mutableStateOf<Int?>(null)
        private set

    var exportMessage by mutableStateOf<Int?>(null)
        private set


    var exitPrompt by mutableStateOf(false)
        private set
    var exitSignal by mutableStateOf(0L)
        private set
    private var exitPending=false
    private var exitAfterSave=false
    private fun signalExit(){checkpoint();exitPending=true;exitSignal++}
    fun consumeExitRequest():Boolean{
        if(!exitPending)return false
        exitPending=false;return true
    }
    fun requestExit(){
        when(ReaderExitPolicy.decide(hasUnsavedContent,editBusy)){
            ReaderExitDecision.WAIT->Unit // Never leave during an unconfirmed source write/edit.
            ReaderExitDecision.CONFIRM->exitPrompt=true
            ReaderExitDecision.LEAVE->signalExit()
        }
    }
    fun cancelExit(){exitPrompt=false;exitAfterSave=false}
    fun leaveWithoutSaving(){if(!exitPrompt || editBusy)return;exitPrompt=false;signalExit()}
    fun saveAndExit():Boolean{
        if(!exitPrompt || !canSave)return false
        exitPrompt=false;exitAfterSave=true;return true
    }

    private var exportPendingUri:String?=null
    private var savePendingUri:String?=null
    var sourceDetached by mutableStateOf(false)
        private set
    private var saveBlocked by mutableStateOf(false)
    private var editFeedbackAt=0L
    private var editFeedbackSerial:Long?=null
    val canSave:Boolean get()=canExport && editAllowed
    val needsReplacement:Boolean get()=sourceDetached || saveBlocked
    private var currentContentHash:String?=null
    private var externallySavedHash:String?=null
    val hasUnsavedContent:Boolean get()=SavedContentPolicy.needsSave(currentContentHash,externallySavedHash)
    var replacementChallenge by mutableStateOf<SourceReplacementChallenge?>(null)
        private set
    var navigationPrompt by mutableStateOf(false)
        private set
    var pickerRequest by mutableStateOf(0L)
        private set
    var recoveryChoices by mutableStateOf<List<LocalRecoveryChoice>>(emptyList())
        private set
    var recoveryPrompt by mutableStateOf(false)
        private set
    var recoveryRejected by mutableStateOf(0)
        private set
    private var recoveryInitial=false
    private var pendingRecovery:LocalRecoveryChoice?=null
    private var pendingVersionSwitch=false
    private var pendingDestination:String?=null
    private var hasPendingDestination=false
    private var saveThenOpen=false
    private var pickerLaunchPending=false
    private var pickerAwaitingResult=false
    fun requestDocumentOpen(uri:String?){
        if(!ready || loading || editBusy || replacementChallenge!=null || navigationPrompt || exitPrompt)return
        pendingDestination=uri;hasPendingDestination=true
        if(hasUnsavedContent)navigationPrompt=true else executePendingOpen()
    }
    private fun executePendingOpen(){
        if(!hasPendingDestination)return
        val target=pendingDestination
        val copy=pendingRecovery;val version=pendingVersionSwitch
        pendingRecovery=null;pendingVersionSwitch=false
        pendingDestination=null;hasPendingDestination=false;navigationPrompt=false;saveThenOpen=false
        if(target==null){pickerAwaitingResult=true;pickerLaunchPending=true;pickerRequest++}
        else if(version)open(target,page,viewportState,copy)else openDocumentNow(target)
    }
    fun consumePickerLaunch():Boolean{
        if(!pickerLaunchPending)return false
        pickerLaunchPending=false;return true
    }
    fun finishDocumentPicker(uri:String?){
        if(!pickerAwaitingResult)return
        pickerAwaitingResult=false
        if(uri!=null)openDocumentNow(uri)
    }
    fun continueWithoutSaving(){if(navigationPrompt)executePendingOpen()}
    fun cancelDocumentOpen(){
        exitAfterSave=false;exitPrompt=false
        navigationPrompt=false;hasPendingDestination=false;pendingDestination=null;saveThenOpen=false
        pendingRecovery=null;pendingVersionSwitch=false
    }
    fun saveBeforeOpen():Boolean{
        if(!navigationPrompt || !canSave)return false
        navigationPrompt=false;saveThenOpen=true;return true
    }
    val canRecover:Boolean get()=ready && !loading && !editBusy && recoveryChoices.isNotEmpty() &&
        (activeUri!=null || unavailableUri!=null)
    fun beginSave():android.net.Uri?{
        if(!canSave)return null
        savePendingUri=activeUri;editBusy=true;exportMessage=R.string.reader_source_save_wait
        stopRefinement();cancelPreviewSelection();checkpoint();return activeUri?.toUri()
    }
    fun hasWriteGrant(uri:android.net.Uri):Boolean=app.checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun cancelSave(message:Int){
        savePendingUri=null;replacementChallenge=null;editBusy=false
        cancelDocumentOpen();exportMessage=message;scheduleRefinement()
    }
    fun finishSaveAuthorization(target:android.net.Uri?){
        val expected=savePendingUri ?: return
        if(target==null){cancelSave(R.string.reader_source_save_cancelled);return}
        if(target.toString()!=expected || activeUri!=expected){cancelSave(R.string.reader_source_save_wrong_target);return}
        if(!hasWriteGrant(target)){cancelSave(R.string.reader_source_save_permission);return}
        if(needsReplacement){
            session.inspectReplacement(expected){result->if(!cleared){
                result.fold(onSuccess={replacementChallenge=it;exportMessage=null},onFailure={
                    cancelSave(R.string.reader_replacement_unavailable)
                })
                // Keep the native view disabled behind the confirmation; no edit/picker race.
            }}
        }else performSourceSave(null)
    }
    fun cancelReplacement(){cancelSave(R.string.reader_source_save_cancelled)}
    fun confirmReplacement(){
        val challenge=replacementChallenge ?: return
        replacementChallenge=null
        performSourceSave(challenge)
    }
    private fun performSourceSave(challenge:SourceReplacementChallenge?){
        val expected=savePendingUri ?: return
        savePendingUri=null
        session.saveSource(expected,{result->if(!cleared){
            editBusy=false;properties=properties?.copy(sourceModifiedMillis=null)
            result.fold(onSuccess={receipt->
                sourceDetached=false;saveBlocked=false;externallySavedHash=receipt.sha256
                if(challenge?.name!=null){documentName=challenge.name;properties=properties?.copy(displayName=challenge.name)}
                exportMessage=R.string.reader_source_save_ok
                if(exitAfterSave){exitAfterSave=false;signalExit()}else if(saveThenOpen)executePendingOpen()
            },onFailure={error->
                saveBlocked=true;cancelDocumentOpen()
                exportMessage=if(error is SourceSaveConflict)R.string.reader_source_save_conflict else R.string.reader_source_save_failed
            })
            scheduleRefinement()
        }},challenge?.sha256)
    }

    fun dismissRecovery(){recoveryPrompt=false;recoveryInitial=false}
    fun continueDisk(){
        val reload=!recoveryInitial || sourceDetached || bitmap==null
        recoveryPrompt=false;recoveryInitial=false
        if(reload)queueVersion(null)
    }
    fun chooseRecovery(choice:LocalRecoveryChoice){
        if(!recoveryChoices.contains(choice))return
        recoveryPrompt=false;recoveryInitial=false;queueVersion(choice)
    }
    private fun queueVersion(choice:LocalRecoveryChoice?){
        if(loading || editBusy || navigationPrompt || exitPrompt)return
        val uri=activeUri ?: unavailableUri ?: return
        pendingDestination=uri;hasPendingDestination=true;pendingRecovery=choice;pendingVersionSwitch=true
        if(hasUnsavedContent)navigationPrompt=true else executePendingOpen()
    }
    fun recoverLocal(){
        if(!canRecover)return
        val uri=activeUri ?: unavailableUri ?: return
        recoveryInitial=false;editBusy=true;stopRefinement()
        session.listRecoveries(uri.toUri()){result->if(!cleared){
            editBusy=false
            result.fold(onSuccess={listing->
                recoveryChoices=listing.choices;recoveryRejected=listing.rejected;recoveryPrompt=true
            },onFailure={exportMessage=R.string.reader_copy_listing_failed})
            scheduleRefinement()
        }}
    }
    fun recordFrameDrawn(serial:Long){
        if(editFeedbackSerial!=serial)return
        editFeedbackSerial=null
        android.util.Log.i("PdfEditFeedback","requestToDrawMs=${android.os.SystemClock.uptimeMillis()-editFeedbackAt}")
    }
    private fun startEditFeedback(){bitmap?.let{session.retainEditResolution(it.width,it.height)};editFeedbackAt=android.os.SystemClock.uptimeMillis();editFeedbackSerial=null;exportMessage=null}
    val canExport:Boolean get()=!cleared && !loading && !editBusy && bitmap!=null &&
        activeUri!=null && localEditNotice==LocalEditNotice.NONE
    fun beginExport():String? {
        if(!canExport)return null
        val stamp=java.text.SimpleDateFormat("yyyyMMdd-HHmmss-SSS",java.util.Locale.ROOT).format(java.util.Date())
        val name=VerifiedDocumentExport.suggestedName(documentName,stamp)
        exportPendingUri=activeUri;exportMessage=R.string.reader_export_choose;editBusy=true
        stopRefinement();cancelPreviewSelection();checkpoint()
        return name
    }
    fun finishExportPicker(target:android.net.Uri?) {
        // A result surviving process death has no retained request: never export a different PDF.
        val expected=exportPendingUri ?: return
        exportPendingUri=null
        if(target==null){editBusy=false;scheduleRefinement();exportMessage=R.string.reader_export_cancelled;return}
        exportMessage=R.string.reader_export_busy
        session.exportNewDocument(target,expected){result->
            if(!cleared){
                editBusy=false;scheduleRefinement()
                result.getOrNull()?.let{externallySavedHash=it.sha256}
                exportMessage=if(result.isSuccess)R.string.reader_export_ok else R.string.reader_export_failed
            }
        }
    }

    private var pageHighlights:List<ManagedHighlight> = emptyList()
    var selectedHighlight by mutableStateOf<ManagedHighlight?>(null)
        private set
    private var reselectHighlightId:String?=null
    fun tapHighlight(point:TextPoint):Boolean {
        if(loading || editBusy || !editAllowed)return false
        val snapshot=textSnapshot ?: return false
        val hits=ManagedHighlightSelection.hits(pageHighlights,point)
        if(hits.isEmpty())return false
        if(hits.size!=1){localEditError=R.string.reader_highlight_overlap;return true}
        val item=hits.single()
        val span=ManagedHighlightSelection.span(snapshot,item)
        if(span==null){localEditError=R.string.reader_highlight_unresolved;return true}
        selectionQueue.cancel();selectedHighlight=item;previewSlot=item.edit.slot
        previewSpan=span;previewCommitted=true;previewRejection=null;previewRetention.commit(snapshot,span)
        localEditError=null
        return true
    }

    fun editHighlightContents(contents:String){
        val before=selectedHighlight ?: return
        if(contents.length>100000){localEditError=R.string.reader_contents_too_long;return}
        if(contents==before.contents)return
        changeHighlight(before,before.copy(contents=contents,mode=TextMode.MANUAL))
    }
    fun smoothHighlight(){
        val before=selectedHighlight ?: return
        val span=previewSpan ?: return
        changeHighlight(before,before.copy(edit=before.edit.copy(appearance=NativeHighlightAppearance.forSpan(span))))
    }
    private fun clearHighlightTarget(){selectedHighlight=null;reselectHighlightId=null}

    private fun changeHighlight(before:ManagedHighlight,after:ManagedHighlight?){
        if(loading || editBusy || !editAllowed)return
        val uri=activeUri ?: return
        val styled=after?.let{item->previewSpan?.let{span->item.copy(edit=item.edit.copy(appearance=NativeHighlightAppearance.forSpan(span)))} ?: item}
        if(before==styled)return
        startEditFeedback()
        editBusy=true;localEditError=null;stopRefinement();selectionQueue.cancel();pendingViewport=viewportState
        reselectHighlightId=styled?.edit?.id
        session.replaceLocalHighlight(before,styled,java.util.UUID.randomUUID().toString(),uri,::acceptLocalEdit)
    }
    private var pendingHighlight:HighlightEdit?=null
    private var pendingHighlightSpan:SelectionSpan?=null
    private var pendingHighlightSlot:Int?=null
    val canCommitHighlight:Boolean get()=!cleared && !loading && !editBusy && editAllowed &&
        previewCommitted && previewSpan!=null && previewSlot!=null
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
    var pageIdentity by mutableStateOf(0L)
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

    fun openDocument(uri:String){requestDocumentOpen(uri)}
    private fun openDocumentNow(uri: String) {
        if (!ready || loading || editBusy) return
        val entry = recents.firstOrNull { it.uri == uri }
        open(uri, entry?.page ?: 0, entry?.viewport ?: PageViewportState())
    }

    private fun open(uri: String, index: Int, state: PageViewportState,copy:LocalRecoveryChoice?=null) {
        recoveryPrompt=false;recoveryChoices=emptyList();recoveryRejected=0
        exportMessage=null;sourceDetached=false;saveBlocked=false;savePendingUri=null;replacementChallenge=null
        currentContentHash=null;externallySavedHash=null
        editAllowed=false;localRevision=0L;localCanUndo=false;localCanRedo=false
        localEditNotice=LocalEditNotice.NONE;localEditError=null
        pendingHighlight=null;pendingHighlightSpan=null;pendingHighlightSlot=null
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
        session.open(uri.toUri(), index, ::accept,recoveryOnly=copy!=null,recoveryId=copy?.id,recoverySha=copy?.sha256)
    }

    fun navigate(index: Int, navigationState: PageViewportState) {
        if (loading || editBusy || bitmap == null || index !in 0 until pageCount || index == page) return
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
                recoveryChoices=frame.recoveries.choices;recoveryRejected=frame.recoveries.rejected
                if(opening && !frame.sourceDetached && (recoveryChoices.isNotEmpty() || recoveryRejected>0)){recoveryInitial=true;recoveryPrompt=true}
                currentContentHash=frame.contentHash
                if(opening)externallySavedHash=frame.sourceHash
                sourceDetached=frame.sourceDetached;saveBlocked=frame.sourceSaveBlocked
                pageHighlights=frame.managedHighlights
                editAllowed=frame.editAllowed;localRevision=frame.localRevision
                localCanUndo=frame.canUndoLocal;localCanRedo=frame.canRedoLocal;localEditNotice=frame.editNotice
                pageWidth = frame.pageWidth
                pageHeight = frame.pageHeight
                frameSerial++;pageIdentity++
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
                if(error is RecoveryCopiesAvailable){
                    recoveryChoices=error.listing.choices;recoveryRejected=error.listing.rejected;recoveryPrompt=true
                }
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
        if (loading || editBusy || bitmap == null || activeUri == null) return
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
        if (!cleared && !loading && !editBusy && !gestureActive && !refinementBlocked &&
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
        selectedHighlight=null;pageHighlights=emptyList();reselectHighlightId=null
        val tool=PreviewContinuityPolicy.toolAfterPageChange(previewSlot,preserveTool)
        previewRetention.clear();previewSlot=tool
        cancelPreviewSelection()
        textSnapshot=null;textLoading=false;textFailed=false
    }

    private fun clearEditSelection(){
        selectionQueue.cancel();selectedHighlight=null;previewRetention.clear()
        previewSpan=null;previewCommitted=false;previewRejection=null
    }
    private fun restoreEditedSelection(snapshot:PageTextSnapshot){
        val id=reselectHighlightId;reselectHighlightId=null
        val item=pageHighlights.singleOrNull{it.edit.id==id} ?: return
        val span=ManagedHighlightSelection.span(snapshot,item)
        if(span!=null){selectedHighlight=item;previewSlot=item.edit.slot;previewSpan=span
            previewCommitted=true;previewRetention.commit(snapshot,span)}
        else{selectedHighlight=null;localEditError=R.string.reader_highlight_unresolved}
    }
    private fun loadPageText() {
        textLoading=true;textFailed=false
        val serial=frameSerial;val uri=activeUri;val index=page
        session.extractText(index) { result ->
            if (cleared || loading || serial!=frameSerial || uri!=activeUri || index!=page) return@extractText
            textLoading=false

            result.fold(onSuccess={ snapshot->
                textSnapshot=snapshot
                restoreEditedSelection(snapshot)
            },onFailure={ textFailed=true;textSnapshot=null;reselectHighlightId=null })
        }
    }
    fun choosePreviewSlot(slot: Int) {
        require(slot in 0..5)
        val snapshot=textSnapshot ?: return
        if (loading || editBusy || snapshot.status!=PageTextStatus.READY) return

        val selected=selectedHighlight
        if(selected!=null){
            if(selected.edit.slot==slot)changeHighlight(selected,null)
            else changeHighlight(selected,selected.copy(edit=selected.edit.copy(slot=slot)))
        }else if(previewSlot==slot)stopPreview() else previewSlot=slot
    }
    fun cancelPreviewSelection() {
        selectionQueue.cancel()
        previewSpan=previewRetention.restore(textSnapshot)
        previewCommitted=previewSpan!=null;previewRejection=null
    }
    fun stopPreview() { clearHighlightTarget();previewSlot=null;previewRetention.clear();cancelPreviewSelection() }
    fun previewSelection(first:TextPoint,last:TextPoint,finished:Boolean) {
        if(!loading && !editBusy)localEditError=null
        if(selectedHighlight!=null){clearHighlightTarget();previewRetention.clear()}
        offerPreview(first,last,finished,true,null)
    }
    fun previewAdjustedSelection(moving:TextPoint,fixed:TextAddress,finished:Boolean) {
        offerPreview(moving,moving,finished,false,fixed)
    }
    private fun offerPreview(first:TextPoint,last:TextPoint,finished:Boolean,words:Boolean,fixed:TextAddress?) {
        if(cleared || loading || editBusy || previewSlot==null)return
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

                var completedAccepted=false
                if (!loading && !editBusy && previewSlot!=null && request.snapshot===textSnapshot && selectionQueue.acceptsFeedback(request,result.getOrNull() is TextSelectionAttempt.Selected)) {
                    result.fold(onSuccess={ selection ->
                        when(selection) {
                            is TextSelectionAttempt.Selected -> {
                                if(SelectionPreviewQueue.allows(selection.span)) {
                                    previewSpan=selection.span;previewCommitted=request.finished;previewRejection=null
                                    if(request.finished){previewRetention.commit(request.snapshot,selection.span);completedAccepted=true}
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
                if(completedAccepted && !editBusy && !loading && request.snapshot===textSnapshot &&
                    previewCommitted && previewSpan!=null && localEditError==null)commitLocalHighlight()
            }
        }
    }

    fun commitLocalHighlight(){
        if(!canCommitHighlight)return

        val span=previewSpan ?: return;val slot=previewSlot ?: return;val uri=activeUri ?: return
        val selected=selectedHighlight
        if(selected!=null){changeHighlight(selected,selected.resized(span));return}
        val edit=try{
            if(pendingHighlightSpan===span && pendingHighlightSlot==slot)checkNotNull(pendingHighlight)
            else HighlightEdit.from(page,slot,span).also{pendingHighlight=it;pendingHighlightSpan=span;pendingHighlightSlot=slot}
        }catch(_:Exception){localEditError=R.string.reader_edit_failed;return}
        startEditFeedback()
        editBusy=true;localEditError=null;stopRefinement();selectionQueue.cancel()
        pendingViewport=viewportState
        reselectHighlightId=edit.id
        session.createLocalHighlight(edit,uri,::acceptLocalEdit)
    }
    fun undoLocalHighlight(){
        if(cleared || loading || editBusy || !localCanUndo)return
        val uri=activeUri ?: return
        startEditFeedback()
        editBusy=true;localEditError=null;stopRefinement();clearHighlightTarget();clearEditSelection()
        pendingViewport=viewportState
        session.undoLocalHighlight(page,uri,::acceptLocalEdit)
    }
    fun redoLocalHighlight(){
        if(cleared || loading || editBusy || !localCanRedo)return
        val uri=activeUri ?: return
        startEditFeedback()
        editBusy=true;localEditError=null;stopRefinement();clearHighlightTarget();clearEditSelection()
        pendingViewport=viewportState
        session.redoLocalHighlight(page,uri,::acceptLocalEdit)
    }

    private fun acceptLocalEdit(result:Result<LocalEditOutcome>){
        if(cleared){result.getOrNull()?.frame?.bitmap?.recycle();return}
        editBusy=false
        result.fold(onSuccess={outcome->
            pendingHighlight=null;pendingHighlightSpan=null;pendingHighlightSlot=null
            localRevision=outcome.sequence;localCanUndo=outcome.canUndo;localCanRedo=outcome.canRedo
            // Even a failed frame refresh follows a durable successful commit.
            currentContentHash=outcome.frame?.contentHash ?: ("unrefreshed:"+outcome.sequence)
            val frame=outcome.frame
            if(frame!=null){
                val retained=textSnapshot.takeIf{EditFramePolicy.retainText(page,frame.page,pageWidth,frame.pageWidth,
                    pageHeight,frame.pageHeight,frame.textCodePoints!=null,it)}
                clearEditSelection();pageHighlights=frame.managedHighlights;editAllowed=frame.editAllowed
                localEditNotice=frame.editNotice;sourceDetached=frame.sourceDetached;saveBlocked=frame.sourceSaveBlocked
                localRevision=frame.localRevision;localCanUndo=frame.canUndoLocal;localCanRedo=frame.canRedoLocal
                pageWidth=frame.pageWidth;pageHeight=frame.pageHeight;page=frame.page;pageCount=frame.pageCount
                frameSerial++;editFeedbackSerial=frameSerial
                bitmap=frame.bitmap;properties=frame.properties
                refinementBlocked=false;renderWarning=false;opening=false;loading=false;localEditError=null
                textSnapshot=retained;textLoading=false;textFailed=false
                if(retained!=null)restoreEditedSelection(retained)else loadPageText()
                scheduleRefinement()
            }else{editAllowed=false;refinementBlocked=true;localEditError=R.string.reader_edit_refresh_failed}
        },onFailure={
            editFeedbackSerial=null;reselectHighlightId=null;localEditError=R.string.reader_edit_failed
            if(textSnapshot==null && bitmap!=null)loadPageText()
        })
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
        val read = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
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
