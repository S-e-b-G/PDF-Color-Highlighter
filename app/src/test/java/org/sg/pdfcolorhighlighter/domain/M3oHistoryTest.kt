// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
class M3oHistoryTest {
    private fun trial(body:(File)->Unit){val root=Files.createTempDirectory("m3o-history").toFile();try{body(root)}finally{root.deleteRecursively()}}
    @Test fun keeps1000Commands(){trial{r->
        val a=File(r,"a").apply{writeText("a")};val b=File(r,"b").apply{writeText("b")}
        val change=CheckpointChange(a,b);val history=CheckpointHistory()
        repeat(1001){history.added(change)}
        var n=0;while(history.undo!=null){history.didUndo(history.undo!!);n++}
        assertEquals(1000,n)
    }}
    @Test fun redoStillWorks(){trial{r->
        val a=File(r,"a").apply{writeText("a")};val b=File(r,"b").apply{writeText("b")};val c=CheckpointChange(a,b)
        val h=CheckpointHistory();h.added(c);h.didUndo(c);assertEquals(c,h.redo);h.didRedo(c);assertEquals(c,h.undo)
    }}
    @Test fun explicitByteBudgetStillBounds(){trial{r->
        val list=(0..8).map{File(r,"f$it").apply{writeText("x")}};val h=CheckpointHistory(1000,3)
        for(i in 0 until list.lastIndex)h.added(CheckpointChange(list[i],list[i+1]))
        var n=0;while(h.undo!=null){h.didUndo(h.undo!!);n++};assertEquals(2,n)
    }}
    @Test fun newEditClearsRedo(){trial{r->
        val a=File(r,"a").apply{writeText("a")};val b=File(r,"b").apply{writeText("b")};val c=CheckpointChange(a,b)
        val h=CheckpointHistory();h.added(c);h.didUndo(c);h.added(c);assertNull(h.redo)
    }}
}
