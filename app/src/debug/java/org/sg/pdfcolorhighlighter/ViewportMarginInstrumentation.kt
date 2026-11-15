// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter
import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import org.sg.pdfcolorhighlighter.domain.PageViewportState
import org.sg.pdfcolorhighlighter.ui.PdfViewportView

/** Synthetic Canvas proof; actual pan gestures are covered separately in isolated UI workflows. */
class ViewportMarginInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        var failure:Throwable?=null
        runOnMainSync{
            val source=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
            val refined=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888)
            val out=Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888)
            try{
                source.eraseColor(Color.WHITE);refined.eraseColor(Color.WHITE)
                val v=PdfViewportView(targetContext);v.layout(0,0,400,400)
                val states=listOf(PageViewportState(2f,0f,.5f),PageViewportState(2f,1f,.5f),PageViewportState(2f,.5f,0f),PageViewportState(2f,.5f,1f))
                val samples=listOf(intArrayOf(400,300,200,300),intArrayOf(200,300,400,300),intArrayOf(300,400,300,200),intArrayOf(300,200,300,400))
                for(i in states.indices){
                    v.show(source,states[i],100f,100f,(i+1).toLong(),(i+1).toLong());out.eraseColor(Color.MAGENTA)
                    val c=Canvas(out);val saved=c.save();try{c.translate(100f,100f);v.draw(c)}finally{c.restoreToCount(saved)}
                    val xy=samples[i];check(out.getPixel(xy[0],xy[1])==Color.WHITE){"Expected visible page half missing"}
                    check(out.getPixel(xy[2],xy[3])==Color.rgb(225,228,232)){"Expected grey beyond page edge missing"}
                    val pixels=IntArray(600*600);out.getPixels(pixels,0,600,0,0,600,600)
                    for(y in 0 until 600)for(x in 0 until 600)if(x !in 100 until 500 || y !in 100 until 500){check(pixels[y*600+x]==Color.MAGENTA){"Drawing escaped PDF viewport"}}
                }
                val before=v.viewportSnapshot();v.show(refined,before,100f,100f,99L,4L)
                check(v.viewportSnapshot()==before){"Same-page refinement reset extended pan"}
                v.fitPage();check(v.viewportSnapshot()==PageViewportState())
            }catch(t:Throwable){failure=t}finally{source.recycle();refined.recycle();out.recycle()}
        }
        finish(if(failure==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply{
            putString("result",if(failure==null)"PASS: page margins centre clipping refinement and fit"else"FAIL: viewport margins")
            failure?.let{putString("details",it.stackTraceToString())}
        })
    }
}
