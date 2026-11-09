// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.util.UUID

/** Copied app-owned native Text state, never a native handle or a sidecar. */
data class ManagedNote(val id:String,val page:Int,val slot:Int,val bounds:TextBounds,val contents:String) {
    init {
        require(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(id))
        require(page>=0 && slot in 0..5 && bounds.width>0f && bounds.height>0f)
        require(NotePolicy.validText(contents))
    }
}
object NotePolicy {
    private val palette=intArrayOf(0xFFAD5B,0x00FF00,0xFC3636,0xFFFF00,0xD1DFEB,0xFCD7FB)
    fun color(slot:Int):Int { require(slot in 0..5);return palette[slot] }
    fun validText(text:String)=text.isNotBlank() && text.length<=100000 && '\u0000' !in text
    fun place(page:Int,slot:Int,point:TextPoint,sheet:TextBounds,contents:String,id:String=UUID.randomUUID().toString()):ManagedNote {
        require(sheet.width>0f && sheet.height>0f)
        require(point.x in sheet.left..sheet.right && point.y in sheet.top..sheet.bottom)
        val w=minOf(24f,sheet.width);val h=minOf(24f,sheet.height)
        val x=(point.x-w/2).coerceIn(sheet.left,sheet.right-w)
        val y=(point.y-h/2).coerceIn(sheet.top,sheet.bottom-h)
        return ManagedNote(id,page,slot,TextBounds(x,y,x+w,y+h),contents)
    }
    fun hits(notes:List<ManagedNote>,point:TextPoint)=notes.filter{
        point.x in it.bounds.left..it.bounds.right && point.y in it.bounds.top..it.bounds.bottom
    }
}
