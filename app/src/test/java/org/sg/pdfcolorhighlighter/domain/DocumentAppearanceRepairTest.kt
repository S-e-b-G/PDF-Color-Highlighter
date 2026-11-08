// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
class DocumentAppearanceRepairTest{
    private val hash="a".repeat(64)
    private fun item(page:Int=0):ManagedHighlight{
        fun q(x:Float)=GlyphQuad(TextPoint(x,10f),TextPoint(x+8,10f),TextPoint(x,20f),TextPoint(x+8,20f))
        val qs=listOf(q(0f),q(12f))
        return ManagedHighlight(HighlightEdit(UUID.randomUUID().toString(),page,2,"Original quotation",qs,qs),"Manual\nContents",TextMode.MANUAL)
    }
    private fun plan(vararg items:ManagedHighlight)=DocumentAppearanceRepair.plan(hash,3,items.toList())
    @Test fun emptyPlanIsNoop(){assertEquals(0,plan().highlights)}
    @Test fun simplePlanRepairsAppearance(){assertEquals(1,plan(item()).changes.single().second.edit.appearance.size)}
    @Test fun allPagesIncluded(){val p=plan(item(0),item(2));assertEquals(2,p.highlights);assertEquals(2,p.affectedPages)}
    @Test fun samePageCountedOnce(){assertEquals(1,plan(item(0),item(0)).affectedPages)}
    @Test fun manualContentsKept(){val x=item();assertEquals(x.contents,plan(x).changes.single().second.contents)}
    @Test fun modeKept(){assertEquals(TextMode.MANUAL,plan(item()).changes.single().second.mode)}
    @Test fun idQuotationSlotAndExactQuadsKept(){val x=item();val y=plan(x).changes.single().second;assertEquals(x.edit,y.edit.copy(appearance=x.edit.appearance))}
    @Test fun cleanItemsSkipped(){val x=item();val clean=x.copy(edit=x.edit.copy(appearance=NativeHighlightAppearance.forQuads(x.edit.quads)));assertEquals(0,plan(clean).highlights)}
    @Test fun badBaseHashRejected(){try{DocumentAppearanceRepair.plan("bad",1,listOf(item()));fail()}catch(_:IllegalArgumentException){}}
    @Test fun staleReviewRejected(){try{DocumentAppearanceRepair.verify(plan(item()),"b".repeat(64));fail()}catch(_:IllegalArgumentException){}}
    @Test fun duplicateSamePageRejected(){val x=item();try{plan(x,x);fail()}catch(_:IllegalArgumentException){}}
    @Test fun sameIdDifferentPagesDistinct(){val x=item();assertEquals(2,plan(x,x.copy(edit=x.edit.copy(page=1))).highlights)}
    @Test fun invalidPageRejected(){try{DocumentAppearanceRepair.plan(hash,1,listOf(item(2)));fail()}catch(_:IllegalArgumentException){}}
    @Test fun nonAppearanceEditRejected(){val x=item();val p=DocumentAppearancePlan(hash,1,listOf(x to x.copy(contents="changed")));try{DocumentAppearanceRepair.verify(p,hash);fail()}catch(_:IllegalArgumentException){}}
}
