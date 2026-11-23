// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val data=result.data;val uri=data?.data
        if(result.resultCode==RESULT_OK && uri!=null && data!=null){
            reader.persistPickedGrant(uri,data.flags)
            reader.finishDocumentPicker(uri.toString())
        }else reader.finishDocumentPicker(null)
    }


    private val fallbackPicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val data=result.data;reader.finishFallbackFolder(if(result.resultCode==RESULT_OK)data?.data else null,data?.flags ?: 0)
    }
    private fun chooseFallbackFolder(){
        if(!reader.beginFallbackFolder())return
        try{fallbackPicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply{
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        })}catch(_:Exception){reader.finishFallbackFolder(null,0)}
    }

    private val exportPicker=registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
        reader.finishExportPicker(uri)
    }
    private val existingExportPicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        reader.finishExistingExportPicker(if(result.resultCode==RESULT_OK)result.data?.data else null)
    }
    private fun saveAs(replace:Boolean){
        val name=reader.beginExport() ?: return
        try{
            if(replace)existingExportPicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }) else exportPicker.launch(name)
        }catch(_:Exception){reader.finishExportPicker(null)}
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

        val incomingPdf = intent.takeIf {
            savedInstanceState == null && it.action == Intent.ACTION_VIEW &&
                it.type == "application/pdf" && it.data?.scheme == "content"
        }?.data

        setContent {
            LaunchedEffect(reader.ready, incomingPdf) {
                if (reader.ready && incomingPdf != null) {
                    reader.persistPickedGrant(incomingPdf, intent.flags)
                    reader.openDocument(incomingPdf.toString())
                }
            }
            BackHandler { reader.requestExit() }
            var saveAsChoice by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            if(saveAsChoice)AlertDialog(onDismissRequest={saveAsChoice=false},
                title={Text(getString(R.string.reader_export_mode_title))},
                text={Text(getString(R.string.reader_export_mode_body))},
                confirmButton={TextButton(onClick={saveAsChoice=false;saveAs(false)}){Text(getString(R.string.reader_export_new_copy))}},
                dismissButton={TextButton(onClick={saveAsChoice=false;saveAs(true)}){Text(getString(R.string.reader_export_existing))}})
            reader.exportReplacement?.let{challenge->
                AlertDialog(onDismissRequest={reader.cancelExportReplacement()},
                    title={Text(getString(R.string.reader_export_existing))},
                    text={Text(getString(R.string.reader_export_replace_confirm,challenge.name))},
                    confirmButton={TextButton(onClick={reader.confirmExportReplacement()}){Text(getString(R.string.reader_export_replace_action))}},
                    dismissButton={TextButton(onClick={reader.cancelExportReplacement()}){Text(getString(R.string.reader_contents_cancel))}})
            }
            if(reader.noteEditorOpen){
                org.sg.pdfcolorhighlighter.ui.NoteContentsDialog(
                    reader.noteDraft,{reader.updateNoteDraft(it)},reader.editBusy,
                    reader.localEditError?.let{getString(it)},
                    {reader.confirmNoteEditor()},{reader.cancelNoteEditor()})
            }
            LaunchedEffect(reader.exitSignal){if(reader.consumeExitRequest())finish()}
            if(reader.exitPrompt){
                AlertDialog(onDismissRequest={reader.cancelExit()},
                    title={Text(getString(R.string.reader_exit_title))},
                    text={Text(getString(R.string.reader_exit_message))},
                    confirmButton={Column{
                        TextButton(onClick={if(reader.saveAndExit())saveOriginal()},enabled=reader.canSave){Text(getString(R.string.reader_exit_save))}
                        TextButton(onClick={reader.leaveWithoutSaving()}){Text(getString(R.string.reader_exit_keep))}
                    }},dismissButton={TextButton(onClick={reader.cancelExit()}){Text(getString(R.string.reader_contents_cancel))}})
            }

            if(reader.recoveryPrompt){
                var trash by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<org.sg.pdfcolorhighlighter.pdf.LocalRecoveryChoice?>(null) }
                fun copyLabel(copy:org.sg.pdfcolorhighlighter.pdf.LocalRecoveryChoice):String{
                    val date=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(copy.modified))
                    return "$date — ${copy.sha256.take(8)}"
                }
                val target=trash
                if(target!=null){
                    AlertDialog(onDismissRequest={trash=null},title={Text(getString(R.string.reader_copy_trash))},
                        text={Text(getString(R.string.reader_copy_trash_confirm,copyLabel(target)))},
                        confirmButton={TextButton(onClick={trash=null;reader.trashRecovery(target)}){Text(getString(R.string.reader_copy_trash))}},
                        dismissButton={TextButton(onClick={trash=null}){Text(getString(R.string.reader_contents_cancel))}})
                }else AlertDialog(onDismissRequest={reader.dismissRecovery()},
                    title={Text(getString(R.string.reader_copy_title))},
                    text={Column(Modifier.verticalScroll(rememberScrollState())){
                        Text(getString(if(reader.bitmap==null)R.string.reader_copy_unavailable else if(reader.sourceDetached)R.string.reader_copy_active_local else R.string.reader_copy_disk_first))
                        if(reader.recoveryRejected>0)Text(getString(R.string.reader_copy_unverified,reader.recoveryRejected))
                        if(reader.recoveryTrashError)Text(getString(R.string.reader_copy_trash_failed))
                        reader.recoveryChoices.forEach{copy->
                            Row{
                                TextButton(modifier=Modifier.weight(1f),enabled=!reader.editBusy,onClick={reader.chooseRecovery(copy)}){
                                    Text(getString(R.string.reader_copy_open)+" — "+copyLabel(copy))
                                }
                                TextButton(enabled=!reader.editBusy,onClick={trash=copy}){Text(getString(R.string.reader_copy_trash))}
                            }
                        }
                    }},
                    confirmButton={TextButton(enabled=!reader.editBusy,onClick={reader.continueDisk()}){Text(getString(R.string.reader_copy_continue))}},
                    dismissButton={TextButton(enabled=!reader.editBusy,onClick={reader.dismissRecovery()}){Text(getString(R.string.reader_contents_cancel))}})
            }

            if(reader.documentRepairBusy || reader.documentRepairReview!=null){
                val review=reader.documentRepairReview
                AlertDialog(onDismissRequest={reader.cancelDocumentRepair()},
                    title={Text(getString(R.string.reader_smooth_document))},
                    text={Text(if(review==null)getString(R.string.reader_document_repair_scan)
                        else if(review.highlights==0)getString(R.string.reader_document_repair_clean,review.totalPages)
                        else getString(R.string.reader_document_repair_review,review.highlights,review.affectedPages,review.totalPages))},
                    confirmButton={if(review!=null && review.highlights>0)TextButton(onClick={reader.confirmDocumentRepair()}){
                        Text(getString(R.string.reader_document_repair_confirm))
                    }},
                    dismissButton={TextButton(onClick={reader.cancelDocumentRepair()}){Text(getString(R.string.reader_contents_cancel))}})
            }
            LaunchedEffect(reader.pickerRequest){if(reader.consumePickerLaunch())launchDocumentPicker()}
            if(reader.settingsOpen){
                org.sg.pdfcolorhighlighter.ui.PaletteSettingsDialog(reader.paletteDraft,reader.paletteBusy,reader.paletteWarning,
                    {i,value->reader.updatePaletteDraft(i,value)},{reader.resetPaletteSlot(it)},{reader.resetPalette()},
                    {reader.applyPalette()},{reader.closeSettings()},
                    reader.fallbackEnabled,reader.fallbackBusy,reader.fallbackWarning,{chooseFallbackFolder()},{reader.disableFallback()})
            }
            ReaderScreen(
                highlightEditorRequest=reader.highlightEditorRequest,highlightEditorCloseSerial=reader.highlightEditorCloseSerial,highlightCanSmooth=reader.selectedHighlight?.imported==null,
                annotationPalette=reader.palette,selectedHighlightColor=reader.selectedHighlight?.edit?.color,
                canOpenSettings=reader.canOpenSettings,onSettings={reader.openSettings()},
                onDeleteHighlight={reader.deleteSelectedHighlight()},
                canDeleteSelection=reader.editAllowed && !reader.loading && !reader.editBusy &&
                    !reader.noteEditorOpen && (reader.selectedHighlight!=null || reader.selectedNote!=null),
                onDeleteSelection={if(reader.selectedNote!=null)reader.deleteNote() else reader.deleteSelectedHighlight()},
                noteSlot=reader.noteSlot,canUseNotes=reader.canUseNotes,noteBounds=reader.nativePageBounds,
                selectedNote=reader.selectedNote,onChooseNoteSlot={reader.chooseNoteSlot(it)},onNoteTap={reader.tapNote(it)},
                onEditNote={reader.openNoteEditor()},onDeleteNote={reader.deleteNote()},
                externalSaveState={reader.externalSaveState},unsavedContent=reader.hasUnsavedContent,
                canSave=reader.canSave,onSave={saveOriginal()},needsReplacement=reader.needsReplacement,
                canRecover=reader.canRecover,onRecover={reader.recoverLocal()},sourceDetached=reader.sourceDetached,
                onFrameDrawn={reader.recordFrameDrawn(it)},
                canExport=reader.canExport,onSaveAs={saveAsChoice=true},
                exportMessage=reader.exportMessage?.let{getString(it)},
                bitmap = reader.bitmap, resizePreviewBitmap=reader.resizePreviewBitmap,
                documentName = reader.documentName,
                documentProperties = reader.properties,
                page = reader.page,
                pageCount = reader.pageCount,
                loading = reader.loading || !reader.ready,
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
                canSmoothPage=reader.canSmoothPage,onSmoothPage={reader.smoothPage()},
                canReviewDocument=reader.canReviewDocument,onReviewDocument={reader.reviewDocumentRepair()},
                onHighlightTap={reader.tapHighlight(it)},
                onPreviewCancel={ reader.cancelPreviewSelection() },
                canCommitHighlight=reader.canCommitHighlight,localCanUndo=reader.localCanUndo,localCanRedo=reader.localCanRedo,
                editBusy=reader.editBusy,localRevision=reader.localRevision,localEditNotice=reader.localEditNotice,
                localEditError=reader.localEditError?.let{getString(it)},
                onCommitHighlight={reader.commitLocalHighlight()},onUndoLocal={reader.undoLocalHighlight()},onRedoLocal={reader.redoLocalHighlight()},
                pageWidth = reader.pageWidth, pageHeight = reader.pageHeight, frameSerial = reader.frameSerial,pageIdentity=reader.pageIdentity,
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
            if(reader.navigationPrompt){
                AlertDialog(onDismissRequest={reader.cancelDocumentOpen()},
                    title={Text(getString(R.string.reader_unsaved_title))},
                    text={Text(getString(R.string.reader_unsaved_message,reader.documentName ?: "PDF"))},
                    confirmButton={Column{
                        TextButton(onClick={if(reader.saveBeforeOpen())saveOriginal()},enabled=reader.canSave){Text(getString(R.string.reader_save))}
                        TextButton(onClick={reader.continueWithoutSaving()}){Text(getString(R.string.reader_unsaved_keep))}
                    }},dismissButton={TextButton(onClick={reader.cancelDocumentOpen()}){Text(getString(R.string.reader_contents_cancel))}})
            }
            reader.replacementChallenge?.let{challenge->
                AlertDialog(onDismissRequest={reader.cancelReplacement()},
                    title={Text(getString(R.string.reader_replacement_title))},
                    text={Text(getString(R.string.reader_replacement_warning,challenge.name ?: reader.activeUri ?: "PDF"))},
                    confirmButton={TextButton(onClick={reader.confirmReplacement()}){Text(getString(R.string.reader_source_save_confirm))}},
                    dismissButton={TextButton(onClick={reader.cancelReplacement()}){Text(getString(R.string.reader_contents_cancel))}})
            }
        }
    }

    private fun navigate(page: Int) {
        val state = viewportView?.pageNavigationState()
            ?: reader.viewportState.copy(centerY = 0f, startAtTop = true)
        reader.navigate(page, state)
    }

    private fun pickDocument(){reader.requestDocumentOpen(null)}
    private fun launchDocumentPicker() {
        if (!reader.ready || reader.loading || reader.editBusy) return
        reader.checkpoint()
        try{picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        })}catch(_:Exception){reader.finishDocumentPicker(null)}
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
        viewportView?.onNoteTap=null;viewportView?.onHighlightTap=null;viewportView?.onFrameDrawn=null
        viewportView?.onPreviewDrag=null;viewportView?.onPreviewCancel=null;viewportView?.onPreviewAdjust=null
        viewportView?.onRenderViewport = null
        viewportView?.onRenderGesture = null
        viewportView = null
        // Native lifetime belongs to the retained ViewModel.
        super.onDestroy()
    }
}
