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
import java.security.MessageDigest
import java.util.UUID

/** Isolated real native add/undo/redo/reopen workflow, not a Compose touch or external-save test. */
class SourceSaveInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun count(file:File):Int {
        val doc=Document.openDocument(file.absolutePath)
        try{val page=doc.loadPage(0) as PDFPage
            try{val all=page.annotations ?: emptyArray();try{return all.count{it.type==PDFAnnotation.TYPE_HIGHLIGHT}}finally{all.forEach{it.destroy()}}}
            finally{page.destroy()}
        }finally{doc.destroy()}
    }
    private fun edit(source:File,text:String,slot:Int):HighlightEdit {
        val doc=Document.openDocument(source.absolutePath)
        try{val page=doc.loadPage(0)
            try{val snapshot=MuPdfTextSnapshot.extract(page,true)
                val line=snapshot.lines.single{it.text==text}
                fun center(g:TextGlyph):TextPoint{val q=g.quad;return TextPoint((q.ul.x+q.ur.x+q.ll.x+q.lr.x)/4,(q.ul.y+q.ur.y+q.ll.y+q.lr.y)/4)}
                val selected=PageTextSelector.select(snapshot,center(line.glyphs.first()),center(line.glyphs.last()),false)
                check(selected is TextSelectionAttempt.Selected)
                return HighlightEdit.from(0,slot,selected.span)
            }finally{page.destroy()}
        }finally{doc.destroy()}
    }
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"source-save-proof-${UUID.randomUUID()}")
        var failure:Throwable?=null
        try{
            check(root.mkdirs());val source=File(root,"input.pdf");val bytes=fixture();source.writeBytes(bytes)
            val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
            val editRoot=File(root,"edits")
            val workspace=LocalHighlightWorkspace(editRoot,"content://synthetic/highlights",source,hash)
            check(workspace.recoverExisting()==null)


            val uri="content://synthetic/highlights";val journal=SourceSaveJournal(File(root,"save-journal"))
            fun save(expected:String)=journal.save(uri,expected,workspace.sourceOwnerHash,workspace.fileForSave(),
                {source.inputStream()},{source.outputStream()},NativePdfEditTransaction::validateEditable)
            val a=edit(source,"Local highlight one.",0)
            workspace.add(a);workspace.maintain();check(count(source)==0)
            val first=save(hash);check(count(source)==1 && journal.ownerFor(uri,first.sha256)==hash)
            val b=edit(source,"Local highlight two.",1)
            workspace.add(b);workspace.maintain();check(count(source)==1 && count(workspace.currentFile())==2)
            // Saved source plus later UNSAVED edit must reopen the ORIGINAL namespace/head.
            val adopted=LocalHighlightWorkspace(editRoot,uri,source,checkNotNull(journal.ownerFor(uri,first.sha256)))
            check(adopted.recoverExisting()!=null && count(adopted.currentFile())==2 && !adopted.canUndo)
            workspace.undo();workspace.maintain();check(save(first.sha256).sha256==first.sha256)
            workspace.undo();workspace.maintain();val undone=save(first.sha256);check(count(source)==0)
            workspace.redo();workspace.maintain();val redone=save(undone.sha256);check(count(source)==1)
            val savedBytes=source.readBytes();workspace.add(b);workspace.maintain()
            source.writeBytes(savedBytes+byteArrayOf(10));var opened=false;var conflict=false
            try{journal.save(uri,redone.sha256,hash,workspace.currentFile(),{source.inputStream()},
                {opened=true;source.outputStream()},NativePdfEditTransaction::validateEditable)}catch(_:SourceSaveConflict){conflict=true}
            check(conflict && !opened && source.readBytes().contentEquals(savedBytes+byteArrayOf(10)))
            // Only this self-authored isolated test resets its own source for a failure injection.
            source.writeBytes(savedBytes);var failed=false
            try{journal.save(uri,redone.sha256,hash,workspace.currentFile(),{source.inputStream()},
                {object:java.io.OutputStream(){val out=source.outputStream()
                    override fun write(value:Int){out.write(value);out.flush();throw java.io.IOException("Synthetic interruption")}
                    override fun close(){out.close()}}},NativePdfEditTransaction::validateEditable)}catch(_:java.io.IOException){failed=true}
            check(failed && count(workspace.currentFile())==2)
            val recovered=LocalHighlightWorkspace(editRoot,uri,source,hash)
            check(recovered.recoverExisting()!=null && count(recovered.currentFile())==2)
            val doc=Document.openDocument(recovered.currentFile().absolutePath)
            try{val page=doc.loadPage(0)
                try{val bitmap=com.artifex.mupdf.fitz.android.AndroidDrawDevice.drawPageFit(page,EditFramePolicy.EDIT_EDGE,EditFramePolicy.EDIT_EDGE)
                    try{check(bitmap.width>0 && bitmap.height>0 && kotlin.math.max(bitmap.width,bitmap.height)<=EditFramePolicy.EDIT_EDGE+2)}finally{bitmap.recycle()}
                }finally{page.destroy()}
            }finally{doc.destroy()}
        }catch(error:Throwable){failure=error}
        finally{root.deleteRecursively()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: source save and recovery"else"FAIL: source save and recovery")
            if(error!=null)putString("details",error.stackTraceToString())
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
