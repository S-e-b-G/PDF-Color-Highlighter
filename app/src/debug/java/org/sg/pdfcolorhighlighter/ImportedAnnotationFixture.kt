// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

/** Self-authored standard PDF; no private/user PDF or SGCH ownership metadata. */
object ImportedAnnotationFixture{
    fun bytes(locked:Boolean=false,duplicate:Boolean=false,unnamed:Boolean=false,rich:Boolean=false,notePopup:Boolean=false,wrongPopupParent:Boolean=false,thread:Boolean=false):ByteArray{
        fun hex(s:String)="<FEFF"+s.toByteArray(Charsets.UTF_16BE).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}+">"
        val content="BT /F1 12 Tf 50 750 Td (AUTOTEST Alpha beta gamma delta epsilon.) Tj 0 -24 Td (AUTOTEST Second row for resize and citation.) Tj ET"
        val hname=if(unnamed)"" else "/NM (external-highlight)"
        val nname=if(unnamed)"" else if(duplicate)"/NM (external-highlight)"else "/NM (external-note)"
        val baseObjects=listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R /Annots [6 0 R 7 0 R${if(notePopup) " 8 0 R" else ""}${if(thread) " 9 0 R" else ""}] >>",
            "<< /Length ${content.toByteArray(Charsets.US_ASCII).size} >>\nstream\n$content\nendstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
            "<< /Type /Annot /Subtype /Highlight /Rect [50 746 370 762] /QuadPoints [50 762 370 762 50 746 370 746] /C [0.13 0.42 0.71] /CA 0.37 /F ${if(locked)128 else 4} $hname /Contents ${hex("External quotation – élève")} /T (External author) /SGFixtureKeep (preserve) ${if(rich)"/RC (unsupported rich text)"else ""} >>",
            "<< /Type /Annot /Subtype /Text /Rect [430 400 454 424] /Name /Note ${if(notePopup) "/Popup 8 0 R" else ""} /C [0.63 0.24 0.51] /CA 0.43 /F 4 $nname /Contents ${hex("External note – cœur")} /T (External author) /SGFixtureKeep (preserve) ${if(thread) "/RC (<body>Parent</body>)" else ""} >>"
        )
        require(!thread || notePopup)
        val withPopup=if(notePopup) baseObjects+"<< /Type /Annot /Subtype /Popup /Rect [460 400 560 480] /Parent ${if(wrongPopupParent) 6 else 7} 0 R /P 3 0 R >>" else baseObjects
        val objects=if(thread) withPopup+"<< /Type /Annot /Subtype /Text /Rect [470 370 494 394] /C [1 1 0] /F 4 /NM (reply) /Contents (Reply) /RC (<body>Reply</body>) /IRT 7 0 R >>" else withPopup
        val output=java.io.ByteArrayOutputStream();val offsets=mutableListOf<Int>()
        fun append(s:String){output.write(s.toByteArray(Charsets.US_ASCII))}
        append("%PDF-1.4\n");objects.forEachIndexed{i,obj->offsets.add(output.size());append("${i+1} 0 obj\n$obj\nendobj\n")}
        val xref=output.size();append("xref\n0 ${objects.size+1}\n0000000000 65535 f \n")
        offsets.forEach{append(it.toString().padStart(10,'0')+" 00000 n \n")}
        append("trailer << /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return output.toByteArray()
    }
}
