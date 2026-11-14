// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.data

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import org.sg.pdfcolorhighlighter.domain.AnnotationPalette

/** Preferences only, not annotation persistence. Access is serialized by the existing I/O worker. */
class PaletteStore(private val file:File) {
    private val atomic=AtomicFile(file)
    private fun readBytes()=atomic.openRead().use{input->
        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(128)
        while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=128);out.write(buffer,0,n)}
        out.toByteArray()
    }
    fun read():AnnotationPalette=try{AnnotationPalette.decode(readBytes())}catch(e:FileNotFoundException){
        if(file.exists() || File(file.path+".bak").exists())throw e
        AnnotationPalette.DEFAULT
    }
    fun write(p:AnnotationPalette){
        val bytes=AnnotationPalette.encode(p);val out=atomic.startWrite()
        try{out.write(bytes);atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
        check(readBytes().contentEquals(bytes)){"Palette read-back verification failed"}
    }
}
