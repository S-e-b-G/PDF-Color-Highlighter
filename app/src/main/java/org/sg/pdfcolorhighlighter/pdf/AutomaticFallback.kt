// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf
import java.io.*
import java.security.MessageDigest

interface FallbackTarget {
    fun authorized():Boolean
    fun createNew(baseName:String,original:String):String
    fun read(id:String):InputStream
    fun write(id:String):OutputStream
}
class SourceFallbackFailure(val copyVerified:Boolean,cause:Throwable):IOException("Original Save failed; fallback verified="+copyVerified,cause)
object AutomaticFallback {
    fun baseName(original:String?,stamp:String):String {
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}").matches(stamp))
        val leaf=(original ?: "document").substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\p{Cntrl}<>:\"/\\\\|?*]"),"_").trim().trimStart('.').take(90)
        val stem=leaf.replace(Regex("(?i)\\.pdf$"),"").ifBlank{"document"}
        return "${stem}_$stamp.pdf"
    }
    fun freshName(base:String,names:Set<String>):String {
        if(base !in names)return base
        for(n in 2..4098){val next=base.removeSuffix(".pdf")+"_$n.pdf";if(next !in names)return next}
        error("Fallback name inventory limit")
    }
    fun copy(source:File,uri:String,name:String?,stamp:String,target:FallbackTarget):VerifiedDocumentExport.Receipt {
        require(uri.startsWith("content://") && source.isFile && source.length() in 1..VerifiedDocumentExport.MAX_BYTES)
        check(target.authorized());val id=target.createNew(baseName(name,stamp),uri)
        check(id!=uri && target.authorized())
        target.read(id).use{check(it.read()==-1){"Nonempty fallback target"}}
        return VerifiedDocumentExport.copy(source,{check(target.authorized());target.write(id)},{check(target.authorized());target.read(id)})
    }
    fun encode(uri:String?):ByteArray {
        require(uri==null || uri.startsWith("content://") && uri.length in 1..4096 && uri.none{it.isISOControl()})
        val body=if(uri==null)byteArrayOf()else{
            val b=Charsets.UTF_8.newEncoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(uri))
            ByteArray(b.remaining()).also{b.get(it)}
        }
        require(body.size<=8192)
        return body+MessageDigest.getInstance("SHA-256").digest(body)
    }
    fun decode(data:ByteArray):String? {
        require(data.size in 32..8224);val body=data.copyOfRange(0,data.size-32)
        require(MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(body),data.copyOfRange(data.size-32,data.size)))
        if(body.isEmpty())return null
        val value=Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(body)).toString()
        require(encode(value).contentEquals(data));return value
    }
}
