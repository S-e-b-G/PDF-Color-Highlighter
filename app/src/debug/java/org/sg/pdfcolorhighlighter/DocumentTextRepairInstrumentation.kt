// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.util.Base64
import com.artifex.mupdf.fitz.Document
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.MuPdfTextSnapshot
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Self-authored synthetic font/PDF proof; contains no user document or extracted vendor font. */
class DocumentTextRepairInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun center(g:TextGlyph):TextPoint {val b=g.quad.bounds;return TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
    override fun onStart(){
        val source=File(targetContext.cacheDir,"document-text-repair-${UUID.randomUUID()}.pdf")
        var failure:Throwable?=null
        try{
            val bytes=fixture();source.writeBytes(bytes)
            val doc=Document.openDocument(source.absolutePath)
            try{
                check(doc.hasPermission(Document.PERMISSION_COPY))
                val page=doc.loadPage(0)
                val snapshot=try{MuPdfTextSnapshot.extract(page,true)}finally{page.destroy()}
                check(snapshot.status==PageTextStatus.READY)
                val first=snapshot.lines.first{it.text.contains("AFIS")}
                val last=snapshot.lines.first{it.text.contains("incose")}
                val mapped=snapshot.lines.flatMap{it.glyphs}.filter{it.sourceCodePoint==15 || it.sourceCodePoint==0xFFFD}
                check(mapped.size==2 && mapped.all{(it.codePoint==0x25CF || it.codePoint==0x2022) && it.unicodeReliable}){
                    "Reviewed legacy circle font did not normalize its native control/replacement representation"
                }
                check(mapped.any{it.sourceCodePoint==0xFFFD}){"Explicit replacement fixture did not reach the native callback"}
                val selection=PageTextSelector.select(snapshot,center(first.glyphs.first()),center(last.glyphs.last()),false)
                check(selection is TextSelectionAttempt.Selected){"Synthetic AFIS-to-INCOSE flow refused: $selection"}
                // PDF placement does not require a literal space after a list marker.
                // Still require both names, their order, and INCOSE on a separate bullet line.
                val incoseLine=Regex("""^[\t \u00A0\u202F]*[●•][\t \u00A0\u202F]*INCOSE\b""")
                for(sample in listOf("•INCOSE :", "● INCOSE :", "  •\u00A0INCOSE :"))
                    check(incoseLine.containsMatchIn(sample)){"Native assertion positive-format self-test failed"}
                for(sample in listOf("AFIS • INCOSE", "INCOSE", "• INCOSEx", "•"))
                    check(!incoseLine.containsMatchIn(sample)){"Native assertion negative-format self-test failed"}
                val quotation=selection.span.quotation
                val escaped=quotation.replace("\r", "<CR>").replace("\n", "<LF>")
                check(quotation.contains("AFIS") && quotation.contains("INCOSE") &&
                    quotation.indexOf("AFIS")<quotation.indexOf("INCOSE")){
                    "Synthetic quotation lost or reordered a name: $escaped"
                }
                check(quotation.lineSequence().any{incoseLine.containsMatchIn(it)}){
                    "Expected INCOSE on its own bullet line (marker spacing optional); synthetic quotation: $escaped"
                }
            }finally{doc.destroy()}
            check(source.readBytes().contentEquals(bytes)){"Read-only proof changed its source"}
        }catch(error:Throwable){failure=error}
        finally{source.delete()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: document text repair"else"FAIL: document text repair")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
    private fun fixture():ByteArray {
        fun ascii(s:String)=s.toByteArray(Charsets.US_ASCII)
        fun stream(bytes:ByteArray,extra:String=""):ByteArray =
            ascii("<< /Length ${bytes.size} $extra >>\nstream\n")+bytes+ascii("\nendstream")
        val font=Base64.decode("AQAEAQABAQELQWR2UDRDNEU3NAABAQEZ+BwC+BwD+BgE5eX4LvguBeYPi/cUEukRAAIBAQQkQzE1U0cgc3ludGhldGljIGxlZ2FjeS1zeW1ib2wgcHJvb2YAAAABhwACAQEEHfiIDviI+C73jhXjQ9MzM0NDMzPTQ+Pj09PjHg4=",Base64.DEFAULT)
        val content="""BT /F2 12 Tf 1 0 0 1 50 750 Tm <01> Tj
/F1 12 Tf 1 0 0 1 62 750 Tm (AFIS : systems association) Tj
1 0 0 1 62 732 Tm (https://example.invalid/afis) Tj
/F3 12 Tf 1 0 0 1 50 710 Tm <01> Tj
/F1 12 Tf 1 0 0 1 62 710 Tm (INCOSE : systems engineering) Tj
1 0 0 1 62 692 Tm (https://example.invalid/incose) Tj ET
"""
        val replacementCmap="""/CIDInit /ProcSet findresource begin
12 dict begin begincmap
/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def
/CMapName /SGReplacementProof def /CMapType 2 def
1 begincodespacerange <00> <FF> endcodespacerange
1 beginbfchar <01> <FFFD> endbfchar
endcmap CMapName currentdict /CMap defineresource pop end end
"""
        val objects=listOf(
            ascii("<< /Type /Catalog /Pages 2 0 R >>"),
            ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
            ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R /F2 6 0 R /F3 9 0 R >> >> /Contents 4 0 R >>"),
            stream(ascii(content)),
            ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"),
            ascii("<< /Type /Font /Subtype /Type1 /BaseFont /ABCDEF+AdvP4C4E74 /FirstChar 1 /LastChar 1 /Widths [500] /Encoding << /Type /Encoding /BaseEncoding /WinAnsiEncoding /Differences [1 /C15] >> /FontDescriptor 7 0 R >>"),
            ascii("<< /Type /FontDescriptor /FontName /ABCDEF+AdvP4C4E74 /Flags 4 /FontBBox [0 0 500 800] /ItalicAngle 0 /Ascent 800 /Descent -200 /CapHeight 700 /StemV 80 /FontFile3 8 0 R >>"),
            stream(font,"/Subtype /Type1C"),
            ascii("<< /Type /Font /Subtype /Type1 /BaseFont /ABCDEF+AdvP4C4E74 /FirstChar 1 /LastChar 1 /Widths [500] /Encoding << /Type /Encoding /BaseEncoding /WinAnsiEncoding /Differences [1 /C15] >> /FontDescriptor 7 0 R /ToUnicode 10 0 R >>"),
            stream(ascii(replacementCmap)))
        val out=ByteArrayOutputStream();val offsets=mutableListOf<Int>()
        fun append(s:String){out.write(ascii(s))}
        append("%PDF-1.4\n")
        objects.forEachIndexed{i,bytes->offsets.add(out.size());append("${i+1} 0 obj\n");out.write(bytes);append("\nendobj\n")}
        val xref=out.size();append("xref\n0 ${objects.size+1}\n0000000000 65535 f \n")
        offsets.forEach{append(it.toString().padStart(10,'0')+" 00000 n \n")}
        append("trailer\n<< /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }
}
