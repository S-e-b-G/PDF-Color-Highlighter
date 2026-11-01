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
    fun add(pdf:PDFDocument,edit:HighlightEdit){
        require(pdf.hasPermission(Document.PERMISSION_COPY)){"PDF copy permission is required for extracted quotations"}
        require(edit.page in 0 until pdf.countPages())
        val page=pdf.loadPage(edit.page) as PDFPage
        try {
            val existing=page.annotations ?: emptyArray()
            try{require(existing.none{id(it)==edit.id}){"Highlight identity already exists"}}finally{existing.forEach{it.destroy()}}
            val annotation=page.createAnnotation(PDFAnnotation.TYPE_HIGHLIGHT)
            try {
                annotation.setQuadPoints(edit.quads.map{q->Quad(q.ul.x,q.ul.y,q.ur.x,q.ur.y,q.ll.x,q.ll.y,q.lr.x,q.lr.y)}.toTypedArray())
                val color=colors[edit.slot]
                annotation.setColor(floatArrayOf(((color shr 16) and 255)/255f,((color shr 8) and 255)/255f,(color and 255)/255f))
                annotation.setOpacity(.5f)
                annotation.setFlags(annotation.flags or PDFAnnotation.IS_PRINT)
                annotation.setContents(edit.quotation)
                val obj=annotation.`object`
                try {
                    obj.put("NM",edit.id);obj.put("SGCHTextMode",TextMode.AUTOMATIC.name)
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
