// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import org.sg.pdfcolorhighlighter.domain.PageViewportState
import org.sg.pdfcolorhighlighter.ui.PdfViewportView
import org.sg.pdfcolorhighlighter.ui.ReaderScreen

class MainActivity : ComponentActivity() {
    private lateinit var reader: ReaderViewModel
    private var viewportView: PdfViewportView? = null

    private val picker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.data?.let { uri ->
                reader.persistPickedGrant(uri, data.flags)
                reader.openDocument(uri.toString())
            }
        }
    }


    private val exportPicker=registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
        reader.finishExportPicker(uri)
    }
    private fun saveAs(){
        val name=reader.beginExport() ?: return
        try{exportPicker.launch(name)}catch(_:Exception){reader.finishExportPicker(null)}
    }


    private val savePicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val data=result.data;val target=if(result.resultCode==RESULT_OK)data?.data else null
        if(target!=null && data!=null && target.toString()==reader.activeUri)reader.persistPickedGrant(target,data.flags)
        reader.finishSaveAuthorization(target)
    }
    private fun saveOriginal(){
        val uri=reader.beginSave() ?: return
        if(reader.hasWriteGrant(uri)){reader.finishSaveAuthorization(uri);return}
        try{savePicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf"
            putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        })}catch(_:Exception){reader.finishSaveAuthorization(null)}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reader = ViewModelProvider(this)[ReaderViewModel::class.java]

        savedInstanceState?.getString("activeUri")?.let { uri ->
            reader.restoreActivity(
                uri, savedInstanceState.getInt("page", 0),
                PageViewportState(
                    savedInstanceState.getFloat("viewportZoom", 1f),
                    savedInstanceState.getFloat("viewportCenterX", 0.5f),
                    savedInstanceState.getFloat("viewportCenterY", 0.5f),
                    savedInstanceState.getBoolean("viewportFreeHorizontalPan", false),
                    savedInstanceState.getBoolean("viewportStartAtTop", false),
                    savedInstanceState.getFloat("viewportNavigationX", Float.NaN)
                        .takeIf { it.isFinite() },
                ),
            )
        }

        setContent {
            ReaderScreen(
                canSave=reader.canSave,onSave={saveOriginal()},
                canRecover=reader.canRecover,onRecover={reader.recoverLocal()},sourceDetached=reader.sourceDetached,
                onFrameDrawn={reader.recordFrameDrawn(it)},
                canExport=reader.canExport,onSaveAs={saveAs()},
                exportMessage=reader.exportMessage?.let{getString(it)},
                bitmap = reader.bitmap,
                documentName = reader.documentName,
                documentProperties = reader.properties,
                page = reader.page,
                pageCount = reader.pageCount,
                loading = reader.loading || reader.editBusy || !reader.ready,
                errorText = reader.errorRes?.let { getString(it) }
                    ?: if (reader.renderWarning) getString(R.string.reader_render_warning) else null,
                grantWarning = reader.grantWarning,
                viewportState = reader.viewportState,
                textSnapshot=reader.textSnapshot,textLoading=reader.textLoading,textFailed=reader.textFailed,
                previewSlot=reader.previewSlot,previewSpan=reader.previewSpan,previewCommitted=reader.previewCommitted,
                previewRejection=reader.previewRejection,
                onChoosePreviewSlot={ reader.choosePreviewSlot(it) },
                onPreviewDrag={ first,last,finished -> reader.previewSelection(first,last,finished) },
                onPreviewAdjust={ moving,fixed,finished -> reader.previewAdjustedSelection(moving,fixed,finished) },
                highlightContents=reader.selectedHighlight?.contents,highlightId=reader.selectedHighlight?.edit?.id,
                onChangeContents={reader.editHighlightContents(it)},onSmoothHighlight={reader.smoothHighlight()},
                onHighlightTap={reader.tapHighlight(it)},
                onPreviewCancel={ reader.cancelPreviewSelection() },
                canCommitHighlight=reader.canCommitHighlight,localCanUndo=reader.localCanUndo,localCanRedo=reader.localCanRedo,
                editBusy=reader.editBusy,localRevision=reader.localRevision,localEditNotice=reader.localEditNotice,
                localEditError=reader.localEditError?.let{getString(it)},
                onCommitHighlight={reader.commitLocalHighlight()},onUndoLocal={reader.undoLocalHighlight()},onRedoLocal={reader.redoLocalHighlight()},
                pageWidth = reader.pageWidth, pageHeight = reader.pageHeight, frameSerial = reader.frameSerial,
                onRenderViewport = { width, height, zoom -> reader.renderViewport(width, height, zoom) },
                onRenderGesture = { reader.renderGesture(it) },
                recentDocuments = reader.recents,
                unavailableUri = reader.unavailableUri,
                storageWarning = reader.storageWarning,
                onOpen = { pickDocument() },
                onRecent = { reader.openDocument(it) },
                onRemoveRecent = { reader.removeRecent(it) },
                onPrevious = { navigate(reader.page - 1) },
                onNext = { navigate(reader.page + 1) },
                onFit = { viewportView?.fitPage() },
                onFitWidth = { viewportView?.fitWidth() },
                onJumpToPage = { navigate(it) },
                onHand = { reader.stopPreview();viewportView?.activateNavigation() },
                onViewportReady = { viewportView = it },
                onViewportReleased = {
                    if (viewportView === it) {
                        viewportView = null
                        reader.releaseRenderViewport()
                    }
                },
                onViewportChanged = { reader.viewportChanged(it) },
            )
        }
    }

    private fun navigate(page: Int) {
        val state = viewportView?.pageNavigationState()
            ?: reader.viewportState.copy(centerY = 0f, startAtTop = true)
        reader.navigate(page, state)
    }

    private fun pickDocument() {
        if (!reader.ready || reader.loading || reader.editBusy) return
        reader.checkpoint()
        picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        })
    }

    override fun onStop() {
        reader.cancelPreviewSelection()
        reader.releaseRenderViewport()
        reader.checkpoint()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (reader.bitmap != null && !reader.loading) {
            outState.putString("activeUri", reader.activeUri)
            outState.putInt("page", reader.page)
            val state = reader.viewportState
            outState.putFloat("viewportZoom", state.zoom)
            outState.putFloat("viewportCenterX", state.centerX)
            outState.putFloat("viewportCenterY", state.centerY)
            outState.putBoolean("viewportFreeHorizontalPan", state.freeHorizontalPan)
            outState.putBoolean("viewportStartAtTop", state.startAtTop)
            outState.putFloat(
                "viewportNavigationX",
                state.navigationHorizontalFraction ?: Float.NaN
            )
        }
        reader.checkpoint()
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        viewportView?.onViewportChanged = null
        viewportView?.onHighlightTap=null;viewportView?.onFrameDrawn=null
        viewportView?.onPreviewDrag=null;viewportView?.onPreviewCancel=null;viewportView?.onPreviewAdjust=null
        viewportView?.onRenderViewport = null
        viewportView?.onRenderGesture = null
        viewportView = null
        // Native lifetime belongs to the retained ViewModel.
        super.onDestroy()
    }
}