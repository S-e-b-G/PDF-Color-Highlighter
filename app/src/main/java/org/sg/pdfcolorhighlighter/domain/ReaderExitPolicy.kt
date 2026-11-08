// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
enum class ReaderExitDecision { WAIT, CONFIRM, LEAVE }
object ReaderExitPolicy {
    fun decide(unsaved:Boolean,busy:Boolean):ReaderExitDecision =
        if(busy)ReaderExitDecision.WAIT else if(unsaved)ReaderExitDecision.CONFIRM else ReaderExitDecision.LEAVE
}
