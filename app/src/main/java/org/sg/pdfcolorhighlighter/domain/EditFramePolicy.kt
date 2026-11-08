// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
object EditFramePolicy {
    const val EDIT_EDGE=1600
    const val READER_EDGE=1600
    fun editEdge(displayedWidth:Int,displayedHeight:Int):Int = maxOf(EDIT_EDGE,displayedWidth,displayedHeight)
    fun retainText(oldPage:Int,newPage:Int,oldWidth:Float,newWidth:Float,oldHeight:Float,newHeight:Float,
        canCopy:Boolean,snapshot:PageTextSnapshot?):Boolean = snapshot!=null && canCopy && oldPage==newPage &&
        oldWidth==newWidth && oldHeight==newHeight
}
