// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter
import android.app.*
import android.os.Bundle
import java.io.File
import java.util.UUID
import org.sg.pdfcolorhighlighter.pdf.*
import org.sg.pdfcolorhighlighter.domain.CheckpointFingerprint
import org.sg.pdfcolorhighlighter.data.FallbackFolderStore
import com.artifex.mupdf.fitz.Document

/** Synthetic file/AtomicFile/native round-trip, not a real SAF grant proof. */
class AuthorizedFallbackInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"fallback-native-${UUID.randomUUID()}");var failure:Throwable?=null
        try{
            check(root.mkdirs());val src=File(root,"source.pdf");src.writeBytes(ColumnContinuationFixture.bytes())
            val editable=Document.openDocument(src.absolutePath) as com.artifex.mupdf.fitz.PDFDocument
            try{
                val page=editable.loadPage(0) as com.artifex.mupdf.fitz.PDFPage
                val snap=try{MuPdfTextSnapshot.extract(page,true)}finally{page.destroy()}
                val row=snap.lines.first{it.text.startsWith("LEFT A")}
                fun center(g:org.sg.pdfcolorhighlighter.domain.TextGlyph):org.sg.pdfcolorhighlighter.domain.TextPoint {val b=g.quad.bounds;return org.sg.pdfcolorhighlighter.domain.TextPoint((b.left+b.right)/2,(b.top+b.bottom)/2)}
                val selected=org.sg.pdfcolorhighlighter.domain.PageTextSelector.select(snap,center(row.glyphs.first()),center(row.glyphs.last()),false) as org.sg.pdfcolorhighlighter.domain.TextSelectionAttempt.Selected
                NativeHighlightEdits.add(editable,org.sg.pdfcolorhighlighter.domain.HighlightEdit.from(0,1,selected.span))
                val p=editable.loadPage(0) as com.artifex.mupdf.fitz.PDFPage
                try{val all=p.annotations ?: emptyArray();try{all.single().setOpacity(.35f);all.single().update();p.update()}finally{all.forEach{it.destroy()}}}finally{p.destroy()}
                val before=NativeHighlightEdits.list(editable,0).single()
                NativeHighlightEdits.replace(editable,before,before.copy(contents="Synthetic correction",mode=org.sg.pdfcolorhighlighter.domain.TextMode.MANUAL))
                check(kotlin.math.abs(NativeHighlightEdits.list(editable,0).single().opacity-.35f)<.00001f)
                editable.save(File(root,"styled.pdf").absolutePath,"compress")
            }finally{editable.destroy()}
            val styled=Document.openDocument(File(root,"styled.pdf").absolutePath) as com.artifex.mupdf.fitz.PDFDocument
            try{check(kotlin.math.abs(NativeHighlightEdits.list(styled,0).single().opacity-.35f)<.00001f)}finally{styled.destroy()}
            val hash=CheckpointFingerprint.of(src);val pref=FallbackFolderStore(File(root,"config.bin"))
            check(pref.read()==null);pref.write("content://synthetic/tree/folder");check(pref.read()=="content://synthetic/tree/folder")
            val out=File(root,"copy.pdf")
            val gateway=object:FallbackTarget{
                override fun authorized()=true
                override fun createNew(baseName:String,original:String):String{check(out.createNewFile());return out.name}
                override fun read(id:String)=out.inputStream()
                override fun write(id:String)=out.outputStream()
            }
            val receipt=AutomaticFallback.copy(src,"content://synthetic/source","source.pdf","2026-11-15_17-34-00",gateway)
            check(receipt.sha256==hash && CheckpointFingerprint.of(out)==hash && CheckpointFingerprint.of(src)==hash)
            val doc=Document.openDocument(out.absolutePath);try{check(doc.countPages()==1)}finally{doc.destroy()}
            pref.write(null);check(pref.read()==null)
        }catch(t:Throwable){failure=t}finally{root.deleteRecursively()}
        finish(if(failure==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(failure==null)"PASS: fallback copy integrity native reopen and preferences"else"FAIL: fallback")
            failure?.let{putString("details",it.stackTraceToString())}
        })
    }
}
