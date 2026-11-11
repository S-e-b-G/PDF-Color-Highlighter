// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.domain.ExternalSaveState

/** Read observable presentation inputs in the composable that paints the label.
 * The provider must read snapshot state, not retain a previously evaluated enum.
 * No PDF work, hash computation or save mutation belongs in this composable.
 */
@Composable
fun ExternalSaveStatus(currentState:()->ExternalSaveState){
    val current=currentState()
    if(current==ExternalSaveState.NO_DOCUMENT)return
    val warning=current in setOf(ExternalSaveState.UNSAVED,ExternalSaveState.RECOVERED,
        ExternalSaveState.WRITE_FAILED,ExternalSaveState.UNKNOWN)
    Text(stringResource(externalSaveStateLabel(current)),
        Modifier.widthIn(max=280.dp).padding(start=8.dp).semantics{stateDescription=current.name},
        style=MaterialTheme.typography.labelSmall,
        color=if(warning)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines=1,overflow=TextOverflow.Ellipsis)
}
