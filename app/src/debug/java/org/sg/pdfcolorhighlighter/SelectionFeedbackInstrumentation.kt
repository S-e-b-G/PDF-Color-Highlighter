// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.ui.PdfViewportView

/** Synthetic paint-only ranges verify continuous feedback, gutter separation and unchanged glyph quads. */
class SelectionFeedbackInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        var failure:Throwable?=null
        runOnMainSync{
            var image:Bitmap?=null;var output:Bitmap?=null
            try{
                val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).also{image=it;it.eraseColor(Color.WHITE)}
                fun glyph(x:Float)=TextGlyph(65,GlyphQuad(TextPoint(x,20f),TextPoint(x+10f,20f),TextPoint(x,30f),TextPoint(x+10f,30f)))
                val a=glyph(10f);val b=glyph(28f);val c=glyph(70f)
                val snapshot=PageTextSnapshot(TextBounds(0f,0f,100f,100f),listOf(PageTextLine(0,0,listOf(a,b))),PageTextStatus.READY)
                val span=SelectionSpan(TextAddress(0,0),TextAddress(0,1),"A B","A B",listOf(a,b))
                val before=span.quads
                val view=PdfViewportView(targetContext);view.layout(0,0,100,100)
                view.show(bitmap,PageViewportState(),100f,100f,1L);view.setSelectionPreview(snapshot,3,span)
                val target=Bitmap.createBitmap(140,140,Bitmap.Config.ARGB_8888).also{output=it}
                fun draw(){
                    target.eraseColor(Color.MAGENTA);val canvas=Canvas(target);val saved=canvas.save()
                    try{canvas.translate(20f,20f);view.draw(canvas)}finally{canvas.restoreToCount(saved)}
                }
                draw()
                check(target.getPixel(44,45)!=Color.WHITE){"Inferred word gap was not painted continuously"}
                check(span.quads==before && span.quads.size==2){"Paint helper modified real selection geometry"}
                // Deliberate disconnected test span: production selector still rejects such column crossings.
                val separated=span.copy(glyphs=listOf(a,c))
                view.setSelectionPreview(snapshot,3,separated);draw()
                check(target.getPixel(65,45)==Color.WHITE){"Paint band crossed a wide gutter"}
                check(target.getPixel(35,45)!=Color.WHITE)
                check(target.getPixel(0,0)==Color.MAGENTA && target.getPixel(139,139)==Color.MAGENTA)
            }catch(error:Throwable){failure=error}
            finally{image?.recycle();output?.recycle()}
        }
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: selection feedback" else "FAIL: selection feedback")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
}
