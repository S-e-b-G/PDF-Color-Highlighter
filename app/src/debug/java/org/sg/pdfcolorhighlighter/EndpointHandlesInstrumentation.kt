// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.ui.PdfViewportView

/** Synthetic View touch routing: character adjustment/fixed endpoint/tap/cancel, not real hardware pinch. */
class EndpointHandlesInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        var failure:Throwable?=null
        runOnMainSync{
            var bitmap:Bitmap?=null
            try{
                val image=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).also{bitmap=it;it.eraseColor(Color.WHITE)}
                val glyphs="ABCD".mapIndexed{i,c->val x=20f+i*10f;TextGlyph(c.code,
                    GlyphQuad(TextPoint(x,20f),TextPoint(x+10f,20f),TextPoint(x,30f),TextPoint(x+10f,30f)))}
                val bounds=TextBounds(0f,0f,100f,100f)
                val snapshot=PageTextSnapshot(bounds,listOf(PageTextLine(0,0,glyphs)),PageTextStatus.READY)
                val original=SelectionSpan(TextAddress(0,1),TextAddress(0,2),"BC","BC",glyphs.subList(1,3))
                val mapping=PageCoordinateMap(bounds,0f,0f,400f,400f)
                val density=targetContext.resources.displayMetrics.density
                val handles=checkNotNull(SelectionHandleGeometry.layout(original,mapping,density,400f,400f))
                val view=PdfViewportView(targetContext);view.layout(0,0,400,400)
                view.show(image,PageViewportState(),100f,100f,1L)
                view.setSelectionPreview(snapshot,1,original,true)
                var adjustments=0;var finished=false;var fixed:TextAddress?=null;var last=original;var cancels=0
                view.onPreviewAdjust={moving,opposite,end->
                    adjustments++;fixed=opposite;finished=end
                    val selected=PageTextSelector.adjust(snapshot,moving,opposite)
                    check(selected is TextSelectionAttempt.Selected)
                    last=selected.span
                    view.setSelectionPreview(snapshot,1,last,end)
                }
                view.onPreviewCancel={cancels++;view.setSelectionPreview(snapshot,1,null,false)}
                var down=SystemClock.uptimeMillis()
                fun event(action:Int,x:Float,y:Float){
                    val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0)
                    try{check(view.onTouchEvent(e))}finally{e.recycle()}
                }
                val end=handles.end.center
                event(MotionEvent.ACTION_DOWN,end.x,end.y);event(MotionEvent.ACTION_UP,end.x,end.y)
                check(adjustments==0 && cancels==0){"Handle tap changed the selection"}
                val delta=ViewConfiguration.get(targetContext).scaledTouchSlop+40f
                down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,end.x,end.y)
                event(MotionEvent.ACTION_MOVE,end.x+delta,end.y);event(MotionEvent.ACTION_UP,end.x+delta,end.y)
                check(adjustments>0 && finished && fixed==TextAddress(0,1) && last.quotation=="BCD"){
                    "Handle drag did not perform character-level adjustment with fixed opposite endpoint"
                }
                view.setSelectionPreview(snapshot,1,original,true)
                down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,end.x,end.y)
                event(MotionEvent.ACTION_MOVE,end.x+delta,end.y)
                val before=cancels;event(MotionEvent.ACTION_CANCEL,end.x+delta,end.y)
                check(cancels>before){"Canceled handle drag was not cleared"}
            }catch(error:Throwable){failure=error}
            finally{bitmap?.recycle()}
        }
        val error=failure
        finish(if(error==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(error==null)"PASS: endpoint handles" else "FAIL: endpoint handles")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
}
