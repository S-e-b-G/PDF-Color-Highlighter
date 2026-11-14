// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter
import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.File
import java.util.UUID

class ColumnContinuationInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private inline fun <T> pdf(file:File,body:(PDFDocument)->T):T {
        val d=Document.openDocument(file.absolutePath) as PDFDocument
        try{return body(d)}finally{d.destroy()}
    }
    private fun text(file:File)=pdf(file){d->val p=d.loadPage(0);try{MuPdfTextSnapshot.extract(p,true)}finally{p.destroy()}}
    private fun point(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"column-continuation-${UUID.randomUUID()}");var failure:Throwable?=null
        try{
            check(root.mkdirs());val source=File(root,"source.pdf");source.writeBytes(ColumnContinuationFixture.bytes())
            val baseline=CheckpointFingerprint.of(source);val snapshot=text(source)
            val left=snapshot.lines.first{it.text.startsWith("LEFT C")};val right=snapshot.lines.first{it.text.startsWith("RIGHT B")}
            val a=point(left.glyphs.first());val b=point(right.glyphs.last())
            val result=PageTextSelector.select(snapshot,a,b,false)
            check(result is TextSelectionAttempt.Selected){"Clear native columns rejected:$result"}
            val span=result.span
            val expected=listOf("LEFT C","LEFT D","LEFT E","LEFT F","RIGHT A","RIGHT B")
            check(expected.zipWithNext().all{(x,y)->span.quotation.indexOf(x)>=0 && span.quotation.indexOf(x)<span.quotation.indexOf(y)})
            check(!span.quotation.contains("LEFT B") && !span.quotation.contains("RIGHT C"))
            check(PageTextSelector.select(snapshot,b,a,false)==result)
            val shuffled=snapshot.copy(lines=snapshot.lines.reversed())
            check((PageTextSelector.select(shuffled,a,b,false) as TextSelectionAttempt.Selected).span.quotation==span.quotation)
            check(PreviewPaintGeometry.bands(span).none{it.left<280 && it.right>300})
            check(NativeHighlightAppearance.forSpan(span).none{it.bounds.left<280 && it.bounds.right>300})
            val edit=HighlightEdit.from(0,2,span)
            val w=LocalHighlightWorkspace(File(root,"edits"),"content://synthetic/column-continuation",source,baseline)
            w.add(edit);val head=w.contentHash
            var managed=pdf(w.currentFile()){NativeHighlightEdits.list(it,0).single()}
            val restored=checkNotNull(ManagedHighlightSelection.span(shuffled,managed))
            check(restored.quotation==span.quotation && restored.quads.size==span.quads.size)
            w.undo();check(pdf(w.currentFile()){NativeHighlightEdits.list(it,0).isEmpty()})
            w.redo();check(w.contentHash==head)
            w.replace(managed,managed.copy(contents="Manual cross-column quotation",mode=TextMode.MANUAL),UUID.randomUUID().toString())
            managed=pdf(w.currentFile()){NativeHighlightEdits.list(it,0).single()}
            val later=snapshot.lines.first{it.text.startsWith("RIGHT C")}
            val resized=PageTextSelector.adjust(snapshot,point(later.glyphs.last()),span.start) as TextSelectionAttempt.Selected
            w.replace(managed,managed.resized(resized.span),UUID.randomUUID().toString())
            val after=pdf(w.currentFile()){NativeHighlightEdits.list(it,0).single()}
            check(after.contents=="Manual cross-column quotation" && after.edit.color==managed.edit.color)
            check(ManagedHighlightSelection.span(shuffled,after)!=null)
            val recovered=LocalHighlightWorkspace(File(root,"edits"),"content://synthetic/column-continuation",source,baseline)
            check(recovered.recoverExisting()!=null)
            check(pdf(recovered.currentFile()){NativeHighlightEdits.list(it,0).single().contents}==after.contents)
            val out=File(root,"export.pdf")
            VerifiedDocumentExport.copy(w.fileForSave(),{out.outputStream()},{out.inputStream()})
            check(pdf(out){NativeHighlightEdits.list(it,0).single().contents}==after.contents)
            check(CheckpointFingerprint.of(source)==baseline)
        }catch(t:Throwable){failure=t}finally{root.deleteRecursively()}
        finish(if(failure==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(failure==null)"PASS: native column continuation geometry history recovery export"else"FAIL: column continuation")
            failure?.let{putString("details",it.stackTraceToString())}
        })
    }
}
