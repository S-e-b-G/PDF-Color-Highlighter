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

/** Native glyph geometry plus deliberately fragmented copied rows; never edits a user PDF. */
class PhysicalTextRowsInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun center(g:TextGlyph)=TextPoint((g.quad.bounds.left+g.quad.bounds.right)/2,(g.quad.bounds.top+g.quad.bounds.bottom)/2)
    override fun onStart(){
        val source=File(targetContext.cacheDir,"physical-rows-${UUID.randomUUID()}.pdf")
        var failure:Throwable?=null
        try{
            val bytes=fixture();source.writeBytes(bytes)
            val doc=Document.openDocument(source.absolutePath)
            try{
                check(doc.isPDF() && doc.countPages()==2 && doc.hasPermission(Document.PERMISSION_COPY))
                val page=doc.loadPage(0)
                val original=try{MuPdfTextSnapshot.extract(page,true)}finally{page.destroy()}
                val rows=original.lines.filter{it.text.trim()=="AB CD EF GH"}.sortedBy{it.bounds.top}
                check(rows.size==3){"Synthetic native row extraction mismatch"}
                val above=rows[0].copy(block=8,run=0);val body=rows[1].copy(block=8,run=0);val below=rows[2].copy(block=8,run=0)
                val words=mutableListOf<MutableList<TextGlyph>>();var word=mutableListOf<TextGlyph>()
                for(glyph in body.glyphs){
                    if(glyph.whitespace){if(word.isNotEmpty()){words.add(word);word=mutableListOf()}}
                    else word.add(glyph)
                }
                if(word.isNotEmpty())words.add(word)
                check(words.size==4)
                val fragments=words.map{PageTextLine(8,0,it.toList())}
                // Simulate separate native word-lines without whitespace glyphs; keep real native quads.
                val split=original.copy(lines=listOf(above)+fragments.reversed()+below)
                val normalized=TextRowCoalescer.normalize(split)
                val merged=normalized.lines.first{it.sourceFragments==4}
                check(merged.text=="AB CD EF GH" && merged.glyphs.size==8 && merged.spacesBefore.size==3)
                check(merged.glyphs==words.flatten())
                val a=TextPoint(merged.bounds.left-8f,(merged.bounds.top+merged.bounds.bottom)/2)
                val b=TextPoint(merged.bounds.right+8f,a.y)
                val selected=PageTextSelector.select(normalized,a,b)
                check(selected is TextSelectionAttempt.Selected && selected.span.quotation=="AB CD EF GH")
                check(selected.span.quads.size==8){"Logical spaces acquired invented highlight geometry"}
                val onlyWord=PageTextSelector.select(normalized,center(merged.glyphs[2]),center(merged.glyphs[3]))
                check(onlyWord is TextSelectionAttempt.Selected && onlyWord.span.quotation=="CD")
                check(PageTextSelector.select(normalized,b,a)==selected)
                check(TextRowCoalescer.normalize(normalized)===normalized)
                val columnPage=doc.loadPage(1)
                val columns=try{MuPdfTextSnapshot.extract(columnPage,true)}finally{columnPage.destroy()}
                val left=columns.lines.first{it.text.trim()=="LEFT COLUMN"};val right=columns.lines.first{it.text.trim()=="RIGHT COLUMN"}
                val crossing=PageTextSelector.select(columns,center(left.glyphs.first()),center(right.glyphs.last()))
                check(crossing==TextSelectionAttempt.Rejected(SelectionRejection.DIFFERENT_TEXT_FLOW))
            }finally{doc.destroy()}
            check(source.readBytes().contentEquals(bytes)){"Input changed during read-only proof"}
        }catch(error:Throwable){failure=error}
        finally{source.delete()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: physical text rows" else "FAIL: physical text rows")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
    private fun fixture():ByteArray{
        val paragraph="BT /F1 14 Tf 50 750 Td (AB CD EF GH) Tj 0 -18 Td (AB CD EF GH) Tj 0 -18 Td (AB CD EF GH) Tj ET\n"
        val columns="BT /F1 14 Tf 50 750 Td (LEFT COLUMN) Tj ET\nBT /F1 14 Tf 350 750 Td (RIGHT COLUMN) Tj ET\n"
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
