// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
class NativeAppearanceEquivalenceTest{
    private fun q(x:Float=40f,y:Float=35.75461f)=GlyphQuad(TextPoint(x,y),TextPoint(x+20f,y),TextPoint(x,y+7f),TextPoint(x+20f,y+7f))
    private fun item(paint:List<GlyphQuad>):ManagedHighlight{
        val raw=listOf(q(),q(64f))
        return ManagedHighlight(HighlightEdit(UUID.randomUUID().toString(),0,3,"Exact quotation",raw,paint),"Manual Contents",TextMode.MANUAL)
    }
    @Test fun exactPaintEqual(){assertTrue(NativeAppearanceEquivalence.same(listOf(q()),listOf(q())))}
    @Test fun pdfYRoundTripNoiseEqual(){
        val a=q();val height=841.89f
        fun p(t:TextPoint)=TextPoint(t.x,height-(height-t.y))
        val b=GlyphQuad(p(a.ul),p(a.ur),p(a.ll),p(a.lr))
        assertTrue(NativeAppearanceEquivalence.same(listOf(a),listOf(b)))
    }
    @Test fun boundedSubPointNoiseEqual(){assertTrue(NativeAppearanceEquivalence.same(listOf(q()),listOf(q(y=35.75481f))))}
    @Test fun largerShiftStillChange(){assertFalse(NativeAppearanceEquivalence.same(listOf(q()),listOf(q(y=35.75661f))))}
    @Test fun bandCountMustMatch(){assertFalse(NativeAppearanceEquivalence.same(listOf(q()),listOf(q(),q(64f))))}
    @Test fun bandOrderMustMatch(){assertFalse(NativeAppearanceEquivalence.same(listOf(q(),q(64f)),listOf(q(64f),q())))}
    @Test fun everyCornerCompared(){val a=q();val b=a.copy(lr=TextPoint(a.lr.x+.002f,a.lr.y));assertFalse(NativeAppearanceEquivalence.same(listOf(a),listOf(b)))}
    @Test fun logicalGeometryNeverMutated(){val a=listOf(q());val saved=a.toList();NativeAppearanceEquivalence.same(a,listOf(q(y=35.75481f)));assertEquals(saved,a)}
    @Test fun documentRoundTripPaintIsNoop(){val raw=item(emptyList());val target=NativeHighlightAppearance.forQuads(raw.edit.quads);val noisy=target.map{v->
            fun p(t:TextPoint)=TextPoint(t.x,t.y+.0002f)
            GlyphQuad(p(v.ul),p(v.ur),p(v.ll),p(v.lr))
        };val before=raw.copy(edit=raw.edit.copy(appearance=noisy));assertEquals(0,DocumentAppearanceRepair.plan("a".repeat(64),1,listOf(before)).highlights);assertEquals(noisy,before.edit.appearance)}
    @Test fun fragmentedRealPaintStillNeedsRepair(){val raw=item(emptyList());val before=raw.copy(edit=raw.edit.copy(appearance=raw.edit.quads));assertEquals(1,DocumentAppearanceRepair.plan("a".repeat(64),1,listOf(before)).highlights)}
}
