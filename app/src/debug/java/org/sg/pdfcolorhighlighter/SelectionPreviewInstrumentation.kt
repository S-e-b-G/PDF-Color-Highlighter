// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import org.sg.pdfcolorhighlighter.domain.*
import org.sg.pdfcolorhighlighter.ui.PdfViewportView

/** Synthetic View touch/coordinate/overlay regression; not the real Compose toolbar or a pinch test. */
class SelectionPreviewInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?) { super.onCreate(arguments);start() }
    override fun onStart() {
        var failure:Throwable?=null
        runOnMainSync {
            var image:Bitmap?=null;var another:Bitmap?=null;var output:Bitmap?=null
            try {
                val first=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).also {image=it;it.eraseColor(Color.WHITE)}
                fun quad(x:Float)=GlyphQuad(TextPoint(x,30f),TextPoint(x+10f,30f),TextPoint(x,40f),TextPoint(x+10f,40f))
                val glyphs=listOf(TextGlyph(65,quad(20f)),TextGlyph(32,quad(30f)),TextGlyph(66,quad(40f)))
                val snapshot=PageTextSnapshot(TextBounds(10f,20f,110f,120f),listOf(PageTextLine(0,0,glyphs)),PageTextStatus.READY)
                val view=PdfViewportView(targetContext)
                view.layout(0,0,100,100)
                var state=PageViewportState();view.onViewportChanged={state=it}
                view.show(first,PageViewportState(),100f,100f,1L)
                view.setSelectionPreview(snapshot,0,null)
                var last:SelectionSpan?=null;var finished=false;var cancels=0
                view.onPreviewDrag={a,b,end ->
                    val selection=PageTextSelector.select(snapshot,a,b)
                    last=(selection as? TextSelectionAttempt.Selected)?.span
                    finished=end
                    view.setSelectionPreview(snapshot,0,last)
                }
                view.onPreviewCancel={cancels++;last=null;view.setSelectionPreview(snapshot,0,null)}
                var down=SystemClock.uptimeMillis()
                fun event(action:Int,x:Float,y:Float) {
                    val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0)
                    try {check(view.onTouchEvent(e))} finally {e.recycle()}
                }
                val before=state
                event(MotionEvent.ACTION_DOWN,15f,15f)
                event(MotionEvent.ACTION_MOVE,35f,15f)
                event(MotionEvent.ACTION_UP,35f,15f)
                check(finished && last?.quotation=="A B") {"Preview did not map native origin/drag/release correctly"}
                check(state==before) {"Preview drag panned the document"}
                val target=Bitmap.createBitmap(140,140,Bitmap.Config.ARGB_8888).also {output=it;it.eraseColor(Color.MAGENTA)}
                val canvas=Canvas(target);val saved=canvas.save()
                try {canvas.translate(20f,20f);view.draw(canvas)}finally{canvas.restoreToCount(saved)}
                check(target.getPixel(35,35)!=Color.WHITE) {"Preview quad was not drawn at the mapped position"}
                check(target.getPixel(0,0)==Color.MAGENTA && target.getPixel(139,139)==Color.MAGENTA)
                down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,15f,15f)
                val count=cancels;event(MotionEvent.ACTION_CANCEL,15f,15f)
                check(cancels>count && last==null) {"ACTION_CANCEL did not clear the preview"}
                view.setSelectionPreview(snapshot,null,null)
                val second=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).also {another=it;it.eraseColor(Color.WHITE)}
                view.show(second,PageViewportState(3f,0.5f,0.5f),100f,100f,2L)
                val navigationBefore=state
                down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,50f,50f)
                event(MotionEvent.ACTION_MOVE,90f,90f);event(MotionEvent.ACTION_UP,90f,90f)
                check(state!=navigationBefore) {"Navigation pan was not restored after preview mode"}
            } catch(error:Throwable) {failure=error}
            finally {image?.recycle();another?.recycle();output?.recycle()}
        }
        val error=failure
        finish(if(error==null) Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply {
            putString("result",if(error==null) "PASS: selection preview" else "FAIL: selection preview")
            if(error!=null)putString("details",error.stackTraceToString())
        })
    }
}
