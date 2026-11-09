// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.NotePolicy

@Composable
fun NoteContentsDialog(draft:String,onChange:(String)->Unit,busy:Boolean,error:String?,onConfirm:()->Unit,onCancel:()->Unit){
    val valid=NotePolicy.validText(draft)
    AlertDialog(onDismissRequest={if(!busy)onCancel()},
        title={Text(stringResource(R.string.reader_note_title))},
        text={Column{
            OutlinedTextField(value=draft,onValueChange=onChange,enabled=!busy,
                label={Text(stringResource(R.string.reader_note_contents))},
                modifier=Modifier.fillMaxWidth().heightIn(min=120.dp,max=320.dp),isError=!valid && draft.isNotEmpty())
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }},
        confirmButton={TextButton(onClick=onConfirm,enabled=valid && !busy){Text(stringResource(R.string.reader_note_confirm))}},
        dismissButton={TextButton(onClick=onCancel,enabled=!busy){Text(stringResource(R.string.reader_contents_cancel))}})
}
