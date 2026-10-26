// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.*
import java.util.Locale

internal fun selectionRejectionResource(reason:SelectionRejection?):Int=when(reason){
    SelectionRejection.DIFFERENT_TEXT_FLOW -> R.string.selection_diag_flow
    SelectionRejection.NO_GLYPH -> R.string.selection_diag_endpoint
    SelectionRejection.WHITESPACE -> R.string.selection_diag_whitespace
    SelectionRejection.UNSUPPORTED_TEXT -> R.string.selection_diag_unsupported
    SelectionRejection.TOO_COMPLEX -> R.string.selection_diag_complex
    SelectionRejection.COPY_RESTRICTED -> R.string.selection_preview_restricted
    SelectionRejection.NO_TEXT -> R.string.selection_preview_empty
    SelectionRejection.NO_GEOMETRY -> R.string.selection_diag_geometry
    null -> R.string.selection_diag_reason_none
}

@Composable
fun SelectionDiagnosticDialog(report:SelectionLayoutReport,onDismiss:()->Unit){
    val unknown=stringResource(R.string.selection_diag_unknown)
    val reason=stringResource(selectionRejectionResource(report.rejection))
    val header=stringResource(R.string.selection_diag_summary,stringResource(R.string.reader_increment),
        report.status?.name ?: unknown,reason,report.totalLines,report.totalGlyphs)
    fun number(value:Float)=String.format(Locale.ROOT,"%.2f",value)
    val page=report.pageBounds
    val bounds=if(page!=null)stringResource(R.string.selection_diag_bounds,number(page.left),number(page.top),number(page.right),number(page.bottom)) else unknown
    val yes=stringResource(R.string.selection_diag_yes);val no=stringResource(R.string.selection_diag_no)
    val lineTemplate=stringResource(R.string.selection_diag_line)
    val unicodeTemplate=stringResource(R.string.selection_diag_unicode)
    val exceptionalTemplate=stringResource(R.string.selection_diag_exceptional)
    val rows=report.lines.joinToString("\n"){line ->
        val b=line.bounds
        String.format(Locale.ROOT,lineTemplate,line.index,line.block,line.run,number(b.left),number(b.top),
            number(b.right),number(b.bottom),line.glyphCount,if(line.selectable)yes else no) + "\n" +
            String.format(Locale.ROOT,unicodeTemplate,line.unreliableGlyphs,line.unsupportedGlyphs,
                line.combiningMarks,line.spacingAccents,line.inferredClusterSplits,line.bulletGlyphs) + "\n" +
            String.format(Locale.ROOT,exceptionalTemplate,line.spacingPairs,line.exceptionalSymbols)
    }
    val limit=if(report.truncated)stringResource(R.string.selection_diag_truncated,report.lines.size,report.totalLines)else ""
    val warning=stringResource(R.string.selection_diag_notice)
    val body=listOf(header,bounds,warning,rows,limit).filter{it.isNotEmpty()}.joinToString("\n\n")
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.selection_diag_title))},
        text={SelectionContainer{Text(body,Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()))}},
        confirmButton={TextButton(onClick=onDismiss){Text(stringResource(R.string.reader_recents_close))}})
}
