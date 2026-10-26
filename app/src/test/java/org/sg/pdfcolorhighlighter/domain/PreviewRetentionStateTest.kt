// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class PreviewRetentionStateTest{
    private val g=TextGlyph(65,GlyphQuad(TextPoint(0f,0f),TextPoint(1f,0f),TextPoint(0f,1f),TextPoint(1f,1f)))
    private val snapshot=PageTextSnapshot(TextBounds(0f,0f,100f,100f),listOf(PageTextLine(0,0,listOf(g))),PageTextStatus.READY)
    private val span=SelectionSpan(TextAddress(0,0),TextAddress(0,0),"A","A",listOf(g))
    @Test fun currentSnapshotCanRestoreCommittedSpan(){val r=PreviewRetentionState();r.commit(snapshot,span);assertSame(span,r.restore(snapshot))}
    @Test fun equalButDifferentSnapshotCannotRestoreOldPage(){val r=PreviewRetentionState();r.commit(snapshot,span);assertNull(r.restore(snapshot.copy()))}
    @Test fun missingSnapshotDoesNotRestore(){val r=PreviewRetentionState();r.commit(snapshot,span);assertNull(r.restore(null))}
    @Test fun clearDropsCommittedRange(){val r=PreviewRetentionState();r.commit(snapshot,span);r.clear();assertNull(r.restore(snapshot))}
    @Test fun latestCommitReplacesEarlierSelection(){val r=PreviewRetentionState();r.commit(snapshot,span);val next=span.copy(quotation="new");r.commit(snapshot,next);assertSame(next,r.restore(snapshot))}
    @Test fun restoreDoesNotMutateCheckpoint(){val r=PreviewRetentionState();r.commit(snapshot,span);repeat(5){assertSame(span,r.restore(snapshot))}}
    @Test fun freshSessionHasNoCommittedSelection(){assertNull(PreviewRetentionState().restore(snapshot))}
    @Test fun committingAnotherOwnerCannotLeakPreviousRange(){val r=PreviewRetentionState();r.commit(snapshot,span);val other=snapshot.copy();r.commit(other,span);assertNull(r.restore(snapshot));assertSame(span,r.restore(other))}
}
