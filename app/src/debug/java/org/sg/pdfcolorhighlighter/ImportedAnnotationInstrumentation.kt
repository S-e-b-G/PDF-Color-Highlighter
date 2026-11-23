// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.File
import java.util.UUID

class ImportedAnnotationInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private inline fun <T> pdf(file:File,body:(PDFDocument)->T):T{
        val doc=Document.openDocument(file.absolutePath);try{return body(doc as PDFDocument)}finally{doc.destroy()}
    }
    private fun highlights(file:File)=pdf(file){NativeHighlightEdits.editable(it,0)}
    private fun notes(file:File)=pdf(file){NativeNoteEdits.editable(it,0)}
    private fun op()=UUID.randomUUID().toString()
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"import-proof-${op()}");var failure:Throwable?=null
        try{
            check(root.mkdirs());val input=File(root,"input.pdf");input.writeBytes(ImportedAnnotationFixture.bytes())
            val original=CheckpointFingerprint.of(input)
            check(pdf(input){NativeHighlightEdits.list(it,0).isEmpty() && NativeNoteEdits.list(it,0).isEmpty()})
            val h=highlights(input).single();val n=notes(input).single()
            check(h.imported!=null && n.imported!=null && h.mode==TextMode.MANUAL)
            check(h.contents=="External quotation – élève" && n.contents=="External note – cœur")
            val w=LocalHighlightWorkspace(File(root,"edits"),"content://synthetic/imported",input,original)
            w.replace(h,h.copy(contents="Manual corrected é\nkept",mode=TextMode.MANUAL),op())
            var current=highlights(w.currentFile()).single()
            check(current.edit.id==h.edit.id && current.contents=="Manual corrected é\nkept" && current.opacity==h.opacity)
            check(current.edit.quads==h.edit.quads && current.edit.color==h.edit.color)
            var rejected=false
            try{w.replace(h,null,op())}catch(_:IllegalArgumentException){rejected=true}
            check(rejected && highlights(w.currentFile()).size==1)
            val textHash=w.contentHash
            w.replace(current,current.copy(edit=current.edit.copy(slot=2,rgb=0xFC3636)),op())
            current=highlights(w.currentFile()).single();check(current.edit.color==0xFC3636 && current.opacity==h.opacity)
            w.undo();check(w.contentHash==textHash);w.redo()
            current=highlights(w.currentFile()).single()
            val small=GlyphQuad(TextPoint(50f,80f),TextPoint(200f,80f),TextPoint(50f,96f),TextPoint(200f,96f))
            w.replace(current,current.copy(edit=current.edit.copy(quads=listOf(small),appearance=listOf(small))),op())
            current=highlights(w.currentFile()).single();check(current.contents=="Manual corrected é\nkept" && current.opacity==h.opacity)
            val currentNote=notes(w.currentFile()).single()
            w.replaceNote(currentNote,currentNote.copy(contents="Corrected native note é",slot=1,rgb=0x00ff00),op())
            val note=notes(w.currentFile()).single();check(note.id==n.id && note.contents=="Corrected native note é" && note.color==0x00ff00)
            val both=w.contentHash;w.replaceNote(note,null,op());check(notes(w.currentFile()).isEmpty());w.undo();check(w.contentHash==both);w.redo();check(notes(w.currentFile()).isEmpty());w.undo()
            pdf(w.currentFile()){doc->val page=doc.loadPage(0) as PDFPage;try{val all=page.annotations ?: emptyArray();try{
                check(all.size==2)
                for(a in all){val obj=a.`object`;try{
                    fun text(key:String):String{val v=obj.get(key);try{return if(v.isString)v.asString()else ""}finally{if(!v.isNull)v.destroy()}}
                    check(text("NM") in setOf("external-highlight","external-note"));check(text("T")=="External author" && text("SGFixtureKeep")=="preserve")
                    val tag=obj.get("SGCHTextMode");try{check(tag.isNull)}finally{if(!tag.isNull)tag.destroy()}
                    if(a.type==PDFAnnotation.TYPE_TEXT)check(kotlin.math.abs(a.opacity-.43f)<.0001f)
                }finally{obj.destroy()}}
            }finally{all.forEach{it.destroy()}}}finally{page.destroy()}}
            val export=File(root,"export.pdf");VerifiedDocumentExport.copy(w.fileForSave(),{export.outputStream()},{export.inputStream()})
            check(highlights(export).single().contents==current.contents && notes(export).single().contents==note.contents)
            val recovered=LocalHighlightWorkspace(File(root,"edits"),"content://synthetic/imported",input,original)
            check(recovered.recoverExisting()!=null && highlights(recovered.currentFile()).single().edit.id==h.edit.id)
            for((name,bytes)in listOf("locked" to ImportedAnnotationFixture.bytes(locked=true),"duplicate" to ImportedAnnotationFixture.bytes(duplicate=true),"unnamed" to ImportedAnnotationFixture.bytes(unnamed=true),"rich" to ImportedAnnotationFixture.bytes(rich=true))){
                val sample=File(root,"$name.pdf");sample.writeBytes(bytes);check(highlights(sample).isEmpty())
                if(name=="duplicate" || name=="unnamed")check(notes(sample).isEmpty())
                check(sample.readBytes().contentEquals(bytes))
            }
            current=highlights(w.currentFile()).single();w.replace(current,null,op());check(highlights(w.currentFile()).isEmpty());w.undo();check(highlights(w.currentFile()).size==1)
            val withPopup=File(root,"popup.pdf").apply{writeBytes(ImportedAnnotationFixture.bytes(notePopup=true))}
            val badPopup=File(root,"popup-invalid.pdf").apply{writeBytes(ImportedAnnotationFixture.bytes(notePopup=true,wrongPopupParent=true))}
            check(notes(badPopup).isEmpty())
            val popupHash=CheckpointFingerprint.of(withPopup)
            val popupNote=notes(withPopup).single()
            val pw=LocalHighlightWorkspace(File(root,"popup-edits"),"content://synthetic/popup-note",withPopup,popupHash)
            pw.replaceNote(popupNote,popupNote.copy(slot=1,rgb=0x00ff00),op())
            val changed=notes(pw.currentFile()).single()
            check(changed.id==popupNote.id && changed.contents==popupNote.contents && changed.color==0x00ff00)
            pw.replaceNote(changed,null,op())
            check(notes(pw.currentFile()).isEmpty() && highlights(pw.currentFile()).size==1)
            pw.undo();check(notes(pw.currentFile()).single()==changed)
            pw.redo();check(notes(pw.currentFile()).isEmpty())
            check(CheckpointFingerprint.of(withPopup)==popupHash)
            check(CheckpointFingerprint.of(input)==original)
        }catch(t:Throwable){failure=t}finally{root.deleteRecursively()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: compatible imported annotations identity history preservation"else "FAIL: imported annotations")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
}
