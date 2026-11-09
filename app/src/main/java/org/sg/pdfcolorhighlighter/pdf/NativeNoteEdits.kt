// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*
import kotlin.math.abs

/** Candidate/owner worker only. Imported, duplicate, locked or unrecognized notes are not edited. */
object NativeNoteEdits {
    private fun string(a:PDFAnnotation,key:String):String? {
        val obj=a.`object`
        try{val v=obj.get(key)
            try{return if(v.isString)v.asString()else null}finally{if(!v.isNull)v.destroy()}
        }finally{obj.destroy()}
    }
    private fun id(a:PDFAnnotation)=string(a,"NM") ?: ""
    private fun read(a:PDFAnnotation,page:Int):ManagedNote? {
        if(a.type!=PDFAnnotation.TYPE_TEXT || string(a,"SGCHNote")!="1")return null
        if((a.flags and ((1 shl 0) or (1 shl 1) or (1 shl 5) or (1 shl 6) or (1 shl 7) or (1 shl 9)))!=0)return null
        val rgb=a.color
        if(rgb.size!=3)return null
        val slot=(0..5).singleOrNull{s->val c=NotePolicy.color(s)
            abs(rgb[0]-((c shr 16) and 255)/255f)<.002f &&
            abs(rgb[1]-((c shr 8) and 255)/255f)<.002f && abs(rgb[2]-(c and 255)/255f)<.002f
        } ?: return null
        val r=a.bounds
        return ManagedNote(id(a),page,slot,TextBounds(r.x0,r.y0,r.x1,r.y1),a.contents ?: "")
    }
    fun list(pdf:PDFDocument,index:Int):List<ManagedNote> {
        val page=pdf.loadPage(index) as PDFPage
        try{val all=page.annotations ?: emptyArray()
            try{
                if(all.size>4096)return emptyList()
                val duplicates=all.map{runCatching{id(it)}.getOrDefault("")}.groupingBy{it}.eachCount().filterValues{it>1}.keys
                val result=mutableListOf<ManagedNote>();var budget=0L
                for(a in all){
                    val n=runCatching{read(a,index)}.getOrNull() ?: continue
                    if(n.id in duplicates)continue
                    budget+=n.contents.length
                    if(budget>4_000_000L)return emptyList()
                    result.add(n)
                }
                return result
            }finally{all.forEach{it.destroy()}}
        }finally{page.destroy()}
    }
    private fun style(a:PDFAnnotation,n:ManagedNote) {
        val c=NotePolicy.color(n.slot)
        a.setColor(floatArrayOf(((c shr 16) and 255)/255f,((c shr 8) and 255)/255f,(c and 255)/255f))
        a.setContents(n.contents);a.update()
    }
    fun add(pdf:PDFDocument,n:ManagedNote) {
        require(pdf.hasPermission(Document.PERMISSION_ANNOTATE))
        val page=pdf.loadPage(n.page) as PDFPage
        try{
            val r=page.bounds
            require(n.bounds.left>=r.x0 && n.bounds.top>=r.y0 && n.bounds.right<=r.x1 && n.bounds.bottom<=r.y1)
            val all=page.annotations ?: emptyArray()
            try{require(all.none{id(it)==n.id}){"Note identity already exists"}}finally{all.forEach{it.destroy()}}
            val a=page.createAnnotation(PDFAnnotation.TYPE_TEXT)
            try{
                a.setRect(Rect(n.bounds.left,n.bounds.top,n.bounds.right,n.bounds.bottom))
                a.setIcon("Note");a.setFlags(a.flags or PDFAnnotation.IS_PRINT)
                val obj=a.`object`
                try{obj.put("NM",n.id);obj.put("SGCHNote","1")}finally{obj.destroy()}
                style(a,n)
            }finally{a.destroy()}
            page.update()
        }finally{page.destroy()}
    }
    fun replace(pdf:PDFDocument,before:ManagedNote,after:ManagedNote?) {
        require(pdf.hasPermission(Document.PERMISSION_ANNOTATE))
        require(after==null || after.id==before.id && after.page==before.page && after.bounds==before.bounds)
        val page=pdf.loadPage(before.page) as PDFPage
        try{val all=page.annotations ?: emptyArray()
            try{
                val matches=all.filter{id(it)==before.id};require(matches.size==1){"Note identity not unique"}
                val a=matches.single();require(read(a,before.page)==before){"Stale note state"}
                if(after==null)page.deleteAnnotation(a)else style(a,after)
                page.update()
            }finally{all.forEach{it.destroy()}}
        }finally{page.destroy()}
    }
}
