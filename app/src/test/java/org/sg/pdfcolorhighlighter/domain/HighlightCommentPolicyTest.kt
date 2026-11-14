// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import org.junit.Assert.*
import org.junit.Test

class HighlightCommentPolicyTest {
    @Test fun firstTapOnlySelects(){assertFalse(HighlightCommentPolicy.openOnTap(null,"a"));assertFalse(HighlightCommentPolicy.openOnTap("a","b"))}
    @Test fun secondTapOpens(){assertTrue(HighlightCommentPolicy.openOnTap("a","a"))}
    @Test fun unchangedClosesWithoutCommit(){assertEquals(CommentSubmission.UNCHANGED,HighlightCommentPolicy.submission("same","same",false))}
    @Test fun emptyManualContentsAreValid(){assertEquals(CommentSubmission.COMMIT,HighlightCommentPolicy.submission("","old",false))}
    @Test fun oversizedDraftIsRejected(){assertEquals(CommentSubmission.INVALID,HighlightCommentPolicy.submission("x".repeat(100001),"old",false))}
    @Test fun busyRejectsDuplicateSubmission(){assertEquals(CommentSubmission.WAIT,HighlightCommentPolicy.submission("new","old",true))}
    @Test fun failureKeepsEditor(){assertFalse(HighlightCommentPolicy.closeAfterResult(false))}
    @Test fun successClosesEditor(){assertTrue(HighlightCommentPolicy.closeAfterResult(true))}
}
