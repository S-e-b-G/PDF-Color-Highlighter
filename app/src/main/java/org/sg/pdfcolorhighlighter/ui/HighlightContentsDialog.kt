// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.HighlightCommentPolicy

@Composable
fun HighlightContentsDialog(draft:String,onDraft:(String)->Unit,apply:(String)->Unit,busy:Boolean,error:String?,close:()->Unit){
    val valid=HighlightCommentPolicy.valid(draft)
    val submit:()->Unit={if(!busy && valid)apply(draft)}
    AlertDialog(onDismissRequest=submit,title={Text(stringResource(R.string.reader_contents_title))},
        text={Column{
            Text(stringResource(R.string.reader_contents_explanation))
            Text(stringResource(R.string.reader_comment_dismiss_hint))
            if(error!=null)Text(error,color=MaterialTheme.colorScheme.error)
            OutlinedTextField(value=draft,onValueChange=onDraft,enabled=!busy,
                modifier=Modifier.fillMaxWidth().heightIn(min=160.dp,max=360.dp),
                isError=!valid,label={Text(stringResource(R.string.reader_contents_label))})
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }},
        confirmButton={TextButton(onClick=submit,enabled=valid && !busy){Text(stringResource(R.string.reader_contents_apply))}},
        dismissButton={TextButton(onClick=close,enabled=!busy){Text(stringResource(R.string.reader_contents_cancel))}})
}
