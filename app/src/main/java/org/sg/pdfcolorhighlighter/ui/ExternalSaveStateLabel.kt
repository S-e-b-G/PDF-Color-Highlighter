// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.ExternalSaveState

fun externalSaveStateLabel(state:ExternalSaveState):Int=when(state){
    ExternalSaveState.NO_DOCUMENT->R.string.reader_saved_no_document
    ExternalSaveState.ORIGINAL_SAVED->R.string.reader_saved_original
    ExternalSaveState.COPY_SAVED->R.string.reader_saved_copy
    ExternalSaveState.UNSAVED->R.string.reader_saved_unsaved
    ExternalSaveState.RECOVERED->R.string.reader_saved_recovered
    ExternalSaveState.WRITE_FAILED->R.string.reader_saved_failed
    ExternalSaveState.UNKNOWN->R.string.reader_saved_unknown
}
