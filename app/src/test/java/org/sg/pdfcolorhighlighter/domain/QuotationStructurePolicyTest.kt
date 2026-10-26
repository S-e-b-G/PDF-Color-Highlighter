// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test
class QuotationStructurePolicyTest{
    private fun line(text:String,y:Float=20f,x:Float=20f,block:Int=0)=PageTextLine(block,0,text.codePoints().toArray().mapIndexed{i,cp->
        val left=x+i*5f;TextGlyph(cp,GlyphQuad(TextPoint(left,y),TextPoint(left+5f,y),TextPoint(left,y+10f),TextPoint(left+5f,y+10f)))})
    private fun quote(vararg lines:PageTextLine)=SelectionQuotationPolicy.compose(PageTextSnapshot(TextBounds(0f,0f,600f,800f),lines.toList(),PageTextStatus.READY),lines.indices.toList(),lines.map{it.text})
    @Test fun ordinaryWrapStillJoinsWithSpace(){assertEquals("Une phrase se poursuit",quote(line("Une phrase"),line("se poursuit",y=35f)))}
    @Test fun unicodeBulletItemsStayOnTheirOwnLines(){assertEquals("• un\n• deux",quote(line("• un"),line("• deux",y=35f,block=1)))}
    @Test fun wrappedBulletItemHasNoExtraHardBreak(){assertEquals("• item très long qui se poursuit\n• suivant",quote(line("• item très long"),line("qui se poursuit",y=35f),line("• suivant",y=50f,block=1)))}
    @Test fun repeatedDashItemsKeepItemBreaks(){assertEquals("- un\n- deux",quote(line("- un"),line("- deux",y=35f,block=1)))}
    @Test fun numberedItemsKeepTheirNumbersAndBreaks(){assertEquals("1. un\n2. deux",quote(line("1. un"),line("2. deux",y=35f,block=1)))}
    @Test fun letteredItemsAreRecognizedWithPeerContext(){assertEquals("a) un\nb) deux",quote(line("a) un"),line("b) deux",y=35f,block=1)))}
    @Test fun decimalIsNotAListMarker(){assertEquals("1.25 valeur continue",quote(line("1.25 valeur"),line("continue",y=35f,block=1)))}
    @Test fun minusNumberIsNotADashList(){assertEquals("-3 unités continue",quote(line("-3 unités"),line("continue",y=35f,block=1)))}
    @Test fun isolatedDashWithoutPeerDoesNotInventItemBreak(){assertEquals("phrase - parenthèse",quote(line("phrase"),line("- parenthèse",y=35f,block=1)))}
    @Test fun clearInterBlockParagraphGapIsPreserved(){assertEquals("previous paragraph\n\nnext paragraph",quote(line("previous paragraph",y=661.81f,block=8),line("next paragraph",y=679.45f,x=28f,block=9)))}
    @Test fun normalizationPreservesAccentsAndVisibleHyphen(){assertEquals("inter-national élève",quote(line("inter-"),line("national élève",y=35f)))}
    @Test fun partialSelectedRowsUseFullSourceListContext(){val lines=listOf(line("• item long"),line("suite",y=35f),line("• suivant",y=50f,block=1));val s=PageTextSnapshot(TextBounds(0f,0f,600f,800f),lines,PageTextStatus.READY);assertEquals("suite\n• suivant",SelectionQuotationPolicy.compose(s,listOf(1,2),listOf("suite","• suivant")))}

    @Test fun wrappedItemAcrossNativeBlocksStillJoins(){assertEquals("• an item that wraps here\n• following",quote(line("• an item that",block=0),line("wraps here",y=35f,block=1),line("• following",y=50f,block=2)))}
    @Test fun regularDoubleSpacedWrapsAreNotAllParagraphs(){assertEquals("A long sentence continues and then ends",quote(line("A long sentence",block=0),line("continues",y=40f,block=1),line("and then",y=60f,block=2),line("ends",y=80f,block=3)))}
}
