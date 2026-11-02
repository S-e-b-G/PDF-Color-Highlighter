// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.pdf.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Isolated real native add/undo/redo/reopen workflow, not a Compose touch or external-save test. */
class HighlightInteractionInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun count(file:File):Int {
        val doc=Document.openDocument(file.absolutePath)
        try{val page=doc.loadPage(0) as PDFPage
            try{val all=page.annotations ?: emptyArray();try{return all.count{it.type==PDFAnnotation.TYPE_HIGHLIGHT}}finally{all.forEach{it.destroy()}}}
            finally{page.destroy()}
        }finally{doc.destroy()}
    }
    private fun edit(source:File,text:String,slot:Int):HighlightEdit {
        val doc=Document.openDocument(source.absolutePath)
        try{val page=doc.loadPage(0)
            try{
                // Use the actual production snapshot/selector path, NOT search-result regions.
                val snapshot=MuPdfTextSnapshot.extract(page,true)
                val rows=snapshot.lines.withIndex().filter{it.value.text==text}
                check(rows.size==1){"Synthetic fixture text must identify exactly one physical row"}
                val line=rows.single().value
                fun center(glyph:TextGlyph):TextPoint {
                    val q=glyph.quad
                    // A convex quad's vertex average is interior, including tilted text.
                    return TextPoint((q.ul.x+q.ur.x+q.ll.x+q.lr.x)/4f,
                        (q.ul.y+q.ur.y+q.ll.y+q.lr.y)/4f)
                }
                val first=line.glyphs.first{!it.whitespace}
                val last=line.glyphs.last{!it.whitespace}
                val selected=PageTextSelector.select(snapshot,center(first),center(last),false)
                check(selected is TextSelectionAttempt.Selected){"Production selector refused synthetic fixture"}
                val span=selected.span
                check(span.quotation==text){"Synthetic fixture selected a different quotation"}
                val edit=HighlightEdit.from(0,slot,span)
                check(edit.quads.size>1){"Fixture unexpectedly produced aggregated geometry"}
                val owned=ManagedHighlight(edit,span.quotation,TextMode.AUTOMATIC)
                val restored=ManagedHighlightSelection.span(snapshot,owned)
                check(restored!=null && restored.start==span.start && restored.end==span.end){
                    "Production-generated fixture did not restore its exact endpoints before native persistence"
                }
                return edit
            }finally{page.destroy()}
        }finally{doc.destroy()}
    }
    override fun onStart(){
        val root=File(targetContext.noBackupFilesDir,"highlight-interaction-proof-${UUID.randomUUID()}")
        var failure:Throwable?=null
        try{
            check(root.mkdirs());val source=File(root,"input.pdf");val bytes=fixture();source.writeBytes(bytes)
            val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
            val editRoot=File(root,"edits")
            val workspace=LocalHighlightWorkspace(editRoot,"content://synthetic/highlights",source,hash)
            check(workspace.recoverExisting()==null)

            val a=edit(source,"Local highlight one.",0)
            workspace.add(a);workspace.maintain()
            fun state():ManagedHighlight {
                val pdf=Document.openDocument(workspace.currentFile().absolutePath) as PDFDocument
                try{return NativeHighlightEdits.list(pdf,0).single()}finally{pdf.destroy()}
            }
            fun fileHash(file:File)=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).toList()
            val initial=state();check(initial.edit.slot==0 && initial.mode==TextMode.AUTOMATIC)
            val snapshotDoc=Document.openDocument(workspace.currentFile().absolutePath)
            try{val page=snapshotDoc.loadPage(0)
                try{val snapshot=MuPdfTextSnapshot.extract(page,true)
                    check(ManagedHighlightSelection.span(snapshot,initial)!=null){"Native quads did not restore handles"}
                }finally{page.destroy()}
            }finally{snapshotDoc.destroy()}
            val firstHash=fileHash(workspace.currentFile())
            workspace.replace(initial,initial.copy(edit=initial.edit.copy(slot=3)),UUID.randomUUID().toString());workspace.maintain()
            check(state().edit.slot==3 && count(workspace.currentFile())==1)
            val stableSequence=workspace.sequence
            var staleRejected=false
            try{workspace.replace(initial,null,UUID.randomUUID().toString())}catch(_:IllegalArgumentException){staleRejected=true}
            check(staleRejected && workspace.sequence==stableSequence && count(workspace.currentFile())==1)

            workspace.undo();workspace.maintain();check(fileHash(workspace.currentFile())==firstHash)
            workspace.redo();workspace.maintain();check(state().edit.slot==3)
            val colored=state()
            val resized=edit(source,"Local highlight two.",3)
            workspace.replace(colored,colored.copy(edit=colored.edit.copy(quotation=resized.quotation,quads=resized.quads),contents=resized.quotation),UUID.randomUUID().toString())
            workspace.maintain();check(state().contents=="Local highlight two.")
            // Simulate an explicit manual Contents correction, then adjust geometry.
            val manual=state().copy(contents="e\u0301\nmanual correction",mode=TextMode.MANUAL)
            workspace.replace(state(),manual,UUID.randomUUID().toString());workspace.maintain()
            val corrected=state();check(corrected.mode==TextMode.MANUAL)
            workspace.replace(corrected,corrected.copy(edit=corrected.edit.copy(quotation=a.quotation,quads=a.quads)),UUID.randomUUID().toString())
            workspace.maintain();check(state().contents==manual.contents)
            val beforeDelete=fileHash(workspace.currentFile())
            workspace.replace(state(),null,UUID.randomUUID().toString());workspace.maintain()
            check(count(workspace.currentFile())==0)
            workspace.undo();workspace.maintain()
            check(fileHash(workspace.currentFile())==beforeDelete && state().contents==manual.contents)
            workspace.redo();workspace.maintain();check(count(workspace.currentFile())==0)
            workspace.undo();workspace.maintain()
            val reopened=LocalHighlightWorkspace(editRoot,"content://synthetic/highlights",source,hash)
            check(reopened.recoverExisting()!=null && !reopened.canUndo && !reopened.canRedo)
            val pdf=Document.openDocument(reopened.currentFile().absolutePath) as PDFDocument
            try{check(NativeHighlightEdits.list(pdf,0).single().contents==manual.contents)}finally{pdf.destroy()}
            // A new edit after undo invalidates redo and must not let maintenance delete undo pins.
            workspace.add(edit(source,"Local highlight two.",1));workspace.maintain();check(!workspace.canRedo && count(workspace.currentFile())==2)
            workspace.undo();workspace.maintain();check(count(workspace.currentFile())==1)

            // Simulate a Contents correction made by another reader without app metadata updates.
            val externalFile=File(root,"external-correction.pdf")
            val external=Document.openDocument(workspace.currentFile().absolutePath) as PDFDocument
            try{val page=external.loadPage(0) as PDFPage
                try{val all=page.annotations ?: emptyArray()
                    try{val annotation=all.single();annotation.setContents("external corrected Contents")
                        val obj=annotation.`object`
                        try{obj.put("SGFixtureForeign","preserve me")}finally{obj.destroy()}
                        annotation.update();page.update()
                    }finally{all.forEach{it.destroy()}}
                }finally{page.destroy()}
                external.save(externalFile.absolutePath,"compress")
            }finally{external.destroy()}
            val externalHash=MessageDigest.getInstance("SHA-256").digest(externalFile.readBytes()).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
            val foreignWorkspace=LocalHighlightWorkspace(File(root,"external-edits"),"content://synthetic/external",externalFile,externalHash)
            val foreignDoc=Document.openDocument(externalFile.absolutePath) as PDFDocument
            val foreign=try{NativeHighlightEdits.list(foreignDoc,0).single()}finally{foreignDoc.destroy()}
            check(foreign.mode==TextMode.MANUAL)
            foreignWorkspace.replace(foreign,foreign.copy(edit=foreign.edit.copy(slot=5,quotation=resized.quotation,quads=resized.quads)),UUID.randomUUID().toString())
            foreignWorkspace.maintain()
            val verify=Document.openDocument(foreignWorkspace.currentFile().absolutePath) as PDFDocument
            try{
                check(NativeHighlightEdits.list(verify,0).single().contents=="external corrected Contents")
                val page=verify.loadPage(0) as PDFPage
                try{val all=page.annotations ?: emptyArray()
                    try{val obj=all.single().`object`
                        try{val value=obj.get("SGFixtureForeign")
                            try{check(value.isString && value.asString()=="preserve me")}finally{if(!value.isNull)value.destroy()}
                        }finally{obj.destroy()}
                    }finally{all.forEach{it.destroy()}}
                }finally{page.destroy()}
            }finally{verify.destroy()}
            check(source.readBytes().contentEquals(bytes)){"Original synthetic source changed"}
        }catch(error:Throwable){failure=error}
        finally{root.deleteRecursively()}
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: highlight interaction"else"FAIL: highlight interaction")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
    private fun fixture():ByteArray {
        val content="BT /F1 12 Tf 50 750 Td (Local highlight one.) Tj 0 -24 Td (Local highlight two.) Tj ET\n"
        val objects=listOf("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Length ${content.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${content}endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        val out=ByteArrayOutputStream();val offsets=mutableListOf<Int>()
        fun append(s:String){out.write(s.toByteArray(Charsets.US_ASCII))}
        append("%PDF-1.4\n")
        objects.forEachIndexed{i,s->offsets.add(out.size());append("${i+1} 0 obj\n$s\nendobj\n")}
        val xref=out.size();append("xref\n0 ${objects.size+1}\n0000000000 65535 f \n")
        offsets.forEach{append(it.toString().padStart(10,'0')+" 00000 n \n")}
        append("trailer\n<< /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }
}
