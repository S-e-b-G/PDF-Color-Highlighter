// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*

/** Candidate/owner worker only. Child native objects are always released before return. */
object NativeHighlightEdits {
    private val colors=intArrayOf(0xFFAD5B,0x00FF00,0xFC3636,0xFFFF00,0xD1DFEB,0xFCD7FB)
    private fun id(annotation:PDFAnnotation):String {
        val obj=annotation.`object`
        try {
            val value=obj.get("NM")
            try{return if(value.isString)value.asString()else ""}finally{if(!value.isNull)value.destroy()}
        }finally{if(!obj.isNull)obj.destroy()}
    }

    private fun string(annotation:PDFAnnotation,key:String):String? {
        val obj=annotation.`object`
        try{val value=obj.get(key)
            try{return if(value.isString) {
                if(key=="SGCHQuotation" || key=="SGCHSyncedContents")PdfTextString.decodeAppText(value.asByteString())else value.asString()
            }else null}finally{if(!value.isNull)value.destroy()}
        }finally{obj.destroy()}
    }
    private fun managed(annotation:PDFAnnotation,page:Int):ManagedHighlight? {
        if(annotation.type!=PDFAnnotation.TYPE_HIGHLIGHT)return null
        val name=id(annotation)
        if(!Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(name))return null
        val mode=string(annotation,"SGCHTextMode") ?: return null
        if(mode!=TextMode.AUTOMATIC.name && mode!=TextMode.MANUAL.name)return null
        val quotation=string(annotation,"SGCHQuotation") ?: return null
        val synced=string(annotation,"SGCHSyncedContents") ?: return null
        // Do not allow destructive edits to hidden/locked/read-only annotations.
        if((annotation.flags and ((1 shl 0) or (1 shl 1) or (1 shl 5) or (1 shl 6) or (1 shl 7) or (1 shl 9)))!=0)return null
        val rgb=annotation.color
        if(rgb.size!=3)return null
        val slot=colors.indices.singleOrNull{s->
            val c=colors[s];kotlin.math.abs(rgb[0]-((c shr 16) and 255)/255f)<.002f &&
                kotlin.math.abs(rgb[1]-((c shr 8) and 255)/255f)<.002f && kotlin.math.abs(rgb[2]-(c and 255)/255f)<.002f
        } ?: return null
        if(annotation.quadPointCount !in 1..25000)return null
        val native=annotation.quadPoints
        val painted=native.map{q->GlyphQuad(TextPoint(q.ul_x,q.ul_y),TextPoint(q.ur_x,q.ur_y),TextPoint(q.ll_x,q.ll_y),TextPoint(q.lr_x,q.lr_y))}
        val encoded=string(annotation,"SGCHGlyphQuads")
        val quads=if(encoded==null)painted else HighlightGeometryCodec.decode(encoded)
        if(encoded!=null){
            val expected=HighlightGeometryCodec.decode(string(annotation,"SGCHAppearanceQuads") ?: return null)
            if(expected.size!=painted.size)return null
            fun points(q:GlyphQuad)=listOf(q.ul,q.ur,q.ll,q.lr)
            if(expected.indices.any{i->val a=points(expected[i]);val b=points(painted[i])
                a.indices.any{kotlin.math.abs(a[it].x-b[it].x)>.03f || kotlin.math.abs(a[it].y-b[it].y)>.03f}})return null
        }
        val contents=annotation.contents ?: ""
        val automatic=mode==TextMode.AUTOMATIC.name && contents==synced
        return ManagedHighlight(HighlightEdit(name,page,slot,quotation,quads,if(encoded==null)emptyList()else painted),contents,
            if(automatic)TextMode.AUTOMATIC else TextMode.MANUAL)
    }
    fun list(pdf:PDFDocument,index:Int):List<ManagedHighlight> {
        val page=pdf.loadPage(index) as PDFPage
        try{val annotations=page.annotations ?: emptyArray()
            try{
                // One bad/unrelated annotation must not invalidate the rendered page.
                if(annotations.size>4096)return emptyList()

                val copied=mutableListOf<ManagedHighlight>();var quads=0L
                for(annotation in annotations){
                    val item=runCatching{managed(annotation,index)}.getOrNull() ?: continue
                    quads+=item.edit.quads.size
                    if(quads>100000L)return emptyList()
                    copied.add(item)
                }
                val duplicates=annotations.map{runCatching{id(it)}.getOrDefault("")}.groupingBy{it}.eachCount().filterValues{it>1}.keys
                return copied.filter{it.edit.id !in duplicates}.takeIf{it.sumOf{x->x.edit.quads.size.toLong()}<=100000L}.orEmpty()
            }finally{annotations.forEach{it.destroy()}}
        }finally{page.destroy()}
    }
    /** Modify in place: preserve unrelated entries/flags; apply current app-owned highlight opacity. */
    fun replace(pdf:PDFDocument,before:ManagedHighlight,after:ManagedHighlight?) {
        require(pdf.hasPermission(Document.PERMISSION_COPY))
        require(after==null || after.edit.id==before.edit.id && after.edit.page==before.edit.page)
        val page=pdf.loadPage(before.edit.page) as PDFPage
        try{val annotations=page.annotations ?: emptyArray()
            try{
                val found=annotations.filter{id(it)==before.edit.id}
                require(found.size==1){"Highlight identity not unique"}
                val annotation=found.single()
                require(managed(annotation,before.edit.page)==before){"Stale highlight state"}
                if(after==null)page.deleteAnnotation(annotation)
                else{
                    annotation.setQuadPoints((after.edit.appearance.ifEmpty{NativeHighlightAppearance.forQuads(after.edit.quads)}).map{q->Quad(q.ul.x,q.ul.y,q.ur.x,q.ur.y,q.ll.x,q.ll.y,q.lr.x,q.lr.y)}.toTypedArray())
                    val c=colors[after.edit.slot]
                    annotation.setColor(floatArrayOf(((c shr 16) and 255)/255f,((c shr 8) and 255)/255f,(c and 255)/255f))
                    annotation.setOpacity(.65f)
                    annotation.setContents(after.contents)
                    val obj=annotation.`object`
                    try{
                        obj.put("SGCHTextMode",after.mode.name)
                        obj.put("SGCHGlyphQuads",HighlightGeometryCodec.encode(after.edit.quads))
                        obj.put("SGCHAppearanceQuads",HighlightGeometryCodec.encode(after.edit.appearance.ifEmpty{NativeHighlightAppearance.forQuads(after.edit.quads)}))
                        for((key,value)in listOf("SGCHQuotation" to after.edit.quotation,"SGCHSyncedContents" to after.contents)){
                            val encoded=pdf.newByteString(PdfTextString.encode(value))
                            try{obj.put(key,encoded)}finally{encoded.destroy()}
                        }
                    }finally{obj.destroy()}
                    annotation.update()
                }
                page.update()
            }finally{annotations.forEach{it.destroy()}}
        }finally{page.destroy()}
    }
    fun add(pdf:PDFDocument,edit:HighlightEdit){
        require(pdf.hasPermission(Document.PERMISSION_COPY)){"PDF copy permission is required for extracted quotations"}
        require(edit.page in 0 until pdf.countPages())
        val page=pdf.loadPage(edit.page) as PDFPage
        try {
            val existing=page.annotations ?: emptyArray()
            try{require(existing.none{id(it)==edit.id}){"Highlight identity already exists"}}finally{existing.forEach{it.destroy()}}
            val annotation=page.createAnnotation(PDFAnnotation.TYPE_HIGHLIGHT)
            try {
                annotation.setQuadPoints((edit.appearance.ifEmpty{NativeHighlightAppearance.forQuads(edit.quads)}).map{q->Quad(q.ul.x,q.ul.y,q.ur.x,q.ur.y,q.ll.x,q.ll.y,q.lr.x,q.lr.y)}.toTypedArray())
                val color=colors[edit.slot]
                annotation.setColor(floatArrayOf(((color shr 16) and 255)/255f,((color shr 8) and 255)/255f,(color and 255)/255f))
                annotation.setOpacity(.65f)
                annotation.setFlags(annotation.flags or PDFAnnotation.IS_PRINT)
                annotation.setContents(edit.quotation)
                val obj=annotation.`object`
                try {
                    obj.put("NM",edit.id);obj.put("SGCHTextMode",TextMode.AUTOMATIC.name)
                    obj.put("SGCHGlyphQuads",HighlightGeometryCodec.encode(edit.quads))
                    obj.put("SGCHAppearanceQuads",HighlightGeometryCodec.encode(edit.appearance.ifEmpty{NativeHighlightAppearance.forQuads(edit.quads)}))
                    for(key in listOf("SGCHQuotation","SGCHSyncedContents")){
                        val encoded=pdf.newByteString(PdfTextString.encode(edit.quotation))
                        try{obj.put(key,encoded)}finally{encoded.destroy()}
                    }
                }finally{obj.destroy()}
                annotation.update()
            }finally{annotation.destroy()}
            page.update()
        }finally{page.destroy()}
    }
    fun remove(pdf:PDFDocument,edit:HighlightEdit){
        require(edit.page in 0 until pdf.countPages())
        val page=pdf.loadPage(edit.page) as PDFPage
        try {
            val annotations=page.annotations ?: emptyArray()
            try {
                val found=annotations.filter{id(it)==edit.id}
                require(found.size==1 && found.single().type==PDFAnnotation.TYPE_HIGHLIGHT){"Owned highlight identity not unique"}
                page.deleteAnnotation(found.single());page.update()
            }finally{annotations.forEach{it.destroy()}}
        }finally{page.destroy()}
    }
}
