// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import com.artifex.mupdf.fitz.*
import org.sg.pdfcolorhighlighter.domain.*
import kotlin.math.abs

/** Owning serialized worker only. Callback Font/Image wrappers own kept references in MuPDF 1.28.5;
 * release them during the callback. No native object, font name or document identity enters the DTO.
 */
object MuPdfTextSnapshot {
    const val MAX_GLYPHS=100000
    private class LimitReached:RuntimeException()
    fun extract(page:Page,copyAllowed:Boolean):PageTextSnapshot {
        val b=page.bounds
        val bounds=TextBounds(b.x0,b.y0,b.x1,b.y1)
        if(!copyAllowed)return PageTextSnapshot(bounds,emptyList(),PageTextStatus.COPY_RESTRICTED)
        val text=page.toStructuredText()
        val result=mutableListOf<PageTextLine>()
        var total=0
        var blockIndex=-1
        var glyphs=mutableListOf<TextGlyph>()
        var reliable=true
        var horizontal=true
        try {
            text.walk(object:StructuredTextWalker {
                override fun onImageBlock(bbox:Rect,transform:Matrix,image:Image){image.destroy()}
                override fun beginTextBlock(bbox:Rect,flags:Int){blockIndex++}
                override fun endTextBlock(){}
                override fun beginLine(bbox:Rect,wmode:Int,dir:Point){
                    glyphs=mutableListOf()
                    horizontal=wmode==0 && abs(dir.x)>0.98f && abs(dir.y)<0.05f
                    reliable=horizontal && dir.x>0.98f
                }
                override fun endLine(){
                    TextGeometryPolicy.splitRuns(glyphs).forEachIndexed{runIndex,run->
                        if(run.isNotEmpty())result.add(PageTextLine(blockIndex,runIndex,run.toList(),reliable,horizontal=horizontal))
                    }
                }
                override fun onChar(c:Int,origin:Point,font:Font,size:Float,q:Quad,argb:Int,flags:Int,bidi:Int){
                    try {
                        total++
                        if(total>MAX_GLYPHS)throw LimitReached()
                        if(!Character.isValidCodePoint(c) || c in 0xD800..0xDFFF){reliable=false;return}
                        if(bidi%2!=0)reliable=false
                        val unicodeReliable=(flags and
                            (StructuredText.CHAR_FLAGS_UNICODE_IS_CID or StructuredText.CHAR_FLAGS_UNICODE_IS_GID))==0
                        // Inspect a font only for the two reviewed exceptional representations.
                        val cp=if(c==0x000F || c==0xFFFD)
                            KnownSymbolPolicy.normalize(c,font.name,unicodeReliable,glyphs.all{it.whitespace})else c
                        try {
                            glyphs.add(TextGlyph(cp,GlyphQuad(TextPoint(q.ul_x,q.ul_y),TextPoint(q.ur_x,q.ur_y),
                                TextPoint(q.ll_x,q.ll_y),TextPoint(q.lr_x,q.lr_y)),unicodeReliable,
                                (flags and StructuredText.CHAR_FLAGS_SYNTHETIC)!=0,c))
                        }catch(_:IllegalArgumentException){reliable=false}
                    }finally{font.destroy()}
                }
                override fun beginStruct(standard:String?,raw:String?,index:Int){}
                override fun endStruct(){}
                override fun onVector(bbox:Rect,info:StructuredTextWalker.VectorInfo,argb:Int){}
            })
        }catch(_:LimitReached){
            return PageTextSnapshot(bounds,emptyList(),PageTextStatus.TOO_COMPLEX)
        }finally{text.destroy()}
        val snapshot=PageTextSnapshot(bounds,result.toList(),if(result.isEmpty())PageTextStatus.EMPTY else PageTextStatus.READY)
        return InlineReferenceRows.normalize(TextRowCoalescer.normalize(snapshot))
    }
}
