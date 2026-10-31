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
import kotlin.math.abs

/** Synthetic PDF geometry plus explicit copied row fragmentation; no user PDF is bundled. */
class InlineReferenceRowsInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    override fun onStart(){
        val source=File(targetContext.cacheDir,"inline-reference-${UUID.randomUUID()}.pdf")
        var failure:Throwable?=null
        try{
            val bytes=fixture();source.writeBytes(bytes)
            val doc=Document.openDocument(source.absolutePath)
            try{
                check(doc.hasPermission(Document.PERMISSION_COPY))
                val page=doc.loadPage(0)
                val extracted=try{MuPdfTextSnapshot.extract(page,true)}finally{page.destroy()}
                val fragments=mutableListOf<PageTextLine>()
                for((block,id) in listOf("9277","9320").withIndex()){
                    val label=extracted.lines.first{it.text.contains(id)}
                    val row=extracted.lines.filter{abs(it.bounds.top-label.bounds.top)<=label.bounds.height*.2f}
                        .flatMap{it.glyphs}.sortedBy{it.quad.bounds.left}
                        .dropWhile{it.whitespace}.dropLastWhile{it.whitespace}
                    val split=row.indexOfFirst{it.codePoint=='('.code}
                    check(split>0){"Native synthetic label/year geometry missing"}
                    val left=row.take(split).dropLastWhile{it.whitespace}
                    val right=row.drop(split).dropWhile{it.whitespace}
                    check(right.isNotEmpty())
                    fragments.add(PageTextLine(block,0,left))
                    fragments.add(PageTextLine(block,0,right))
                    val body=extracted.lines.first{it.text.contains(if(block==0)"First description"else"Second description")}
                    fragments.add(body.copy(block=block,run=0,sourceFragments=1))
                }
                val copied=PageTextSnapshot(extracted.pageBounds,fragments,PageTextStatus.READY)
                val normalized=InlineReferenceRows.normalize(copied)
                check(normalized.lines.size==4){"Detached synthetic years were not normalized into complete headings"}
                check(normalized.lines.sumOf{it.glyphs.size}==copied.lines.sumOf{it.glyphs.size})
                val a=center(normalized.lines.first().glyphs.first())
                val b=center(normalized.lines.last().glyphs.last())
                val selected=PageTextSelector.select(normalized,a,b,false)
                check(selected is TextSelectionAttempt.Selected){"Two-reference selection refused: $selected"}
                val q=selected.span.quotation
                val escaped=q.replace("\n","<LF>")
                check(q.contains("9277:2013 (2013).") && q.contains("9320:2013 (2013)")){
                    "Synthetic quotation omitted a heading year: $escaped"
                }
                check(PageTextSelector.select(normalized,b,a,false)==selected)
            }finally{doc.destroy()}
            check(source.readBytes().contentEquals(bytes)){"Read-only synthetic source changed"}
        }catch(error:Throwable){failure=error}
        finally{source.delete()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: inline reference rows"else"FAIL: inline reference rows")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
    private fun fixture():ByteArray {
        val content="""BT /F1 8 Tf
1 0 0 1 50 730 Tm (prEN 9277:2013) Tj
1 0 0 1 124 730 Tm (\(2013\).) Tj
1 0 0 1 40 716 Tm (First description of this synthetic standard.) Tj
1 0 0 1 50 704 Tm (prEN 9320:2013) Tj
1 0 0 1 124 704 Tm (\(2013\)) Tj
1 0 0 1 40 690 Tm (Second description of this synthetic standard.) Tj ET
"""
        fun stream(s:String)="<< /Length ${s.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${s}endstream"
        val objects=listOf("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
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
