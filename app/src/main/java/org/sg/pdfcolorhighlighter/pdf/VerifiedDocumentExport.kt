// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import java.io.*
import java.security.MessageDigest

/** Copy an immutable local PDF to a NEW user-chosen document. No source writes, deletes,
 * rebinding, dirty-state clearing or provider atomicity/cloud-durability claim.
 * Caller serializes checkpoint ownership through verification. Failed targets can be partial.
 */
object VerifiedDocumentExport {
    const val MAX_BYTES = 256L * 1024 * 1024
    data class Receipt(val sizeBytes:Long,val sha256:String)
    private fun digest(input:InputStream,output:OutputStream?=null,maximum:Long):Receipt {
        require(maximum in 1..MAX_BYTES)
        val hash=MessageDigest.getInstance("SHA-256")
        val bytes=ByteArray(64*1024);var total=0L
        while(true){
            val n=input.read(bytes)
            if(n<0)break
            if(n==0)throw IOException("Non-progressing export stream")
            if(n.toLong()>maximum-total)throw IOException("Export size limit")
            output?.write(bytes,0,n);hash.update(bytes,0,n);total+=n
        }
        require(total>0){"Empty export"}
        return Receipt(total,hash.digest().joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')})
    }
    fun copy(source:File,openOutput:()->OutputStream,openReadBack:()->InputStream,
             maximum:Long=MAX_BYTES):Receipt {
        require(maximum in 1..MAX_BYTES && source.isFile && source.length() in 1..maximum)
        // Validate local identity BEFORE touching the destination; compare again after streaming.
        val expected=source.inputStream().use{digest(it,maximum=maximum)}
        val copied=source.inputStream().use{input->openOutput().use{output->
            digest(input,output,maximum).also{output.flush()}
        }}
        check(copied==expected){"Local export source changed"}
        val readBack=openReadBack().use{digest(it,maximum=maximum)}
        check(readBack==expected){"Export read-back mismatch"}
        return expected
    }
    fun suggestedName(name:String?,stamp:String):String {
        val leaf=(name ?: "document").substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\p{Cntrl}<>:\"/\\\\|?*]"),"_").trim().take(120)
        val stem=leaf.removeSuffix(".pdf").removeSuffix(".PDF").ifBlank{"document"}
        require(Regex("[0-9]{8}-[0-9]{6}-[0-9]{3}").matches(stamp))
        return "$stem-annotated-$stamp.pdf"
    }
}
