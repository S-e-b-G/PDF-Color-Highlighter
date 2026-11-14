// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.AnnotationPalette

@Composable
fun PaletteSettingsDialog(draft:List<String>,busy:Boolean,warning:Boolean,onDraft:(Int,String)->Unit,
    reset:(Int)->Unit,resetAll:()->Unit,apply:()->Unit,close:()->Unit){
    val parsed=draft.map{AnnotationPalette.parse(it)}
    val labels=listOf(R.string.reader_slot_1,R.string.reader_slot_2,R.string.reader_slot_3,
        R.string.reader_slot_4,R.string.reader_slot_5,R.string.reader_slot_6)
    AlertDialog(onDismissRequest={if(!busy)close()},title={Text(stringResource(R.string.reader_settings))},
        text={Column(Modifier.fillMaxWidth().heightIn(max=480.dp).verticalScroll(rememberScrollState())){
            Text(stringResource(R.string.palette_explanation))
            if(warning)Text(stringResource(R.string.palette_storage_error),color=MaterialTheme.colorScheme.error)
            draft.forEachIndexed{i,value->
                val rgb=parsed[i];val duplicate=rgb!=null && parsed.count{it==rgb}>1
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(28.dp).background(if(rgb!=null)Color(0xff000000.toInt() or rgb)else Color.Transparent).border(1.dp,Color.DarkGray))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value=value,onValueChange={onDraft(i,it)},enabled=!busy,
                        modifier=Modifier.weight(1f),singleLine=true,
                        label={Text(stringResource(labels[i]))},isError=rgb==null || duplicate,
                        supportingText={Text(stringResource(if(duplicate)R.string.palette_duplicate else R.string.palette_hex_hint))})
                    TextButton(onClick={reset(i)},enabled=!busy){Text(stringResource(R.string.palette_reset_slot))}
                }
            }
            TextButton(onClick=resetAll,enabled=!busy){Text(stringResource(R.string.palette_reset_all))}
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }},
        confirmButton={TextButton(onClick=apply,enabled=!busy && AnnotationPalette.draft(draft)!=null){Text(stringResource(R.string.palette_apply))}},
        dismissButton={TextButton(onClick=close,enabled=!busy){Text(stringResource(R.string.reader_contents_cancel))}})
}
