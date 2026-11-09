// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Native/checkpoint/save integration; not real SAF authorization or downstream interoperability. */
class NativeNotesInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private inline fun <T> pdf(file:File,body:(PDFDocument)->T):T {
        val d=Document.openDocument(file.absolutePath) as PDFDocument
        try{return body(d)}finally{d.destroy()}
    }
    private fun notes(f:File,page:Int=0)=pdf(f){NativeNoteEdits.list(it,page)}
    private fun op()=UUID.randomUUID().toString()
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"notes-proof-${UUID.randomUUID()}")
        var failure:Throwable?=null
        try{
            check(root.mkdirs());val input=File(root,"input.pdf");input.writeBytes(fixture())
            val hash=CheckpointFingerprint.of(input);val edits=File(root,"edits")
            val w=LocalHighlightWorkspace(edits,"content://synthetic/notes",input,hash)
            val bounds=pdf(input){d->val p=d.loadPage(0);try{val b=p.bounds;TextBounds(b.x0,b.y0,b.x1,b.y1)}finally{p.destroy()}}
            for(slot in 0..5){
                val n=NotePolicy.place(0,slot,TextPoint(80f+slot*60f,300f),bounds,"Note $slot : élève — cœur\n第二行")
                w.addNote(n);w.maintain()
            }
            var ns=notes(w.currentFile());check(ns.size==6 && ns.map{it.slot}.toSet()==(0..5).toSet())
            check(ns.all{it.contents.contains("élève — cœur") && it.contents.contains("第二行")})
            // Exact native readback is used for stale-state guards, never a fabricated requested rect.
            val before=ns.first();val firstHash=w.contentHash
            val corrected=before.copy(contents="Correction e\u0301\nfrançais",slot=5)
            w.replaceNote(before,corrected,op());w.maintain()
            val after=notes(w.currentFile()).single{it.id==before.id}
            check(after.contents==corrected.contents && after.slot==5 && after.bounds==before.bounds)
            val seq=w.sequence
            var rejected=false
            try{w.replaceNote(before,null,op())}catch(_:IllegalArgumentException){rejected=true}
            check(rejected && w.sequence==seq && notes(w.currentFile()).size==6)
            w.undo();check(w.contentHash==firstHash);w.redo();check(notes(w.currentFile()).any{it.contents==corrected.contents})
            w.replaceNote(after,null,op());check(notes(w.currentFile()).size==5)
            w.undo();check(notes(w.currentFile()).size==6);w.redo();check(notes(w.currentFile()).size==5);w.undo()
            val stable=w.sequence;w.replaceNote(after,after,op());check(w.sequence==stable)
            var duplicateRejected=false
            try{w.addNote(after)}catch(_:IllegalArgumentException){duplicateRejected=true}
            check(duplicateRejected && w.sequence==stable && notes(w.currentFile()).size==6)

            val q=GlyphQuad(TextPoint(50f,80f),TextPoint(100f,80f),TextPoint(50f,94f),TextPoint(100f,94f))
            w.add(HighlightEdit(op(),0,2,"Separate quotation",listOf(q)));w.maintain()
            check(notes(w.currentFile()).size==6)
            pdf(w.currentFile()){check(NativeHighlightEdits.list(it,0).single().contents=="Separate quotation")}
            val recovered=LocalHighlightWorkspace(edits,"content://synthetic/notes",input,hash)
            check(recovered.recoverExisting()!=null && notes(recovered.currentFile()).size==6 && !recovered.canUndo)
            val exported=File(root,"export.pdf")
            val receipt=VerifiedDocumentExport.copy(w.fileForSave(),{exported.outputStream()},{exported.inputStream()})
            check(receipt.sha256==w.contentHash && notes(exported).size==6)
            // Source-save byte verification already has its dedicated full regression suite.
            check(CheckpointFingerprint.of(input)==hash)
            // An imported note is preserved but not silently adopted for editing.
            val imported=File(root,"imported.pdf")
            pdf(exported){d->val p=d.loadPage(0) as PDFPage
                try{val a=p.createAnnotation(PDFAnnotation.TYPE_TEXT)
                    try{a.setRect(Rect(450f,400f,474f,424f));a.setContents("Foreign note");a.update()}finally{a.destroy()}
                    p.update()
                }finally{p.destroy()}
                d.save(imported.absolutePath,"compress")
            }
            check(notes(imported).size==6)
            val iw=LocalHighlightWorkspace(File(root,"import-edits"),"content://synthetic/imported",imported,CheckpointFingerprint.of(imported))
            val target=notes(imported).first();iw.replaceNote(target,target.copy(slot=1),op())
            pdf(iw.currentFile()){d->val p=d.loadPage(0) as PDFPage
                try{val all=p.annotations ?: emptyArray();try{check(all.any{it.contents=="Foreign note"})}finally{all.forEach{it.destroy()}}}finally{p.destroy()}
            }
        }catch(t:Throwable){failure=t}finally{root.deleteRecursively()}
        val e=failure
        finish(if(e==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(e==null)"PASS: native notes history recovery export"else"FAIL: native notes")
            if(e!=null)putString("details",e.stackTraceToString())
        })
    }
    private fun fixture():ByteArray {
        val content="BT /F1 12 Tf 50 750 Td (Local highlight one.) Tj 0 -24 Td (Local highlight two.) Tj ET\n"
        val objects=listOf("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Length ${content.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${content}endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        val out=ByteArrayOutputStream();val offsets=mutableListOf<Int>()
        fun append(s:String){out.write(s.toByteArray(Charsets.US_ASCII))}
        append("%PDF-1.4\n")
        objects.forEachIndexed{i,s->offsets.add(out.size());append("${i+1} 0 obj\n$s\nendobj\n")}
        val xref=out.size();append("xref\n0 ${objects.size+1}\n0000000000 65535 f \n")
        offsets.forEach{append(it.toString().padStart(10,'0')+" 00000 n \n")}
        append("trailer\n<< /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }
}
