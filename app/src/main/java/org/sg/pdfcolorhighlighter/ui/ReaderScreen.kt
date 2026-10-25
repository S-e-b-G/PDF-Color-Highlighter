// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.PageJumpInput
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.domain.RecentDocument

private val palette = listOf(Color(0xFFFFAD5B), Color(0xFF00FF00), Color(0xFFFC3636),
    Color(0xFFFFFF00), Color(0xFFD1DFEB), Color(0xFFFCD7FB))
private val slotNames = listOf(R.string.reader_slot_1, R.string.reader_slot_2, R.string.reader_slot_3,
    R.string.reader_slot_4, R.string.reader_slot_5, R.string.reader_slot_6)

/** M2b.2: centered page-navigation group, page-jump dialog and fading press feedback. */
@Composable
fun ReaderScreen(
    bitmap: Bitmap?, documentName: String?, documentProperties: org.sg.pdfcolorhighlighter.domain.DocumentProperties?, page: Int, pageCount: Int, loading: Boolean,
    errorText: String?, grantWarning: Boolean, viewportState: PageViewportState,
    onOpen: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onFit: () -> Unit,
    onFitWidth: () -> Unit, onJumpToPage: (Int) -> Unit, onHand: () -> Unit,
    onViewportReady: (PdfViewportView) -> Unit,
    textSnapshot: PageTextSnapshot?, textLoading: Boolean, textFailed: Boolean,
    previewSlot: Int?, previewSpan: SelectionSpan?, previewCommitted: Boolean,
    previewRejection: SelectionRejection?,
    onChoosePreviewSlot: (Int)->Unit,
    onPreviewDrag: (TextPoint,TextPoint,Boolean)->Unit, onPreviewCancel: ()->Unit,
    pageWidth: Float, pageHeight: Float, frameSerial: Long,
    onRenderViewport: (Int, Int, Float) -> Unit,
    onRenderGesture: (Boolean) -> Unit,
    onViewportReleased: (PdfViewportView) -> Unit,
    recentDocuments: List<RecentDocument>, unavailableUri: String?, storageWarning: Boolean,
    onRecent: (String) -> Unit, onRemoveRecent: (String) -> Unit,
    onViewportChanged: (PageViewportState) -> Unit,
) {
    var quotationOpen by remember { mutableStateOf(false) }
    var diagnosticReport by remember { mutableStateOf<SelectionLayoutReport?>(null) }
    LaunchedEffect(textSnapshot) { diagnosticReport=null }
    val previewEnabled=!loading && textSnapshot?.status==PageTextStatus.READY &&
        textSnapshot.lines.any { it.selectable && it.glyphs.any { glyph -> !glyph.whitespace } }
    var jumpOpen by rememberSaveable { mutableStateOf(false) }
    var pageDraft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(previewSpan) { if(previewSpan==null) quotationOpen=false }
    var propertiesOpen by rememberSaveable { mutableStateOf(false) }
    var openMenu by rememberSaveable { mutableStateOf(false) }
    MaterialTheme(colorScheme = lightColorScheme()) {
        val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                BoxWithConstraints(Modifier.fillMaxWidth().background(barColor).zIndex(1f)) {
                    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
                    when {
                        maxWidth >= 768.dp -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PreviewColorTools(previewSlot, previewEnabled, onChoosePreviewSlot)
                            Spacer(Modifier.weight(1f)); Actions(); Spacer(Modifier.weight(1f))
                            ColorTools(notes = true)
                        }
                        !tablet -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row { PreviewColorTools(previewSlot, previewEnabled, onChoosePreviewSlot) }; Row { Actions() }; Row { ColorTools(notes = true) }
                        }
                        else -> Text(stringResource(R.string.reader_width_blocked),
                            Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (grantWarning) Text(stringResource(R.string.grant_warning),
                    Modifier.background(barColor).padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall)
                if (storageWarning) Text(stringResource(R.string.reader_recent_storage_warning),
                    Modifier.background(barColor).padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                errorText?.let { Text(it, Modifier.background(barColor).padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.error) }
                Box(Modifier.fillMaxWidth().weight(1f).clipToBounds(), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        val description = stringResource(R.string.render_description)
                        AndroidView(
                            factory = { context -> PdfViewportView(context).also {
                                it.contentDescription = description
                                it.onViewportChanged = onViewportChanged
                                it.onPreviewDrag=onPreviewDrag;it.onPreviewCancel=onPreviewCancel
                                it.onRenderViewport = onRenderViewport
                                it.onRenderGesture = onRenderGesture
                                onViewportReady(it)
                            } },
                            update = { view ->
                                view.onViewportChanged = onViewportChanged
                                view.onPreviewDrag=onPreviewDrag;view.onPreviewCancel=onPreviewCancel
                                view.onRenderViewport = onRenderViewport
                                view.onRenderGesture = onRenderGesture
                                view.setInteractionEnabled(!loading)
                                view.show(bitmap, viewportState, pageWidth, pageHeight, frameSerial)
                                view.setSelectionPreview(textSnapshot,previewSlot,previewSpan)
                            },
                            onRelease = { view ->
                                view.onViewportChanged = null
                                view.onPreviewDrag=null;view.onPreviewCancel=null
                                view.onRenderViewport = null
                                view.onRenderGesture = null
                                onViewportReleased(view)
                            },
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                        )
                    } else if (loading) Text(stringResource(R.string.loading))
                    else RecentDocumentsPanel(recentDocuments, unavailableUri, onOpen, onRecent,
                        onRemoveRecent, Modifier.fillMaxSize().padding(12.dp))
                    if (previewCommitted && previewSpan!=null) {
                        Surface(Modifier.align(Alignment.TopEnd).padding(8.dp).zIndex(1f),
                            shape=RoundedCornerShape(8.dp),shadowElevation=2.dp) {
                            TextButton(onClick={ quotationOpen=true }) { Text(stringResource(R.string.selection_preview_quotation)) }
                        }
                    } else if (!loading && (previewRejection!=null || textFailed)) {
                        Surface(Modifier.align(Alignment.TopEnd).padding(8.dp).zIndex(1f),
                            shape=RoundedCornerShape(8.dp),shadowElevation=2.dp) {
                            TextButton(onClick={ diagnosticReport=SelectionLayoutDiagnostics.build(textSnapshot,previewRejection) }) {
                                Text(stringResource(R.string.selection_diag_button))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().background(barColor).zIndex(1f).padding(horizontal=8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    val status=when {
                        previewSlot!=null && previewRejection!=null -> selectionRejectionResource(previewRejection)
                        previewSlot!=null -> R.string.selection_preview_active
                        textLoading -> R.string.selection_preview_loading
                        textFailed -> R.string.selection_preview_unavailable
                        textSnapshot?.status==PageTextStatus.COPY_RESTRICTED -> R.string.selection_preview_restricted
                        textSnapshot?.status==PageTextStatus.TOO_COMPLEX -> R.string.selection_preview_complex
                        textSnapshot?.status==PageTextStatus.EMPTY -> R.string.selection_preview_empty
                        textSnapshot!=null && !previewEnabled -> R.string.selection_preview_unsupported
                        else -> R.string.selection_preview_readonly
                    }
                    Text(stringResource(R.string.selection_status_version,stringResource(R.string.reader_increment),stringResource(status)),Modifier.weight(1f).padding(vertical=2.dp),
                        style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                HorizontalDivider()
                ReaderFooter(documentName, page, pageCount, loading, bitmap != null,
                    { openMenu = true }, onPrevious, onNext, onFit, onFitWidth, onHand,
                    onPageNumber = { pageDraft = (page + 1).toString(); jumpOpen = true }, onProperties = { propertiesOpen = true }, navigationActive = previewSlot==null)
            }
        }
        if (openMenu) {
            AlertDialog(
                onDismissRequest = { openMenu = false },
                title = { Text(stringResource(R.string.open)) },
                text = {
                    RecentDocumentsPanel(
                        recentDocuments, unavailableUri,
                        onBrowse = { openMenu = false; onOpen() },
                        onRecent = { uri ->
                            openMenu = false
                            jumpOpen = false
                            propertiesOpen = false
                            onRecent(uri)
                        },
                        onRemove = onRemoveRecent,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 480.dp),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { openMenu = false }) {
                        Text(stringResource(R.string.reader_recents_close))
                    }
                },
            )
        }
        diagnosticReport?.let { report -> SelectionDiagnosticDialog(report) { diagnosticReport=null } }
        if (quotationOpen && previewSpan!=null) {
            AlertDialog(onDismissRequest={ quotationOpen=false },
                title={ Text(stringResource(R.string.selection_preview_quotation_title)) },
                text={ SelectionContainer {
                    Text(previewSpan.quotation,Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()))
                } },
                confirmButton={ TextButton(onClick={ quotationOpen=false }) {
                    Text(stringResource(R.string.reader_recents_close))
                } },
                dismissButton={ TextButton(onClick={
                    quotationOpen=false;diagnosticReport=SelectionLayoutDiagnostics.build(textSnapshot,previewRejection)
                }) { Text(stringResource(R.string.selection_diag_button)) } })
        }
        if (propertiesOpen && documentProperties != null) {
            DocumentPropertiesDialog(documentProperties, onDismiss = { propertiesOpen = false })
        }
        if (jumpOpen && pageCount > 0) {
            val target = PageJumpInput.parse(pageDraft, pageCount)
            val invalid = pageDraft.isNotBlank() && target == null
            val submit: () -> Unit = {
                if (target != null && !loading) {
                    jumpOpen = false
                    if (target != page + 1) onJumpToPage(target - 1)
                }
            }
            AlertDialog(
                onDismissRequest = { jumpOpen = false },
                title = { Text(stringResource(R.string.reader_jump_title)) },
                text = {
                    OutlinedTextField(value = pageDraft, onValueChange = { pageDraft = it },
                        label = { Text(stringResource(R.string.reader_jump_label)) },
                        supportingText = {
                            Text(stringResource(if (invalid) R.string.reader_jump_error else R.string.reader_jump_hint, pageCount))
                        },
                        singleLine = true, isError = invalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                },
                confirmButton = {
                    TextButton(onClick = submit, enabled = target != null && !loading) {
                        Text(stringResource(R.string.reader_jump_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { jumpOpen = false }) { Text(stringResource(R.string.reader_jump_cancel)) }
                },
            )
        }
    }
}

@Composable
private fun ReaderFooter(
    documentName: String?, page: Int, pageCount: Int, loading: Boolean, hasPage: Boolean,
    onOpen: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onFit: () -> Unit,
    onFitWidth: () -> Unit, onHand: () -> Unit, onPageNumber: () -> Unit, onProperties: () -> Unit,
    navigationActive: Boolean,
) {
    val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
    BoxWithConstraints(Modifier.fillMaxWidth().background(barColor).zIndex(1f)) {
        if (maxWidth >= 600.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Open(loading, onOpen); DocumentLabel(documentName, !loading && hasPage, onProperties)
                }
                // 48 + 48 + 80 + 48 + 48 dp, with equal flexible sides: number stays truly centered.
                Row(Modifier.width(272.dp), verticalAlignment = Alignment.CenterVertically) {
                    CenterCommands(page, pageCount, loading, hasPage, onPrevious, onNext, onFit, onFitWidth, onPageNumber)
                }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End) {
                    NavigationButton(loading, hasPage, onHand, navigationActive)
                    UnavailableIcon(R.drawable.reader_settings, stringResource(R.string.reader_settings))
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Open(loading, onOpen); DocumentLabel(documentName, !loading && hasPage, onProperties)
                    NavigationButton(loading, hasPage, onHand, navigationActive)
                    UnavailableIcon(R.drawable.reader_settings, stringResource(R.string.reader_settings))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    CenterCommands(page, pageCount, loading, hasPage, onPrevious, onNext, onFit, onFitWidth, onPageNumber)
                }
            }
        }
    }
}

@Composable
private fun CenterCommands(page: Int, total: Int, loading: Boolean, hasPage: Boolean,
                           previous: () -> Unit, next: () -> Unit, fit: () -> Unit,
                           fitWidth: () -> Unit, pageNumber: () -> Unit) {
    PulseIcon(R.drawable.reader_fit, stringResource(R.string.fit_page), !loading && hasPage, fit)
    PulseIcon(R.drawable.reader_previous, stringResource(R.string.previous), !loading && total > 0 && page > 0, previous)
    val description = if (total > 0) stringResource(R.string.reader_jump_accessibility, page + 1, total)
                      else stringResource(R.string.reader_no_page)
    PulseAction(!loading && total > 0, pageNumber, Modifier.width(80.dp).height(48.dp), description) {
        Text(if (total > 0) stringResource(R.string.reader_page, page + 1, total) else stringResource(R.string.reader_no_page),
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium)
    }
    PulseIcon(R.drawable.reader_next, stringResource(R.string.next), !loading && page + 1 < total, next)
    PulseIcon(R.drawable.reader_fit_width, stringResource(R.string.reader_fit_width), !loading && hasPage, fitWidth)
}
@Composable
private fun Open(loading: Boolean, action: () -> Unit) {
    PulseIcon(R.drawable.reader_open, stringResource(R.string.open), !loading, action)
}
@Composable
private fun RowScope.DocumentLabel(name: String?, enabled: Boolean, action: () -> Unit) {
    val label = name ?: stringResource(R.string.reader_no_document)
    PulseAction(enabled, action, Modifier.weight(1f).height(48.dp),
        stringResource(R.string.reader_properties_accessibility, label)) {
        Text(label, Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun NavigationButton(loading: Boolean, hasPage: Boolean, action: () -> Unit, active: Boolean) {
    PulseIcon(R.drawable.reader_hand, stringResource(if(active) R.string.reader_navigation_active else R.string.selection_preview_hand), !loading && hasPage, action, isSelected = active)
}
@Composable
private fun PulseIcon(icon: Int, label: String, enabled: Boolean, action: () -> Unit, isSelected: Boolean = false) {
    PulseAction(enabled, action, Modifier.size(48.dp), label, isSelected) {
        Icon(painterResource(icon), null, Modifier.size(24.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current)
    }
}
// M2b.3: keep the rectangular acknowledgement, disable only this component's Material ripple.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PulseAction(enabled: Boolean, action: () -> Unit, modifier: Modifier,
                        description: String, isSelected: Boolean = false, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.background(color.copy(alpha = alpha.value), RoundedCornerShape(10.dp)).semantics {
        contentDescription = description
        if (isSelected) selected = true
    }) {
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            IconButton(onClick = {
                job?.cancel()
                job = scope.launch {
                    alpha.snapTo(0.28f)
                    alpha.animateTo(0f, tween(durationMillis = 1000, easing = LinearEasing))
                }
                action()
            }, enabled = enabled, modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}


@Composable
private fun RowScope.PreviewColorTools(active: Int?, enabled: Boolean, choose: (Int)->Unit) {
    palette.forEachIndexed { index,color ->
        val description=stringResource(R.string.selection_preview_tool,stringResource(slotNames[index]))
        PulseAction(enabled,{ choose(index) },Modifier.size(48.dp),description,isSelected=active==index) {
            Box(Modifier.size(26.dp).background(color,CircleShape).border(1.dp,Color.DarkGray,CircleShape)) {
                if(active==index) Canvas(Modifier.fillMaxSize()) {
                    val ink=if(color.luminance()>0.179f) Color.Black else Color.White
                    drawLine(ink,Offset(size.width*0.2f,size.height*0.53f),Offset(size.width*0.43f,size.height*0.75f),
                        strokeWidth=2.dp.toPx(),cap=StrokeCap.Round)
                    drawLine(ink,Offset(size.width*0.43f,size.height*0.75f),Offset(size.width*0.82f,size.height*0.25f),
                        strokeWidth=2.dp.toPx(),cap=StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ColorTools(notes: Boolean) {
    palette.forEachIndexed { index, color ->
        val name = stringResource(slotNames[index])
        val description = stringResource(if (notes) R.string.reader_note_tool else R.string.reader_highlight_tool, name)
        Box(Modifier.size(48.dp).semantics { contentDescription = description; disabled() }, contentAlignment = Alignment.Center) {
            if (notes) Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.reader_note), null, Modifier.size(28.dp), tint = Color.DarkGray)
                Icon(painterResource(R.drawable.reader_note), null, Modifier.size(25.dp), tint = color)
            } else Box(Modifier.size(26.dp).background(color, CircleShape).border(1.dp, Color.DarkGray, CircleShape))
        }
    }
}
@Composable
private fun RowScope.Actions() {
    UnavailableIcon(R.drawable.reader_save, stringResource(R.string.reader_save))
    UnavailableIcon(R.drawable.reader_undo, stringResource(R.string.reader_undo))
    UnavailableIcon(R.drawable.reader_redo, stringResource(R.string.reader_redo))
    UnavailableIcon(R.drawable.reader_save_as, stringResource(R.string.reader_save_as))
}
@Composable
private fun UnavailableIcon(icon: Int, label: String) {
    val description = stringResource(R.string.reader_unavailable, label)
    Box(Modifier.size(48.dp).semantics { contentDescription = description; disabled() }, contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
    }
}
