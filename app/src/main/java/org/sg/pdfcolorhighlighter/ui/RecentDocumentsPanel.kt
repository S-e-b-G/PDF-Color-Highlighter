// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sg.pdfcolorhighlighter.R
import org.sg.pdfcolorhighlighter.domain.RecentDocument
import java.text.DateFormat
import java.util.Date

@Composable
fun RecentDocumentsPanel(
    entries: List<RecentDocument>,
    unavailableUri: String?,
    onBrowse: () -> Unit,
    onRecent: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Button(onClick = onBrowse) {
            Text(stringResource(R.string.reader_recents_browse))
        }
        Text(
            stringResource(R.string.reader_recents_title),
            Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        if (entries.isEmpty()) {
            Text(stringResource(R.string.reader_recents_empty))
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(items = entries, key = { it.uri }) { entry ->
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { onRecent(entry.uri) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    entry.displayName, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val date = DateFormat.getDateTimeInstance(
                                    DateFormat.SHORT, DateFormat.SHORT
                                ).format(Date(entry.lastOpenedMillis))
                                Text(
                                    stringResource(
                                        R.string.reader_recents_detail,
                                        entry.page + 1, entry.pageCount, date
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    entry.uri, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                if (unavailableUri == entry.uri) {
                                    Text(
                                        stringResource(R.string.reader_recents_unavailable),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                        val removeLabel = stringResource(
                            R.string.reader_recents_remove_accessibility,
                            entry.displayName,
                        )
                        TextButton(
                            onClick = { onRemove(entry.uri) },
                            modifier = Modifier.heightIn(min = 48.dp).semantics {
                                contentDescription = removeLabel
                            },
                        ) {
                            Text(stringResource(R.string.reader_recents_remove))
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
