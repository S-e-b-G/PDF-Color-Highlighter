// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class ManagedNoteTest {
    private val id="00000000-0000-0000-0000-000000000001"
    private val sheet=TextBounds(10f,20f,610f,820f)
    private fun note(text:String="été\ncœur",slot:Int=0)=NotePolicy.place(0,slot,TextPoint(100f,120f),sheet,text,id)
    @Test fun identityIsStable(){assertEquals(id,note().id)}
    @Test fun pageIsCopied(){assertEquals(0,note().page)}
    @Test fun defaultSize(){assertEquals(24f,note().bounds.width,0f);assertEquals(24f,note().bounds.height,0f)}
    @Test fun centeredAtTap(){assertEquals(88f,note().bounds.left,0f);assertEquals(108f,note().bounds.top,0f)}
    @Test fun topLeftClamped(){assertEquals(TextBounds(10f,20f,34f,44f),NotePolicy.place(0,0,TextPoint(10f,20f),sheet,"A",id).bounds)}
    @Test fun bottomRightClamped(){assertEquals(TextBounds(586f,796f,610f,820f),NotePolicy.place(0,0,TextPoint(610f,820f),sheet,"A",id).bounds)}
    @Test fun tinyPage(){assertEquals(TextBounds(0f,0f,10f,12f),NotePolicy.place(0,0,TextPoint(5f,6f),TextBounds(0f,0f,10f,12f),"A",id).bounds)}
    @Test(expected=IllegalArgumentException::class) fun offPageRejected(){NotePolicy.place(0,0,TextPoint(0f,0f),sheet,"A",id)}
    @Test(expected=IllegalArgumentException::class) fun invalidSlotRejected(){note(slot=6)}
    @Test(expected=IllegalArgumentException::class) fun invalidIdRejected(){note().copy(id="other")}
    @Test(expected=IllegalArgumentException::class) fun negativePageRejected(){note().copy(page=-1)}
    @Test fun emptyRejected(){assertFalse(NotePolicy.validText(""));assertFalse(NotePolicy.validText(" \n\t"))}
    @Test fun nullCharacterRejected(){assertFalse(NotePolicy.validText("a\u0000b"))}
    @Test fun textLimit(){assertTrue(NotePolicy.validText("x".repeat(100000)));assertFalse(NotePolicy.validText("x".repeat(100001)))}
    @Test fun unicodeAndLinesUnchanged(){val text="élève — cœur\n日本語 e\u0301";assertEquals(text,note(text).contents)}
    @Test fun recolorDoesNotChangeTextOrIdentity(){val n=note();val next=n.copy(slot=5);assertEquals(n.contents,next.contents);assertEquals(n.id,next.id);assertEquals(n.bounds,next.bounds)}
    @Test fun exactHit(){assertEquals(listOf(note()),NotePolicy.hits(listOf(note()),TextPoint(100f,120f)))}
    @Test fun noHit(){assertTrue(NotePolicy.hits(listOf(note()),TextPoint(200f,220f)).isEmpty())}
    @Test fun overlappingHitsStayAmbiguous(){val n=note();assertEquals(2,NotePolicy.hits(listOf(n,n.copy(id="00000000-0000-0000-0000-000000000002")),TextPoint(100f,120f)).size)}
    @Test fun paletteMatchesSixTools(){assertEquals(6,(0..5).map{NotePolicy.color(it)}.distinct().size);assertEquals(0xFCD7FB,NotePolicy.color(5))}
}
