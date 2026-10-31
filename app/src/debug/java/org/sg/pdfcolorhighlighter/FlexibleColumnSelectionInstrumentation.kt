// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.Document
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.MuPdfTextSnapshot
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Self-authored native text/paragraph/list range proof, not a UI gesture or user-PDF export. */
class FlexibleColumnSelectionInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    override fun onStart(){
        val source=File(targetContext.cacheDir,"flexible-column-${UUID.randomUUID()}.pdf")
        var failure:Throwable?=null
        try{
            val bytes=fixture();source.writeBytes(bytes)
            val doc=Document.openDocument(source.absolutePath)
            try{
                check(doc.hasPermission(Document.PERMISSION_COPY))
                val page=doc.loadPage(0)
                val snapshot=try{MuPdfTextSnapshot.extract(page,true)}finally{page.destroy()}
                val first=snapshot.lines.first{it.text.contains("Introductory")}
                val last=snapshot.lines.first{it.text.contains("Final paragraph")}
                val a=center(first.glyphs.first());val b=center(last.glyphs.last())
                val selected=PageTextSelector.select(snapshot,a,b,false)
                check(selected is TextSelectionAttempt.Selected){"Synthetic broad paragraph/list range refused: $selected"}
                val q=selected.span.quotation
                check(listOf("Introductory","first item","second item","third item","Final paragraph").all{q.contains(it)}){
                    "Synthetic broad range lost text"
                }
                check(selected.span.glyphs.count{it.codePoint==0x2022}==3){"Synthetic range lost a list marker"}
                check(PageTextSelector.select(snapshot,b,a,false)==selected)
            }finally{doc.destroy()}
            check(source.readBytes().contentEquals(bytes)){"Read-only synthetic source changed"}
        }catch(error:Throwable){failure=error}
        finally{source.delete()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: flexible column selection"else"FAIL: flexible column selection")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
    private fun fixture():ByteArray {
        val content="""BT /F1 12 Tf
1 0 0 1 100 750 Tm (Introductory sentence for the following list:) Tj
1 0 0 1 118 720 Tm <95> Tj
1 0 0 1 136 720 Tm (first item with delegates) Tj
1 0 0 1 118 690 Tm <9520> Tj (second item with directors) Tj
1 0 0 1 118 660 Tm <9520> Tj (third item with studies) Tj
1 0 0 1 100 570 Tm (Final paragraph after a large vertical gap.) Tj ET
"""
        fun stream(s:String)="<< /Length ${s.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${s}endstream"
        val objects=listOf("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 596 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            stream(content),"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
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
