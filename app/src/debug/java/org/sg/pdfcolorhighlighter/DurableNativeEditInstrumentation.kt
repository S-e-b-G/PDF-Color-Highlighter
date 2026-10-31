// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFPage
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.StructuredText
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

/** Isolated durable native checkpoint proof; never edits the reader session or a user PDF. */
class DurableNativeEditInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private val quotation="Durable : élève, été."
    private fun validate(file:File){
        val doc=Document.openDocument(file.absolutePath)
        try{check(doc is PDFDocument && !doc.needsPassword() && doc.countPages()==1)}finally{doc.destroy()}
    }
    private fun verifyHighlight(file:File){
        val doc=Document.openDocument(file.absolutePath)
        try{
            val page=doc.loadPage(0) as PDFPage
            try{
                val annotations=page.annotations ?: emptyArray()
                try{
                    check(annotations.size==1 && annotations.single().type==PDFAnnotation.TYPE_HIGHLIGHT)
                    check(annotations.single().contents==quotation){"Persisted synthetic highlight lost its Unicode Contents"}
                    check(annotations.single().quadPoints.isNotEmpty())
                }finally{annotations.forEach{it.destroy()}}
            }finally{page.destroy()}
        }finally{doc.destroy()}
    }
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"durable-edit-proof-${UUID.randomUUID()}")
        var failure:Throwable?=null
        try{
            check(root.mkdirs())
            val source=File(root,"input.pdf");val original=fixture();source.writeBytes(original)
            val directory=File(root,"checkpoints")
            val store=DurablePdfRevisionStore(directory)
            val baseline=store.initialize(source,NativePdfEditTransaction::validateEditable)
            val operation=UUID.randomUUID().toString()
            val committed=NativePdfEditTransaction.commit(store,baseline,operation){pdf->
                check(pdf.hasPermission(Document.PERMISSION_COPY))
                val page=pdf.loadPage(0) as PDFPage
                try{
                    // Preserve the accepted JNI workaround: Page.search, not StructuredText.search.
                    val hits=page.search(quotation,StructuredText.SEARCH_EXACT)
                    check(hits.size==1 && hits.single().isNotEmpty())
                    val annotation=page.createAnnotation(PDFAnnotation.TYPE_HIGHLIGHT)
                    try{
                        annotation.setQuadPoints(hits.single())
                        annotation.setColor(floatArrayOf(1f,173f/255f,91f/255f))
                        annotation.setOpacity(.5f)
                        annotation.setFlags(annotation.flags or PDFAnnotation.IS_PRINT)
                        annotation.setContents(quotation)
                        val obj=annotation.`object`
                        try{obj.put("NM",operation)}finally{obj.destroy()}
                        annotation.update()
                    }finally{annotation.destroy()}
                    page.update()
                }finally{page.destroy()}
            }
            check(committed.sequence==1L)
            val fresh=DurablePdfRevisionStore(directory)
            check(fresh.recover()==committed)
            val committedFile=fresh.currentFile(committed)
            verifyHighlight(committedFile)
            // Simulate interruption after a fully validated PDF is published but BEFORE its head slot.
            val interrupted=DurablePdfRevisionStore(directory){stage->
                if(stage==RevisionStage.PDF_PUBLISHED)throw IOException("Synthetic interrupted checkpoint")
            }
            val failed=runCatching{
                interrupted.commit(committed,UUID.randomUUID().toString(),writePdf={ output ->
                    committedFile.inputStream().use{inputStream->output.outputStream().use{inputStream.copyTo(it)}}
                },validatePdf=::validate)
            }
            check(failed.isFailure && fresh.recover()==committed)
            verifyHighlight(fresh.currentFile(committed))
            var repeatedWrites=0
            val retry=NativePdfEditTransaction.commit(fresh,baseline,operation){repeatedWrites++;throw IOException("Duplicate mutation must not run")}
            check(retry==committed && repeatedWrites==0)
            check(fresh.pruneOld())
            check(source.readBytes().contentEquals(original)){"Original synthetic PDF was modified"}
        }catch(error:Throwable){failure=error}
        finally{root.deleteRecursively()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: durable native edit"else"FAIL: durable native edit")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
    private fun fixture():ByteArray {
        val hex=quotation.toByteArray(Charsets.ISO_8859_1).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
        val content="BT /F1 12 Tf 50 750 Td <$hex> Tj ET\n"
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
