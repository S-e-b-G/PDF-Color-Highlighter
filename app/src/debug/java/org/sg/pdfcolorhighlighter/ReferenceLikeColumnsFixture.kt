// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter
import java.io.ByteArrayOutputStream

object ReferenceLikeColumnsFixture {
    fun bytes():ByteArray {
        val content=buildString{
            append("BT /F1 14 Tf 1 0 0 1 50 740 Tm (LEFT column heading) Tj ET\n")
            for(i in 0 until 6){val c=('A'.code+i).toChar()
                append("BT /F1 7 Tf 1 0 0 1 50 ${715-i*18} Tm (LEFT $c body words for continuation.) Tj ET\n")
                append("BT /F1 7 Tf 1 0 0 1 310 ${740-i*18} Tm (RIGHT $c body words for continuation.) Tj ET\n")
            }
            append("BT /F1 7 Tf 0 1 -1 0 22 640 Tm (SYNTHETIC OUTER MARGIN NOTICE FOR REGRESSION ONLY) Tj ET\n")
            append("BT /F1 6 Tf 1 0 0 1 180 65 Tm (SYNTHETIC CENTER FOOTER) Tj ET\n")
        }
        val objects=listOf("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Length ${content.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${content}endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        val out=ByteArrayOutputStream();val offsets=mutableListOf<Int>()
        fun put(s:String){out.write(s.toByteArray(Charsets.US_ASCII))}
        put("%PDF-1.4\n");objects.forEachIndexed{i,x->offsets.add(out.size());put("${i+1} 0 obj\n$x\nendobj\n")}
        val xref=out.size();put("xref\n0 ${objects.size+1}\n0000000000 65535 f \n")
        offsets.forEach{put(it.toString().padStart(10,'0')+" 00000 n \n")}
        put("trailer\n<< /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }
}
