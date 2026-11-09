// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.ui.PdfViewportView

class NoteGestureInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        var failure:Throwable?=null
        runOnMainSync{
            val image=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
            try{
                val v=PdfViewportView(targetContext);v.layout(0,0,400,400)
                v.show(image,PageViewportState(),100f,100f,1L)
                v.setNotePageBounds(TextBounds(10f,20f,110f,120f))
                var taps=0;var point:TextPoint?=null
                v.onNoteTap={taps++;point=it;true}
                var down=SystemClock.uptimeMillis()
                fun event(action:Int,x:Float,y:Float){
                    val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0)
                    try{check(v.onTouchEvent(e))}finally{e.recycle()}
                }
                event(MotionEvent.ACTION_DOWN,200f,200f);event(MotionEvent.ACTION_UP,200f,200f)
                check(taps==1 && point==TextPoint(60f,70f)){"Empty text/nonzero page origin note mapping failed"}
                down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,200f,200f)
                event(MotionEvent.ACTION_MOVE,350f,350f);event(MotionEvent.ACTION_UP,350f,350f);check(taps==1)
                down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,200f,200f)
                event(MotionEvent.ACTION_CANCEL,200f,200f);check(taps==1)
                down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,200f,200f)
                val props=Array(2){i->MotionEvent.PointerProperties().apply{id=i;toolType=MotionEvent.TOOL_TYPE_FINGER}}
                val coords=Array(2){i->MotionEvent.PointerCoords().apply{x=180f+i*40;y=200f;pressure=1f;size=1f}}
                val multi=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,props,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
                try{v.onTouchEvent(multi)}finally{multi.recycle()}
                event(MotionEvent.ACTION_UP,200f,200f);check(taps==1){"Pinch placed a note"}
                v.setInteractionEnabled(false)
                run {
                    val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_DOWN,200f,200f,0)
                    try{check(!v.onTouchEvent(e))}finally{e.recycle()}
                }
                check(taps==1)
            }catch(t:Throwable){failure=t}finally{image.recycle()}
        }
        val e=failure
        finish(if(e==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(e==null)"PASS: note gesture routing"else"FAIL: note gesture routing")
            if(e!=null)putString("details",e.stackTraceToString())
        })
    }
}
