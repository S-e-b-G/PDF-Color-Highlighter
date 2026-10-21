// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.DocumentProperties
import org.sg.pdfcolorhighlighter.domain.PdfDateParser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun DocumentPropertiesDialog(info: DocumentProperties, onDismiss: () -> Unit) {
    val sourceDate = info.sourceModifiedMillis?.let {
        try {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX", Locale.ROOT)
                .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
        } catch (_: Exception) { it.toString() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_properties_title)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PropertyRow(R.string.reader_property_name, info.displayName)
                PropertyRow(R.string.reader_property_title, info.title)
                PropertyRow(R.string.reader_property_author, info.author)
                PropertyRow(R.string.reader_property_pages, info.pageCount.toString())
                PropertyRow(R.string.reader_property_size, pluralStringResource(R.plurals.reader_property_bytes, info.sizeBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), info.sizeBytes))
                PropertyRow(R.string.reader_property_format, info.format)
                PropertyRow(R.string.reader_property_created, formatPdfDate(info.creationDate))
                PropertyRow(R.string.reader_property_modified, formatPdfDate(info.modificationDate))
                PropertyRow(R.string.reader_property_source_modified, sourceDate)
                PropertyRow(R.string.reader_property_creator, info.creator)
                PropertyRow(R.string.reader_property_producer, info.producer)
                PropertyRow(R.string.reader_property_subject, info.subject)
                PropertyRow(R.string.reader_property_keywords, info.keywords)
                PropertyRow(R.string.reader_property_location, info.sourceUri)
                Text(stringResource(R.string.reader_properties_snapshot), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reader_properties_close)) } },
    )
}

@Composable
private fun PropertyRow(label: Int, value: String?) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.reader_property_unknown),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun formatPdfDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val value = PdfDateParser.parse(raw) ?: return raw
    fun number(n: Int, length: Int = 2) = n.toString().padStart(length, '0')
    val date = when {
        value.day != null -> "${number(value.day)}/${number(value.month!!)}/${number(value.year, 4)}"
        value.month != null -> "${number(value.month)}/${number(value.year, 4)}"
        else -> number(value.year, 4)
    }
    val time = listOfNotNull(value.hour, value.minute, value.second).joinToString(":") { number(it) }
    val zone = value.offsetMinutes?.let {
        val sign = if (it < 0) "-" else "+"
        if (it == 0) "UTC" else "UTC$sign${number(abs(it) / 60)}:${number(abs(it) % 60)}"
    } ?: if (time.isNotEmpty()) stringResource(R.string.reader_property_timezone_unknown) else null
    return listOfNotNull(date, time.takeIf { it.isNotEmpty() }, zone?.let { "($it)" }).joinToString(" ")
}
