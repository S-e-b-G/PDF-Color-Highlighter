// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Presentation only. Does not alter save, exit, conflict or recovery decisions. */
enum class ExternalSaveState { NO_DOCUMENT, ORIGINAL_SAVED, COPY_SAVED, UNSAVED, RECOVERED, WRITE_FAILED, UNKNOWN }
object ExternalSavePresentation {
    private fun verified(hash:String?)=hash!=null && Regex("[0-9a-f]{64}").matches(hash)
    fun state(visible:Boolean,current:String?,sourceSaved:String?,exported:String?,detached:Boolean,writeFailed:Boolean):ExternalSaveState {
        if(!visible)return ExternalSaveState.NO_DOCUMENT
        if(!verified(current))return ExternalSaveState.UNKNOWN
        if(writeFailed)return ExternalSaveState.WRITE_FAILED
        if(!detached && verified(sourceSaved) && current==sourceSaved)return ExternalSaveState.ORIGINAL_SAVED
        if(verified(exported) && current==exported)return ExternalSaveState.COPY_SAVED
        if(detached)return ExternalSaveState.RECOVERED
        return ExternalSaveState.UNSAVED
    }
}
