// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter
import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.*
import org.sg.pdfcolorhighlighter.data.PaletteStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class NativePaletteInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private inline fun <T> pdf(f:File,body:(PDFDocument)->T):T {
        val d=Document.openDocument(f.absolutePath) as PDFDocument
        try{return body(d)}finally{d.destroy()}
    }
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"palette-proof-${UUID.randomUUID()}");var failure:Throwable?=null
        try{
            check(root.mkdirs());val input=File(root,"input.pdf");input.writeBytes(fixture())
            val pref=File(root,"palette.bin");val store=PaletteStore(pref)
            check(store.read()==AnnotationPalette.DEFAULT)
            val palette=AnnotationPalette(listOf(0x123456,0x234567,0x345678,0x456789,0x56789A,0x6789AB))
            store.write(palette);check(PaletteStore(pref).read()==palette)
            pref.writeText("invalid");check(runCatching{store.read()}.isFailure);check(pref.readText()=="invalid");store.write(palette)
            val blocked=File(root,"not-a-directory").also{it.writeText("keep")}
            check(runCatching{PaletteStore(File(blocked,"palette.bin")).write(palette)}.isFailure)
            check(blocked.readText()=="keep")
            val baseline=CheckpointFingerprint.of(input)
            val w=LocalHighlightWorkspace(File(root,"edits"),"content://synthetic/palette",input,baseline)
            val q=GlyphQuad(TextPoint(50f,80f),TextPoint(120f,80f),TextPoint(50f,94f),TextPoint(120f,94f))
            val h=HighlightEdit(UUID.randomUUID().toString(),0,0,"Palette quotation",listOf(q),rgb=palette[0])
            w.add(h)
            var saved=pdf(w.currentFile()){NativeHighlightEdits.list(it,0).single()}
            check(saved.edit.color==palette[0] && saved.edit.slot==0)
            val n=NotePolicy.place(0,0,TextPoint(200f,300f),TextBounds(0f,0f,595f,842f),"Color note").copy(rgb=palette[0])
            w.addNote(n)
            var note=pdf(w.currentFile()){NativeNoteEdits.list(it,0).single()}
            val before=w.contentHash
            store.write(AnnotationPalette.DEFAULT)
            check(w.contentHash==before && saved.edit.color==palette[0] && note.color==palette[0])
            w.replace(saved,saved.copy(contents="Edited quotation",mode=TextMode.MANUAL),UUID.randomUUID().toString())
            saved=pdf(w.currentFile()){NativeHighlightEdits.list(it,0).single()}
            check(saved.edit.color==palette[0] && saved.contents=="Edited quotation")
            w.replaceNote(note,note.copy(contents="Edited note"),UUID.randomUUID().toString())
            note=pdf(w.currentFile()){NativeNoteEdits.list(it,0).single()}
            check(note.color==palette[0] && note.contents=="Edited note")
            val colored=w.contentHash
            w.replace(saved,saved.copy(edit=saved.edit.copy(slot=1,rgb=palette[1])),UUID.randomUUID().toString())
            check(pdf(w.currentFile()){NativeHighlightEdits.list(it,0).single().edit.color}==palette[1])
            w.undo();check(w.contentHash==colored);w.redo()
            val recovered=LocalHighlightWorkspace(File(root,"edits"),"content://synthetic/palette",input,baseline)
            check(recovered.recoverExisting()!=null)
            check(pdf(recovered.currentFile()){NativeHighlightEdits.list(it,0).single().edit.color}==palette[1])
            check(pdf(recovered.currentFile()){NativeNoteEdits.list(it,0).single().color}==palette[0])
            val exported=File(root,"export.pdf")
            VerifiedDocumentExport.copy(w.fileForSave(),{exported.outputStream()},{exported.inputStream()})
            check(pdf(exported){NativeHighlightEdits.list(it,0).single().edit.color}==palette[1])
            check(pdf(exported){NativeNoteEdits.list(it,0).single().color}==palette[0])
            check(CheckpointFingerprint.of(input)==baseline)
        }catch(t:Throwable){failure=t}finally{root.deleteRecursively()}
        finish(if(failure==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(failure==null)"PASS: palette native RGB preference history recovery export"else"FAIL: native palette")
            failure?.let{putString("details",it.stackTraceToString())}
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
