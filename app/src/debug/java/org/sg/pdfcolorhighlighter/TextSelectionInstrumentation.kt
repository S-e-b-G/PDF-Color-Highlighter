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

/** Read-only synthetic native proof; no user PDFs, overlays, annotation creation or saving. */
class TextSelectionInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);start() }
    private fun center(glyph: TextGlyph)=TextPoint((glyph.quad.bounds.left+glyph.quad.bounds.right)/2,
        (glyph.quad.bounds.top+glyph.quad.bounds.bottom)/2)
    override fun onStart() {
        val source=File(targetContext.cacheDir,"text-selection-proof-${UUID.randomUUID()}.pdf")
        var failure:Throwable?=null
        try {
            val original=fixture();source.writeBytes(original)
            val doc=Document.openDocument(source.absolutePath)
            try {
                check(doc.isPDF() && doc.countPages()==3)
                val copyAllowed=doc.hasPermission(Document.PERMISSION_COPY)
                check(copyAllowed)
                val page=doc.loadPage(0)
                val snapshot=try {
                    val restricted=MuPdfTextSnapshot.extract(page,false)
                    check(restricted.status==PageTextStatus.COPY_RESTRICTED && restricted.lines.isEmpty())
                    MuPdfTextSnapshot.extract(page,copyAllowed)
                } finally { page.destroy() }
                // Page and StructuredText are destroyed: the copied snapshot must still be usable.
                check(snapshot.status==PageTextStatus.READY)
                val first=snapshot.lines.first { it.text.trim()=="Le texte sélectionné se poursuit" }
                val last=snapshot.lines.first { it.text.trim()=="sur la ligne suivante." }
                val a=center(first.glyphs.first { !it.whitespace })
                val b=center(last.glyphs.last { !it.whitespace })
                val forward=PageTextSelector.select(snapshot,a,b)
                check(forward is TextSelectionAttempt.Selected) { "Native multiline selection rejected: $forward" }
                val span=(forward as TextSelectionAttempt.Selected).span
                check(span.quotation=="Le texte sélectionné se poursuit sur la ligne suivante.")
                check(span.visualText.contains('\n'))
                check(span.quads.isNotEmpty() && span.quads.all { it.area>0.0 })
                check(PageTextSelector.select(snapshot,b,a)==forward)
                check(PageTextSelector.select(snapshot,TextPoint(590f,830f),a) is TextSelectionAttempt.Rejected)
                val columnsPage=doc.loadPage(1)
                val columns=try { MuPdfTextSnapshot.extract(columnsPage,copyAllowed) } finally { columnsPage.destroy() }
                val left=columns.lines.first { it.text.trim()=="COLONNE GAUCHE" }
                val right=columns.lines.first { it.text.trim()=="COLONNE DROITE" }
                val crossing=PageTextSelector.select(columns,center(left.glyphs.first()),center(right.glyphs.last()))
                check(crossing==TextSelectionAttempt.Rejected(SelectionRejection.DIFFERENT_TEXT_FLOW)) {
                    "Column crossing was not rejected: $crossing"
                }
                val emptyPage=doc.loadPage(2)
                val empty=try { MuPdfTextSnapshot.extract(emptyPage,copyAllowed) } finally { emptyPage.destroy() }
                check(empty.status==PageTextStatus.EMPTY && empty.lines.isEmpty())
            } finally { doc.destroy() }
            check(source.readBytes().contentEquals(original)) { "Read-only proof changed input" }
        } catch(error:Throwable) { failure=error }
        finally { source.delete() }
        val error=failure
        finish(if(error==null) Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply {
            putString("result",if(error==null) "PASS: structured text selection" else "FAIL: structured text selection")
            if(error!=null) putString("details",error.stackTraceToString())
        })
    }

    private fun fixture():ByteArray {
        fun hex(value:String)=value.toByteArray(Charsets.ISO_8859_1).joinToString("") {
            (it.toInt() and 255).toString(16).padStart(2,'0')
        }
        val paragraph="BT /F1 14 Tf 50 750 Td <${hex("Le texte sélectionné se poursuit")}> Tj 0 -18 Td <${hex("sur la ligne suivante.")}> Tj ET\n"
        val columns="BT /F1 14 Tf 50 750 Td <${hex("COLONNE GAUCHE")}> Tj ET\n"+
            "BT /F1 14 Tf 350 750 Td <${hex("COLONNE DROITE")}> Tj ET\n"
        fun stream(value:String)="<< /Length ${value.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${value}endstream"
        fun page(contents:Int)="<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 9 0 R >> >> /Contents $contents 0 R >>"
        val objects=listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R 5 0 R 7 0 R] /Count 3 >>",
            page(4),stream(paragraph),page(6),stream(columns),page(8),stream(""),
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        val output=ByteArrayOutputStream()
        fun append(value:String) { output.write(value.toByteArray(Charsets.US_ASCII)) }
        append("%PDF-1.4\n")
        val offsets=mutableListOf<Int>()
        objects.forEachIndexed { index,value -> offsets.add(output.size());append("${index+1} 0 obj\n$value\nendobj\n") }
        val xref=output.size()
        append("xref\n0 ${objects.size+1}\n0000000000 65535 f \n")
        offsets.forEach { append(it.toString().padStart(10,'0')+" 00000 n \n") }
        append("trailer\n<< /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return output.toByteArray()
    }
}
