// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.sg.pdfcolorhighlighter.R

private val helpSections=listOf(
    R.string.reader_help_open_title to R.string.reader_help_open_body,
    R.string.reader_help_read_title to R.string.reader_help_read_body,
    R.string.reader_help_highlight_title to R.string.reader_help_highlight_body,
    R.string.reader_help_edit_title to R.string.reader_help_edit_body,
    R.string.reader_help_quotation_title to R.string.reader_help_quotation_body,
    R.string.reader_help_notes_title to R.string.reader_help_notes_body,
    R.string.reader_help_history_title to R.string.reader_help_history_body,
    R.string.reader_help_repair_title to R.string.reader_help_repair_body,
    R.string.reader_help_palette_title to R.string.reader_help_palette_body,
    R.string.reader_help_save_title to R.string.reader_help_save_body,
    R.string.reader_help_export_title to R.string.reader_help_export_body,
    R.string.reader_help_status_title to R.string.reader_help_status_body,
    R.string.reader_help_recovery_title to R.string.reader_help_recovery_body,
    R.string.reader_help_versions_title to R.string.reader_help_versions_body,
    R.string.reader_help_properties_title to R.string.reader_help_properties_body,
    R.string.reader_help_diagnostics_title to R.string.reader_help_diagnostics_body,
    R.string.reader_help_limits_title to R.string.reader_help_limits_body,
    R.string.reader_help_help_title to R.string.reader_help_help_body,
)

/** Static offline guide only. No ViewModel, PDF, preferences or storage write callback. */
@Composable
fun ReaderHelpDialog(close:()->Unit){
    val scroll=rememberScrollState()
    val scope=rememberCoroutineScope()
    val area=stringResource(R.string.reader_help_scroll_area)
    AlertDialog(onDismissRequest=close,
        title={Column{
            Text(stringResource(R.string.reader_help_title))
            Text(stringResource(R.string.reader_help_version,stringResource(R.string.reader_increment)),style=MaterialTheme.typography.labelSmall)
        }},
        text={Column(Modifier.fillMaxWidth().heightIn(max=480.dp).verticalScroll(scroll).semantics{contentDescription=area}){
            Text(stringResource(R.string.reader_help_intro),style=MaterialTheme.typography.bodyMedium)
            helpSections.forEach{(title,body)->
                Spacer(Modifier.height(16.dp))
                Text(stringResource(title),Modifier.semantics{heading()},style=MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(body),style=MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.reader_help_end),style=MaterialTheme.typography.labelSmall)
        }},
        confirmButton={TextButton(onClick=close){Text(stringResource(R.string.reader_help_close))}},
        dismissButton={TextButton(onClick={scope.launch{scroll.animateScrollTo(0)}}){Text(stringResource(R.string.reader_help_top))}})
}
