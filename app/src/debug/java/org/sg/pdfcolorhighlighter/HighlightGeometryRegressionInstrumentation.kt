// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter
import android.app.Activity
import android.app.Instrumentation
import android.graphics.Color
import android.os.Bundle
import com.artifex.mupdf.fitz.*
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Numeric geometry from supplied PDF only; no original page text, watermark or PDF bytes. */
class HighlightGeometryRegressionInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun blank():ByteArray{
        val out=ByteArrayOutputStream();val offsets=mutableListOf(0)
        fun write(s:String){out.write(s.toByteArray(Charsets.US_ASCII))}
        write("%PDF-1.7\n")
        val objects=listOf("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << >> /Contents 4 0 R >>","<< /Length 0 >>\nstream\n\nendstream")
        for((i,obj)in objects.withIndex()){offsets.add(out.size());write("${i+1} 0 obj\n$obj\nendobj\n")}
        val xref=out.size();write("xref\n0 5\n0000000000 65535 f \n")
        for(offset in offsets.drop(1))write(offset.toString().padStart(10,'0')+" 00000 n \n")
        write("trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n");return out.toByteArray()
    }
    private fun list(file:File):List<ManagedHighlight>{
        val doc=Document.openDocument(file.absolutePath) as PDFDocument
        try{return NativeHighlightEdits.list(doc,0)}finally{doc.destroy()}
    }
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"m3q-numeric-proof-${UUID.randomUUID()}")
        var failure:Throwable?=null
        try{
            check(root.mkdirs())
            val cases=targetContext.resources.openRawResource(R.raw.m3q_geometry).bufferedReader().use{it.readLines()}
                .filter{it.startsWith("SGQ1|")}.map{HighlightGeometryCodec.decode(it)}
            check(cases.map{it.size}==listOf(38,645))
            val original=File(root,"blank.pdf").apply{writeBytes(blank())};val source=File(root,"old-appearance.pdf")
            val doc=Document.openDocument(original.absolutePath) as PDFDocument
            try{
                cases.forEachIndexed{i,qs->
                    NativeHighlightEdits.add(doc,HighlightEdit(UUID.randomUUID().toString(),0,if(i==0)0 else 3,"Numeric regression $i",qs,qs))
                }
                val before=NativeHighlightEdits.list(doc,0)
                before.forEach{h->NativeHighlightEdits.replace(doc,h,h.copy(contents="Manual fixture preserved",mode=TextMode.MANUAL))}
                val page=doc.loadPage(0) as PDFPage
                try{val foreign=page.createAnnotation(PDFAnnotation.TYPE_TEXT)
                    try{foreign.setRect(Rect(400f,400f,420f,420f));foreign.setContents("Unmanaged fixture");foreign.update()}
                    finally{foreign.destroy()};page.update()
                }finally{page.destroy()}
                doc.save(source.absolutePath,"compress")
            }finally{doc.destroy()}
            val sourceHash=CheckpointFingerprint.of(source)
            val work=LocalHighlightWorkspace(File(root,"edits"),"content://synthetic/m3q",source,sourceHash)
            work.fileForSave();val before=list(work.currentFile())
            val changes=before.map{h->h to h.copy(edit=h.edit.copy(appearance=NativeHighlightAppearance.forQuads(h.edit.quads)))}
            check(changes.map{it.second.edit.appearance.size}.sorted()==listOf(1,10))
            work.replaceMany(changes,UUID.randomUUID().toString());check(work.sequence==1L && work.canUndo)
            val after=list(work.currentFile());check(after.size==2)
            for(h in after){val old=before.single{it.edit.id==h.edit.id}
                check(h.edit.quads==old.edit.quads && h.contents==old.contents && h.mode==old.mode)
                check(h.edit.appearance.size==if(h.edit.slot==0)1 else 10)
            }
            val afterHash=work.contentHash
            val rendered=Document.openDocument(work.currentFile().absolutePath)
            try{
                val page=rendered.loadPage(0) as PDFPage
                try{
                    val annotations=page.annotations ?: emptyArray()
                    try{check(annotations.count{it.type==PDFAnnotation.TYPE_TEXT && it.contents=="Unmanaged fixture"}==1)}finally{annotations.forEach{it.destroy()}}
                    val bitmap=AndroidDrawDevice.drawPageFit(page,1600,1600)
                    var count=0
                    try{
                        for(qs in cases)for((a,b)in qs.zipWithNext()){
                            val aa=a.bounds;val bb=b.bounds;val gap=bb.left-aa.right;val h=kotlin.math.min(aa.height,bb.height)
                            if(gap<.25f || gap>2.5f*h || kotlin.math.abs(aa.top-bb.top)>h*.2f)continue
                            val x=(((aa.right+bb.left)/2f)*bitmap.width/595f).toInt()
                            val y=(((aa.top+aa.bottom+bb.top+bb.bottom)/4f)*bitmap.height/842f).toInt()
                            val pixel=bitmap.getPixel(x,y)
                            check(Color.red(pixel)-Color.blue(pixel)>50){"Internal gap pixel not filled at$x,$y"};count++
                        }
                        check(count>10){"Insufficient measured real-geometry gaps"}
                    }finally{bitmap.recycle()}
                }finally{page.destroy()}
            }finally{rendered.destroy()}
            work.undo();check(work.contentHash==sourceHash && work.canRedo)
            work.redo();check(work.contentHash==afterHash)
            check(CheckpointFingerprint.of(source)==sourceHash)
        }catch(t:Throwable){failure=t}
        finally{root.deleteRecursively()}
        finish(if(failure==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(failure==null)"PASS: numeric PDF geometry, page batch, pixels, undo redo and preservation" else "FAIL: "+checkNotNull(failure).stackTraceToString())
        })
    }
}
