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
import kotlin.math.max
import kotlin.math.min

/** Real native geometry, synthetic content, margin and cross-block regression. No PDF edits. */
class SelectionFlowInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?) {super.onCreate(arguments);start()}
    private fun center(line:PageTextLine)= (line.bounds.top+line.bounds.bottom)/2
    override fun onStart() {
        val source=File(targetContext.cacheDir,"selection-flow-${UUID.randomUUID()}.pdf")
        var failure:Throwable?=null
        try {
            val bytes=fixture();source.writeBytes(bytes)
            val doc=Document.openDocument(source.absolutePath)
            try {
                check(doc.isPDF() && doc.countPages()==2 && doc.hasPermission(Document.PERMISSION_COPY))
                val page=doc.loadPage(0)
                val original=try{MuPdfTextSnapshot.extract(page,true)}finally{page.destroy()}
                check(original.status==PageTextStatus.READY)
                val firstIndex=original.lines.indexOfFirst{it.text.trim()=="Le texte sélectionné se poursuit"}
                val lastIndex=original.lines.indexOfFirst{it.text.trim()=="sur la ligne suivante."}
                check(firstIndex>=0 && lastIndex>=0)
                // Force different IDs while preserving the actual native glyph geometry.
                // Native block grouping varies by PDF; the selection must not depend on equal IDs.
                val split=original.copy(lines=original.lines.mapIndexed{index,line->line.copy(block=index+100)})
                val first=split.lines[firstIndex];val last=split.lines[lastIndex]
                val a=TextPoint(max(split.pageBounds.left,first.bounds.left-12f),center(first))
                val b=TextPoint(min(split.pageBounds.right,last.bounds.right+12f),center(last))
                val selected=PageTextSelector.select(split,a,b)
                check(selected is TextSelectionAttempt.Selected){"Native margin/multiline selection refused: $selected"}
                val span=(selected as TextSelectionAttempt.Selected).span
                check(span.quotation=="Le texte sélectionné se poursuit sur la ligne suivante.")
                check(span.visualText.contains('\n') && span.quads.isNotEmpty())
                check(PageTextSelector.select(split,b,a)==selected)
                check(PageTextSelector.select(split,TextPoint(590f,830f),a)==
                    TextSelectionAttempt.Rejected(SelectionRejection.NO_GLYPH))
                val columnPage=doc.loadPage(1)
                val columns=try{MuPdfTextSnapshot.extract(columnPage,true)}finally{columnPage.destroy()}
                val left=columns.lines.first{it.text.trim()=="COLONNE GAUCHE"}
                val right=columns.lines.first{it.text.trim()=="COLONNE DROITE"}
                val leftStart=TextPoint(max(columns.pageBounds.left,left.bounds.left-12f),center(left))
                val leftEnd=TextPoint(left.bounds.right+10f,center(left))
                val within=PageTextSelector.select(columns,leftStart,leftEnd)
                check(within is TextSelectionAttempt.Selected && within.span.quotation=="COLONNE GAUCHE")
                val cross=PageTextSelector.select(columns,leftStart,TextPoint(right.bounds.right,center(right)))
                check(cross==TextSelectionAttempt.Rejected(SelectionRejection.DIFFERENT_TEXT_FLOW))
            }finally{doc.destroy()}
            check(source.readBytes().contentEquals(bytes)){"Read-only regression changed input bytes"}
        }catch(error:Throwable){failure=error}
        finally{source.delete()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: selection flow margins" else "FAIL: selection flow margins")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
    private fun fixture():ByteArray {
        fun hex(value:String)=value.toByteArray(Charsets.ISO_8859_1).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
        val paragraph="BT /F1 14 Tf 50 750 Td <${hex("Le texte sélectionné se poursuit")}> Tj ET\n"+
            "BT /F1 14 Tf 50 732 Td <${hex("sur la ligne suivante.")}> Tj ET\n"
        val columns="BT /F1 14 Tf 50 750 Td <${hex("COLONNE GAUCHE")}> Tj ET\n"+
            "BT /F1 14 Tf 350 750 Td <${hex("COLONNE DROITE")}> Tj ET\n"
        fun stream(value:String)="<< /Length ${value.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${value}endstream"
        fun page(contents:Int)="<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 7 0 R >> >> /Contents $contents 0 R >>"
        val objects=listOf("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R 5 0 R] /Count 2 >>",
            page(4),stream(paragraph),page(6),stream(columns),"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        val output=ByteArrayOutputStream()
        fun append(value:String){output.write(value.toByteArray(Charsets.US_ASCII))}
        append("%PDF-1.4\n");val offsets=mutableListOf<Int>()
        objects.forEachIndexed{index,value->offsets.add(output.size());append("${index+1} 0 obj\n$value\nendobj\n")}
        val xref=output.size();append("xref\n0 ${objects.size+1}\n0000000000 65535 f \n")
        offsets.forEach{append(it.toString().padStart(10,'0')+" 00000 n \n")}
        append("trailer\n<< /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return output.toByteArray()
    }
}
