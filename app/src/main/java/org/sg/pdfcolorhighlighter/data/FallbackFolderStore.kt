// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.data
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import org.sg.pdfcolorhighlighter.pdf.AutomaticFallback

/** Serialized preference only. Digest commit marker prevents unverified activation. */
class FallbackFolderStore(private val path:File){
    private val data=AtomicFile(path)
    private val markPath=File(path.path+".confirmed")
    private val mark=AtomicFile(markPath)
    private fun read(a:AtomicFile,max:Int)=a.openRead().use{input->
        val out=java.io.ByteArrayOutputStream();val b=ByteArray(256)
        while(true){val n=input.read(b);if(n<0)break;check(n>0 && out.size()+n<=max);out.write(b,0,n)};out.toByteArray()
    }
    private fun hash(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b)
    fun read():String? {
        val bytes=try{read(data,8224)}catch(e:FileNotFoundException){
            if(path.exists() || File(path.path+".bak").exists() || markPath.exists() || File(markPath.path+".bak").exists())throw e
            return null
        }
        check(MessageDigest.isEqual(read(mark,32),hash(bytes)));return AutomaticFallback.decode(bytes)
    }
    private fun write(a:AtomicFile,b:ByteArray){val out=a.startWrite();try{out.write(b);a.finishWrite(out)}catch(e:Exception){a.failWrite(out);throw e}}
    fun write(uri:String?){val bytes=AutomaticFallback.encode(uri);write(data,bytes);check(read(data,8224).contentEquals(bytes));write(mark,hash(bytes));check(read()==uri)}
}
