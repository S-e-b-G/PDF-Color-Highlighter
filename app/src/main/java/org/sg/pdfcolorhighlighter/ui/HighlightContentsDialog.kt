// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.R

@Composable
fun HighlightContentsDialog(draft:String,onDraft:(String)->Unit,apply:(String)->Unit,close:()->Unit){
    AlertDialog(onDismissRequest=close,title={Text(stringResource(R.string.reader_contents_title))},
        text={Column{Text(stringResource(R.string.reader_contents_explanation))
            OutlinedTextField(value=draft,onValueChange=onDraft,modifier=Modifier.fillMaxWidth().heightIn(min=160.dp,max=360.dp),
                isError=draft.length>100000,label={Text(stringResource(R.string.reader_contents_label))})}},
        confirmButton={TextButton(onClick={apply(draft);close()},enabled=draft.length<=100000){Text(stringResource(R.string.reader_contents_apply))}},
        dismissButton={TextButton(onClick=close){Text(stringResource(R.string.reader_contents_cancel))}})
}
