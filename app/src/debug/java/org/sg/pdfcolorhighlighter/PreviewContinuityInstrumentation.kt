// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.ui.PdfViewportView

/** Synthetic View/retention routing; not real Activity rotation or final PDF saving. */
class PreviewContinuityInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        var failure:Throwable?=null
        runOnMainSync{
            var image:Bitmap?=null
            try{
                val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).also{image=it;it.eraseColor(Color.WHITE)}
                val glyphs="AB".mapIndexed{i,c->val x=20f+i*10f;TextGlyph(c.code,
                    GlyphQuad(TextPoint(x,20f),TextPoint(x+10f,20f),TextPoint(x,30f),TextPoint(x+10f,30f)))}
                val snapshot=PageTextSnapshot(TextBounds(0f,0f,100f,100f),listOf(PageTextLine(0,0,glyphs)),PageTextStatus.READY)
                val span=SelectionSpan(TextAddress(0,0),TextAddress(0,1),"AB","AB",glyphs)
                val retained=PreviewRetentionState();retained.commit(snapshot,span)
                val view=PdfViewportView(targetContext);view.layout(0,0,400,400)
                view.show(bitmap,PageViewportState(),100f,100f,1L)
                var current:SelectionSpan?=span;var drags=0
                fun restore(){current=retained.restore(snapshot);view.setSelectionPreview(snapshot,0,current,current!=null)}
                restore()
                view.onPreviewCancel={restore()}
                view.onPreviewDrag={_,_,_->drags++;current=null;view.setSelectionPreview(snapshot,0,null,false)}
                val down=SystemClock.uptimeMillis()
                fun single(action:Int,x:Float,y:Float){
                    val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0)
                    try{check(view.onTouchEvent(e))}finally{e.recycle()}
                }
                fun multi(action:Int,x0:Float,y0:Float,x1:Float,y1:Float){
                    val props=Array(2){i->MotionEvent.PointerProperties().apply{id=i;toolType=MotionEvent.TOOL_TYPE_FINGER}}
                    val coords=Array(2){i->MotionEvent.PointerCoords().apply{x=if(i==0)x0 else x1;y=if(i==0)y0 else y1;pressure=1f;size=1f}}
                    val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,2,props,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
                    try{check(view.onTouchEvent(e))}finally{e.recycle()}
                }
                single(MotionEvent.ACTION_DOWN,390f,390f)
                check(drags==0 && current===span){"First pointer replaced the completed range before pinch was known"}
                multi(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),390f,390f,200f,200f)
                multi(MotionEvent.ACTION_MOVE,395f,395f,100f,100f)
                multi(MotionEvent.ACTION_MOVE,398f,398f,80f,80f)
                check(current===span){"Pinch did not retain the completed selection"}
                multi(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),398f,398f,80f,80f)
                single(MotionEvent.ACTION_UP,398f,398f)
                view.layout(0,0,450,400)
                check(current===span && retained.restore(snapshot)===span)
                val listSnapshot=PageTextSnapshot(snapshot.pageBounds,listOf(
                    PageTextLine(0,0,listOf(TextGlyph(0x2022,glyphs.first().quad),TextGlyph(32,glyphs.first().quad),TextGlyph(65,glyphs.first().quad))),
                    PageTextLine(1,0,listOf(TextGlyph(0x2022,glyphs.last().quad),TextGlyph(32,glyphs.last().quad),TextGlyph(66,glyphs.last().quad)))),PageTextStatus.READY)
                check(SelectionQuotationPolicy.compose(listSnapshot,listOf(0,1),listOf("• A","• B"))=="• A\n• B")
            }catch(error:Throwable){failure=error}
            finally{image?.recycle()}
        }
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: preview continuity" else "FAIL: preview continuity")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
}
