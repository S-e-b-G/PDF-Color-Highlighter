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
class VerifiedExportInstrumentation:Instrumentation(){
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
            try{
                val hits=page.search(text,StructuredText.SEARCH_EXACT)
                check(hits.size==1)
                val quads=hits.single().map{q->GlyphQuad(TextPoint(q.ul_x,q.ul_y),TextPoint(q.ur_x,q.ur_y),TextPoint(q.ll_x,q.ll_y),TextPoint(q.lr_x,q.lr_y))}
                return HighlightEdit(UUID.randomUUID().toString(),0,slot,text,quads)
            }finally{page.destroy()}
        }finally{doc.destroy()}
    }
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"verified-export-proof-${UUID.randomUUID()}")
        var failure:Throwable?=null
        try{
            check(root.mkdirs());val source=File(root,"input.pdf");val bytes=fixture();source.writeBytes(bytes)
            val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
            val editRoot=File(root,"edits")
            val workspace=LocalHighlightWorkspace(editRoot,"content://synthetic/highlights",source,hash)
            check(workspace.recoverExisting()==null)
            val a=edit(source,"Local highlight one.",0);val b=edit(source,"Local highlight two.",1)
            workspace.add(a);check(count(workspace.currentFile())==1 && workspace.canUndo && !workspace.canRedo)
            workspace.add(b);check(count(workspace.currentFile())==2)
            workspace.undo();check(count(workspace.currentFile())==1 && workspace.canRedo)
            workspace.redo();check(count(workspace.currentFile())==2)
            workspace.maintain()
            val recovered=LocalHighlightWorkspace(editRoot,"content://synthetic/highlights",source,hash)
            val file=checkNotNull(recovered.recoverExisting())
            check(count(file)==2 && !recovered.canUndo && !recovered.canRedo)
            check(source.readBytes().contentEquals(bytes)){"Original synthetic PDF changed"}
            val exported=File(root,"export.pdf")
            val receipt=VerifiedDocumentExport.copy(file,{exported.outputStream()},{exported.inputStream()})
            check(receipt.sizeBytes==file.length() && count(exported)==2)
            check(exported.readBytes().contentEquals(file.readBytes()))
            workspace.undo()
            check(count(workspace.currentFile())==1 && count(exported)==2){"Export changed with later edits"}
            val doc=Document.openDocument(exported.absolutePath)
            try{val page=doc.loadPage(0) as PDFPage
                try{val all=page.annotations ?: emptyArray()
                    try{
                        check(all.map{it.contents}.toSet()==setOf(a.quotation,b.quotation))
                        check(all.all{it.quadPoints.isNotEmpty()})
                        check(all.map{it.color.toList()}.distinct().size==2){"Different slots lost their native colors"}
                        for(annotation in all){
                            val obj=annotation.`object`
                            try{
                                val mode=obj.get("SGCHTextMode")
                                try{check(mode.isString && mode.asString()==TextMode.AUTOMATIC.name)}finally{if(!mode.isNull)mode.destroy()}
                                val quote=obj.get("SGCHQuotation")
                                try{check(quote.isString && PdfTextString.decodeAppText(quote.asByteString())==annotation.contents)}finally{if(!quote.isNull)quote.destroy()}
                            }finally{obj.destroy()}
                        }
                    }finally{all.forEach{it.destroy()}}
                }finally{page.destroy()}
            }finally{doc.destroy()}
        }catch(error:Throwable){failure=error}
        finally{root.deleteRecursively()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: verified native export"else"FAIL: verified native export")
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
