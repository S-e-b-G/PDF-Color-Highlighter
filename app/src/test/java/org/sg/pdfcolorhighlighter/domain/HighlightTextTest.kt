// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class HighlightTextTest {
    @Test fun newHighlightCopiesQuotation() {
        val state = HighlightText.created("Selected text")
        assertEquals(state.extractedQuotation, state.contents)
        assertEquals(TextMode.AUTOMATIC, state.mode)
    }
    @Test fun automaticContentsFollowBoundaries() {
        assertEquals("Expanded text",
            HighlightText.created("Text").boundariesChanged("Expanded text").contents)
    }
    @Test fun editedContentsSurviveBoundaryChange() {
        val state = HighlightText.created("Bad extraction")
            .commitContentsEdit("Corrected extraction").boundariesChanged("New extraction")
        assertEquals("Corrected extraction", state.contents)
        assertEquals("New extraction", state.extractedQuotation)
        assertEquals(TextMode.MANUAL, state.mode)
    }
    @Test fun unchangedDraftIsNotAnEdit() {
        val state = HighlightText.created("Text")
        assertSame(state, state.commitContentsEdit("Text"))
    }
    @Test fun importedContentsArePreserved() {
        assertEquals("Existing comment",
            HighlightText.imported("Existing comment").boundariesChanged("Text").contents)
    }
    @Test fun explicitReplacementReturnsToAutomatic() {
        val state = HighlightText.created("Text").commitContentsEdit("Correction")
            .boundariesChanged("New text").replaceWithExtractedQuotation()
        assertEquals("New text", state.contents)
        assertEquals(TextMode.AUTOMATIC, state.mode)
    }
    @Test fun contentsAreNotTrimmedOrCombinedWithNotes() {
        assertEquals("  Corrected text\n", HighlightText.created("Text")
            .commitContentsEdit("  Corrected text\n").contents)
    }
}
