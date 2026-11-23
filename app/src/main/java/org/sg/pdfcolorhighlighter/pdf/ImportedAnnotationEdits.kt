// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*

/** Native owner worker only. No rewrite or ownership adoption on open or edit. */
object ImportedAnnotationEdits{
    private val privateKeys=listOf("SGCHNote","SGCHSlot","SGCHTextMode","SGCHGlyphQuads","SGCHAppearanceQuads","SGCHQuotation","SGCHSyncedContents")
    private fun string(a:PDFAnnotation,key:String):String?{
        val obj=a.`object`;try{val v=obj.get(key);try{return if(v.isString)v.asString()else null}finally{if(!v.isNull)v.destroy()}}finally{obj.destroy()}
    }
    private fun present(a:PDFAnnotation,key:String):Boolean{
        val obj=a.`object`;try{val v=obj.get(key);try{return !v.isNull}finally{if(!v.isNull)v.destroy()}}finally{obj.destroy()}
    }
    // A direct backlink to this exact highlight is not a reply or shared popup.
    private fun ownPopup(a:PDFAnnotation):Boolean{
        val owner=a.`object`
        try{
            val popup=owner.get("Popup")
            try{
                if(!owner.isIndirect || !popup.isIndirect)return false
                val subtype=popup.get("Subtype")
                try{if(!subtype.isName || subtype.asName()!="Popup")return false}
                finally{if(!subtype.isNull)subtype.destroy()}
                for(key in listOf("IRT","RT","RC","Popup","A","AA")){
                    val value=popup.get(key)
                    if(!value.isNull){value.destroy();return false}
                }
                val parent=popup.get("Parent")
                try{return parent.isIndirect && parent.asIndirect()==owner.asIndirect()}
                finally{if(!parent.isNull)parent.destroy()}
            }finally{if(!popup.isNull)popup.destroy()}
        }finally{owner.destroy()}
    }
    private fun eligible(a:PDFAnnotation):Boolean{
        if(privateKeys.any{present(a,it)} || listOf("IRT","RT","RC","Parent").any{present(a,it)})return false
        if((a.flags and ((1 shl 0) or (1 shl 1) or (1 shl 5) or (1 shl 6) or (1 shl 7) or (1 shl 9)))!=0)return false
        if(present(a,"Popup") && !ownPopup(a))return false
        val n=string(a,"NM") ?: return false
        if(n.isBlank() || n.length>1024 || '\u0000' in n)return false
        val rgb=a.color
        return rgb.size==3 && rgb.all{it.isFinite() && it in 0f..1f} && a.opacity.isFinite() && a.opacity in 0f..1f
    }
    private fun color(a:PDFAnnotation):Int{val rgb=a.color;return (kotlin.math.round(rgb[0]*255).toInt() shl 16) or (kotlin.math.round(rgb[1]*255).toInt() shl 8) or kotlin.math.round(rgb[2]*255).toInt()}
    private fun quads(a:PDFAnnotation):List<GlyphQuad>{
        require(a.quadPointCount in 1..25000)
        return a.quadPoints.map{q->GlyphQuad(TextPoint(q.ul_x,q.ul_y),TextPoint(q.ur_x,q.ur_y),TextPoint(q.ll_x,q.ll_y),TextPoint(q.lr_x,q.lr_y))}
    }
    private fun reference(a:PDFAnnotation,paint:List<GlyphQuad>):ImportedAnnotationRef{
        val r=a.bounds
        val raw=buildString{
            append(a.type).append('|').append(a.flags).append('|').append(a.opacity.toRawBits())
            a.color.forEach{append('|').append(it.toRawBits())}
            listOf(r.x0,r.y0,r.x1,r.y1).forEach{append('|').append(it.toRawBits())}
            paint.forEach{q->listOf(q.ul,q.ur,q.ll,q.lr).forEach{append('|').append(it.x.toRawBits()).append(':').append(it.y.toRawBits())}}
        }
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.US_ASCII)).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
        return ImportedAnnotationRef(checkNotNull(string(a,"NM")),digest)
    }
    private fun highlight(a:PDFAnnotation,page:Int):ManagedHighlight?{
        if(a.type!=PDFAnnotation.TYPE_HIGHLIGHT || !eligible(a))return null
        val contents=a.contents ?: "";if(contents.length>100000 || '\u0000' in contents)return null
        val paint=quads(a);val ref=reference(a,paint)
        return ManagedHighlight(HighlightEdit(ref.id(page,"Highlight"),page,0,contents,paint,paint,
            AnnotationPalette.storedColor(color(a),0),true),contents,TextMode.MANUAL,a.opacity,ref)
    }
    private fun note(a:PDFAnnotation,page:Int):ManagedNote?{
        if(a.type!=PDFAnnotation.TYPE_TEXT || !eligible(a))return null
        val contents=a.contents ?: "";if(contents.length>100000 || '\u0000' in contents)return null
        val r=a.bounds;val ref=reference(a,emptyList())
        return ManagedNote(ref.id(page,"Text"),page,0,TextBounds(r.x0,r.y0,r.x1,r.y1),contents,
            AnnotationPalette.storedColor(color(a),0),ref)
    }
    private fun names(all:Array<PDFAnnotation>)=all.map{runCatching{string(it,"NM")}.getOrNull()}.filterNotNull().groupingBy{it}.eachCount()
    fun highlights(pdf:PDFDocument,index:Int):List<ManagedHighlight>{
        if(!pdf.hasPermission(Document.PERMISSION_COPY))return emptyList()
        val p=pdf.loadPage(index) as PDFPage;try{val all=p.annotations ?: emptyArray();try{
            if(all.size>4096)return emptyList();val counts=names(all)
            val found=all.mapNotNull{a->runCatching{if(counts[string(a,"NM") ?: ""]!=1)null else highlight(a,index)}.getOrNull()}
            return found.takeIf{it.sumOf{x->x.edit.quads.size.toLong()}<=100000L}.orEmpty()
        }finally{all.forEach{it.destroy()}}}finally{p.destroy()}
    }
    fun notes(pdf:PDFDocument,index:Int):List<ManagedNote>{
        val p=pdf.loadPage(index) as PDFPage;try{val all=p.annotations ?: emptyArray();try{
            if(all.size>4096)return emptyList();val counts=names(all)
            val found=all.mapNotNull{a->runCatching{if(counts[string(a,"NM") ?: ""]!=1)null else note(a,index)}.getOrNull()}
            return found.takeIf{it.sumOf{x->x.contents.length.toLong()}<=4000000L}.orEmpty()
        }finally{all.forEach{it.destroy()}}}finally{p.destroy()}
    }
    private fun rgb(a:PDFAnnotation,value:Int){a.setColor(floatArrayOf(((value shr 16) and 255)/255f,((value shr 8) and 255)/255f,(value and 255)/255f))}
    fun replaceHighlight(pdf:PDFDocument,before:ManagedHighlight,after:ManagedHighlight?){
        require(pdf.hasPermission(Document.PERMISSION_COPY) && pdf.hasPermission(Document.PERMISSION_ANNOTATE))
        val ref=checkNotNull(before.imported)
        require(after==null || after.imported==ref && after.edit.id==before.edit.id && after.edit.page==before.edit.page && after.mode==TextMode.MANUAL)
        val p=pdf.loadPage(before.edit.page) as PDFPage;try{val all=p.annotations ?: emptyArray();try{
            val found=all.filter{string(it,"NM")==ref.name};require(found.size==1){"Imported highlight identity not unique"}
            val a=found.single();require(highlight(a,before.edit.page)==before){"Stale or unsupported imported highlight"}
            // MuPDF removes both the annotation and its verified popup from the page.
            if(after==null)p.deleteAnnotation(a)else{
                if(after.edit.quads!=before.edit.quads || after.edit.appearance!=before.edit.appearance){
                    a.setQuadPoints(after.edit.appearance.ifEmpty{after.edit.quads}.map{q->Quad(q.ul.x,q.ul.y,q.ur.x,q.ur.y,q.ll.x,q.ll.y,q.lr.x,q.lr.y)}.toTypedArray())
                }
                if(after.edit.color!=before.edit.color)rgb(a,after.edit.color)
                if(after.opacity!=before.opacity)a.setOpacity(after.opacity)
                if(after.contents!=before.contents)a.setContents(after.contents)
                a.update()
            };p.update()
        }finally{all.forEach{it.destroy()}}}finally{p.destroy()}
    }
    fun replaceNote(pdf:PDFDocument,before:ManagedNote,after:ManagedNote?){
        require(pdf.hasPermission(Document.PERMISSION_ANNOTATE));val ref=checkNotNull(before.imported)
        require(after==null || after.imported==ref && after.id==before.id && after.page==before.page && after.bounds==before.bounds)
        val p=pdf.loadPage(before.page) as PDFPage;try{val all=p.annotations ?: emptyArray();try{
            val found=all.filter{string(it,"NM")==ref.name};require(found.size==1){"Imported note identity not unique"}
            val a=found.single();require(note(a,before.page)==before){"Stale or unsupported imported note"}
            if(after==null)p.deleteAnnotation(a)else{
                if(after.color!=before.color)rgb(a,after.color)
                if(after.contents!=before.contents)a.setContents(after.contents)
                a.update()
            };p.update()
        }finally{all.forEach{it.destroy()}}}finally{p.destroy()}
    }
}
