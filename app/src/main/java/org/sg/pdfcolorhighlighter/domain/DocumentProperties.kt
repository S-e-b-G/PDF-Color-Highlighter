// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Immutable facts for the opened PDF snapshot. Dates from PDF and source provider stay distinct. */
data class DocumentProperties(
    val displayName: String?, val sourceUri: String, val sizeBytes: Long, val pageCount: Int,
    val title: String?, val author: String?, val subject: String?, val keywords: String?,
    val creator: String?, val producer: String?, val format: String?,
    val creationDate: String?, val modificationDate: String?, val sourceModifiedMillis: Long?,
)
