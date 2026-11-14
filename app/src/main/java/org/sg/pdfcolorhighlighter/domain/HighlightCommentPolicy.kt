// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

enum class CommentSubmission { WAIT, INVALID, UNCHANGED, COMMIT }
object HighlightCommentPolicy {
    fun valid(draft:String)=draft.length<=100000
    fun openOnTap(selectedId:String?,hitId:String)=selectedId!=null && selectedId==hitId
    fun submission(draft:String,saved:String,busy:Boolean)=when {
        busy->CommentSubmission.WAIT
        !valid(draft)->CommentSubmission.INVALID
        draft==saved->CommentSubmission.UNCHANGED
        else->CommentSubmission.COMMIT
    }
    fun closeAfterResult(success:Boolean)=success
}
