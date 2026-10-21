// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class QuotationNormalizerTest {
    @Test fun joinsTwoVisualLines() {
        val result = QuotationNormalizer.fromParagraphs(listOf(listOf("Une phrase sur", "deux lignes.")))
        assertEquals("Une phrase sur deux lignes.", result)
        assertFalse(result.contains('\n'))
        assertFalse(result.contains('\r'))
    }
    @Test fun joinsThreeVisualLinesWithOneSpace() {
        assertEquals("Un texte assez long.", QuotationNormalizer.fromParagraphs(
            listOf(listOf("  Un texte ", " assez ", "long.  "))))
    }
    @Test fun preservesExplicitParagraphBoundary() {
        assertEquals("Premier paragraphe.\n\nSecond paragraphe.",
            QuotationNormalizer.fromParagraphs(listOf(listOf("Premier paragraphe."), listOf("Second paragraphe."))))
    }
    @Test fun retainsVisibleHyphenWithoutAddingAWordSpace() {
        assertEquals("porte-monnaie", QuotationNormalizer.fromParagraphs(listOf(listOf("porte-", "monnaie"))))
    }
    @Test fun removesExplicitSoftHyphenAtWordWrap() {
        assertEquals("préparation", QuotationNormalizer.fromParagraphs(listOf(listOf("pré\u00AD", "paration"))))
    }
    @Test fun preservesInternalSpacingAndNonbreakingSpace() {
        assertEquals("Deux  espaces :\u00A0texte.", QuotationNormalizer.fromParagraphs(
            listOf(listOf("Deux  espaces :\u00A0", "texte."))))
    }
    @Test fun manualContentsKeepsItsNewlineWhenBoundariesChange() {
        val manual = "Correction.\nSaut volontaire."
        val state = HighlightText.created("Old quotation").commitContentsEdit(manual)
        val next = state.boundariesChanged(QuotationNormalizer.fromParagraphs(listOf(listOf("New", "quotation"))))
        assertEquals(manual, next.contents)
        assertEquals(TextMode.MANUAL, next.mode)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnUnclassifiedEmbeddedNewline() {
        QuotationNormalizer.fromParagraphs(listOf(listOf("First\nSecond")))
    }
    @Test fun emptyGroupsDoNotInventParagraphs() {
        assertEquals("", QuotationNormalizer.fromParagraphs(listOf(emptyList(), listOf(" ", "\t"))))
    }
}
