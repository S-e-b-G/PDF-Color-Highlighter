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
class HighlightAppearanceInstrumentation:Instrumentation(){
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
        val root=File(targetContext.noBackupFilesDir,"appearance-proof-${UUID.randomUUID()}")
        var failure:Throwable?=null
        try{
            check(root.mkdirs());val source=File(root,"input.pdf");val bytes=fixture();source.writeBytes(bytes)
            val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
            val editRoot=File(root,"edits")
            val workspace=LocalHighlightWorkspace(editRoot,"content://synthetic/highlights",source,hash)
            check(workspace.recoverExisting()==null)

            val a=edit(source,"Local highlight one.",0)
            check(a.quads.size>1 && a.appearance.size==1)
            workspace.add(a);workspace.maintain()
            fun state():ManagedHighlight {
                val doc=Document.openDocument(workspace.currentFile().absolutePath) as PDFDocument
                try{return NativeHighlightEdits.list(doc,0).single()}finally{doc.destroy()}
            }
            val current=state()
            check(current.edit.quads==a.quads && current.edit.appearance.size==1)
            val doc=Document.openDocument(workspace.currentFile().absolutePath) as PDFDocument
            try{val page=doc.loadPage(0) as PDFPage
                try{val annotations=page.annotations ?: emptyArray()
                    try{
                        check(annotations.single().quadPointCount==1){"Saved native highlight is still per-character"}
                        val snapshot=MuPdfTextSnapshot.extract(page,true)
                        check(ManagedHighlightSelection.span(snapshot,current)!=null){"Row appearance lost character endpoints"}
                    }finally{annotations.forEach{it.destroy()}}
                }finally{page.destroy()}
            }finally{doc.destroy()}
            val manual=current.copy(contents="e\u0301\nmanual citation",mode=TextMode.MANUAL)
            workspace.replace(current,manual,UUID.randomUUID().toString());workspace.maintain()
            check(state().contents==manual.contents && state().mode==TextMode.MANUAL)
            val second=edit(source,"Local highlight two.",3)
            val before=state()
            workspace.replace(before,before.copy(edit=before.edit.copy(quotation=second.quotation,quads=second.quads,appearance=second.appearance)),UUID.randomUUID().toString())
            workspace.maintain();check(state().contents==manual.contents && state().edit.quads==second.quads)
            val encoded=workspace.currentFile().readBytes()
            workspace.replace(state(),null,UUID.randomUUID().toString());workspace.maintain()
            check(count(workspace.currentFile())==0)
            workspace.undo();workspace.maintain()
            check(workspace.currentFile().readBytes().contentEquals(encoded))
            val reopened=LocalHighlightWorkspace(editRoot,"content://synthetic/highlights",source,hash)
            check(reopened.recoverExisting()!=null)
            val checkDoc=Document.openDocument(reopened.currentFile().absolutePath) as PDFDocument
            try{check(NativeHighlightEdits.list(checkDoc,0).single().contents==manual.contents)}finally{checkDoc.destroy()}
            check(source.readBytes().contentEquals(bytes))
        }catch(error:Throwable){failure=error}
        finally{root.deleteRecursively()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: highlight appearance"else"FAIL: highlight appearance")
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
