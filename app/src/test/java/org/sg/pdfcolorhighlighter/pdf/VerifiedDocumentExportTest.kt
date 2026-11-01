// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.nio.file.Files

class VerifiedDocumentExportTest {
    private fun fixture(action:(File)->Unit){
        val dir=Files.createTempDirectory("export-test").toFile()
        try{val source=File(dir,"source.pdf");source.writeBytes(ByteArray(150000){(it%251).toByte()});action(source)}
        finally{dir.deleteRecursively()}
    }
    private fun fails(action:()->Unit){try{action();fail("Expected export rejection")}catch(_:Exception){}}
    private fun copy(file:File,output:ByteArrayOutputStream=ByteArrayOutputStream())=
        VerifiedDocumentExport.copy(file,{output},{ByteArrayInputStream(output.toByteArray())})
    @Test fun exactMultiBufferCopy(){fixture{f->val out=ByteArrayOutputStream();val r=copy(f,out);assertArrayEquals(f.readBytes(),out.toByteArray());assertEquals(f.length(),r.sizeBytes)}}
    @Test fun hashIsSha256(){fixture{f->val r=copy(f);val hash=java.security.MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')};assertEquals(hash,r.sha256)}}
    @Test fun sourceUntouched(){fixture{f->val before=f.readBytes();copy(f);assertArrayEquals(before,f.readBytes())}}
    @Test fun emptySourceNeverOpensTarget(){fixture{f->f.writeBytes(byteArrayOf());var opened=false;fails{VerifiedDocumentExport.copy(f,{opened=true;ByteArrayOutputStream()},{ByteArrayInputStream(byteArrayOf())})};assertFalse(opened)}}
    @Test fun missingSourceRejected(){fixture{f->f.delete();fails{copy(f)}}}
    @Test fun maximumCheckedBeforeOutput(){fixture{f->var opened=false;fails{VerifiedDocumentExport.copy(f,{opened=true;ByteArrayOutputStream()},{f.inputStream()},10)};assertFalse(opened)}}
    @Test fun invalidMaximumRejected(){fixture{f->fails{VerifiedDocumentExport.copy(f,{ByteArrayOutputStream()},{f.inputStream()},0)}}}
    @Test fun missingOutputRejected(){fixture{f->fails{VerifiedDocumentExport.copy(f,{throw IOException("Unavailable")},{f.inputStream()})}}}
    @Test fun writeFailureRejected(){fixture{f->fails{VerifiedDocumentExport.copy(f,{object:OutputStream(){override fun write(b:Int){throw IOException("Full")}}},{f.inputStream()})}}}
    @Test fun closeFailureRejected(){fixture{f->fails{VerifiedDocumentExport.copy(f,{object:ByteArrayOutputStream(){override fun close(){throw IOException("Close failed")}}},{f.inputStream()})}}}
    @Test fun readBackUnavailableRejected(){fixture{f->fails{VerifiedDocumentExport.copy(f,{ByteArrayOutputStream()},{throw IOException("Unavailable")})}}}
    @Test fun corruptSameSizeRejected(){fixture{f->val bad=f.readBytes();bad[0]=(bad[0].toInt() xor 1).toByte();fails{VerifiedDocumentExport.copy(f,{ByteArrayOutputStream()},{ByteArrayInputStream(bad)})}}}
    @Test fun truncatedReadBackRejected(){fixture{f->fails{VerifiedDocumentExport.copy(f,{ByteArrayOutputStream()},{ByteArrayInputStream(byteArrayOf(1))})}}}
    @Test fun trailingBytesRejected(){fixture{f->fails{VerifiedDocumentExport.copy(f,{ByteArrayOutputStream()},{ByteArrayInputStream(f.readBytes()+byteArrayOf(1))})}}}
    @Test fun nameIsBoundedAndSafe(){val n=VerifiedDocumentExport.suggestedName("dir/report.pdf","20261101-214300-001");assertEquals("report-annotated-20261101-214300-001.pdf",n);assertFalse(VerifiedDocumentExport.suggestedName("bad:name?.pdf","20261101-214300-001").contains(':'))}
    @Test fun fallbackAndStampValidation(){assertEquals("document-annotated-20261101-214300-001.pdf",VerifiedDocumentExport.suggestedName(null,"20261101-214300-001"));fails{VerifiedDocumentExport.suggestedName("x","bad")}}
}
