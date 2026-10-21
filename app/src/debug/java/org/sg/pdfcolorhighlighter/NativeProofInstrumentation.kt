// M1.1 quotation normalization and PDF text-string encoding.
// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFObject
import com.artifex.mupdf.fitz.PDFPage
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.StructuredText
import org.sg.pdfcolorhighlighter.domain.HighlightText
import org.sg.pdfcolorhighlighter.domain.TextMode
import org.sg.pdfcolorhighlighter.domain.QuotationNormalizer
import org.sg.pdfcolorhighlighter.domain.PdfTextString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Debug-only, deterministic native interoperability proof.
 *
 * This is not the production annotation editor, save pipeline,
 * history implementation or recovery system.
 */
class NativeProofInstrumentation : Instrumentation() {

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val directory = File(
            targetContext.filesDir,
            "interop-proof/${UUID.randomUUID()}",
        )

        try {
            check(directory.mkdirs()) {
                "Cannot create proof directory"
            }

            val proof = NativeProof(directory)
            proof.run()

            finish(
                Activity.RESULT_OK,
                Bundle().apply {
                    putString("result", "PASS: implemented native assertions")
                    putString("runDirectory", directory.absolutePath)
                    putString(
                        "remaining",
                        "Visual/editability, Docear and Obsidian tests pending",
                    )
                },
            )
        } catch (error: Exception) {
            reportFailure(directory, error)
        } catch (error: LinkageError) {
            reportFailure(directory, error)
        }
    }

    private fun reportFailure(directory: File, error: Throwable) {
        val diagnostic =
            (error as? ProofFailure)?.checkId
                ?: error.javaClass.simpleName

        try {
            directory.mkdirs()
            File(directory, "failure.txt").writeText(
                "Result: FAILED\nDiagnostic: $diagnostic\n\n" + error.stackTraceToString(),
                Charsets.UTF_8,
            )
        } catch (_: Exception) {
            // The instrumentation result still reports the failure.
        }

        finish(
            Activity.RESULT_CANCELED,
            Bundle().apply {
                putString("result", "FAIL")
                putString("diagnostic", diagnostic)
                putString("runDirectory", directory.absolutePath)
            },
        )
    }
}

private class ProofFailure(val checkId: String) : Exception(checkId)

private class NativeProof(private val directory: File) {

    private val lines = listOf(
        "SG01 Bonjour : élève, été, cœur.",
        "SG02 Première ligne de la citation.",
        "SG03 Deuxième ligne : français et naïveté.",
        "SG04 Extension automatique de la citation.",
        "SG05 Limite ajustée après correction manuelle.",
        "SG06 Important : vérifier les accents.",
        "SG07 Notes : une citation indépendante.",
        "SG08 Sous-notes : géométrie du texte.",
        "SG09 Superflu : conserver les couleurs.",
        "SG10 Cette phrase continue sur la ligne suivante parce que",
        "la largeur disponible ne permet pas de tout afficher sur une seule ligne.",
        "SG12 Premier paragraphe : texte indépendant.",
        "SG13 Second paragraphe : séparation conservée.",
    )

    private val wrappedCorrection = "Correction volontaire.\nDeuxième ligne conservée."
    private val wrappedId = "sg-proof-wrapped"
    private val paragraphsId = "sg-proof-paragraphs"

    private val palette = intArrayOf(
        0xFFAD5B,
        0x00FF00,
        0xFC3636,
        0xFFFF00,
        0xD1DFEB,
        0xFCD7FB,
    )

    private val correction =
        "Correction manuelle : élève, été, cœur — à conserver."

    private val noteContents =
        "Note indépendante : commentaire ajouté, français et naïveté."

    private val prefix = "sg-proof-"
    private val modeKey = "SGCHTextMode"
    private val quotationKey = "SGCHQuotation"
    private val syncedKey = "SGCHSyncedContents"

    private val passedChecks = mutableListOf<String>()
    private var document: PDFDocument? = null

    fun run() {
        val input = File(directory, "00-input.pdf")
        createFixture(input)
        val inputHash = sha256(input)

        try {
            document = open(input)
            val baseline = preservedDocumentFields()

            createAnnotations()
            checkpoint("01-created", baseline)
            verifyStage(1)

            recolorAndExtend()
            checkpoint("02-automatic", baseline)
            verifyStage(2)

            correctExtendAndDelete()
            checkpoint("03-manual-deleted", baseline)
            verifyStage(3)

            checkpoint("04-resaved", baseline)
            verifyStage(3)

            checkpoint("05-resaved", baseline)
            verifyStage(3)

            ensure(
                sha256(input) == inputHash,
                "input-fixture-bytes-unchanged",
            )

            File(directory, "report.txt").writeText(
                buildString {
                    appendLine("Native PDF annotation proof - M1.1")
                    appendLine("MuPDF dependency: 1.28.5")
                    appendLine("Result: PASS for implemented assertions")
                    appendLine(
                        "Android API: ${android.os.Build.VERSION.SDK_INT}",
                    )
                    appendLine(
                        "Supported ABIs: " +
                            android.os.Build.SUPPORTED_ABIS.joinToString(),
                    )
                    appendLine(
                        "Supported ABI list does not identify process ABI.",
                    )

                    directory.listFiles()
                        .orEmpty()
                        .filter { it.extension == "pdf" }
                        .sortedBy { it.name }
                        .forEach {
                            appendLine("${it.name} SHA256: ${sha256(it)}")
                        }

                    passedChecks.forEach {
                        appendLine("PASS $it")
                    }

                    appendLine()
                    appendLine("NOT TESTED by this proof:")
                    appendLine("- External-reader appearance/editability")
                    appendLine("- Docear import")
                    appendLine("- Obsidian extraction")
                    appendLine("- General touch selection and handles")
                    appendLine("- Production undo/redo")
                    appendLine("- Autosave and durable recovery")
                    appendLine("- Arbitrary, encrypted or signed PDFs")
                    appendLine("- Full preservation of every PDF feature")
                    appendLine()
                    appendLine(
                        "Highlight opacity 0.5 is provisional for this proof.",
                    )
                },
                Charsets.UTF_8,
            )
        } finally {
            document?.destroy()
            document = null
        }
    }

    private fun pdf(): PDFDocument = checkNotNull(document)

    private fun ensure(condition: Boolean, checkId: String) {
        if (!condition) throw ProofFailure(checkId)
        passedChecks.add(checkId)
    }

    private fun open(file: File): PDFDocument {
        val opened = Document.openDocument(file.absolutePath)

        if (opened !is PDFDocument) {
            opened.destroy()
            throw ProofFailure("opened-document-is-pdf")
        }

        try {
            ensure(!opened.needsPassword(), "fixture-no-password")
            ensure(opened.isUnencryptedPDF(), "fixture-unencrypted")
            ensure(opened.countSignatures() == 0, "fixture-unsigned")
            ensure(opened.countPages() == 2, "fixture-two-pages")
            ensure(
                opened.hasPermission(Document.PERMISSION_COPY) &&
                    opened.hasPermission(Document.PERMISSION_ANNOTATE),
                "fixture-copy-and-annotation-permissions",
            )
            return opened
        } catch (error: Exception) {
            opened.destroy()
            throw error
        }
    }

    private inline fun <T> onPage(
        index: Int,
        action: (PDFPage) -> T,
    ): T {
        val page = pdf().loadPage(index)

        try {
            return action(page as PDFPage)
        } finally {
            page.destroy()
        }
    }

    private inline fun <T> onAnnotation(
        page: PDFPage,
        id: String,
        action: (PDFAnnotation) -> T,
    ): T {
        val annotations = page.annotations ?: emptyArray()

        try {
            val matches = annotations.filter {
                readString(it, "NM") == id
            }

            ensure(matches.size == 1, "unique-identity-$id")
            return action(matches.single())
        } finally {
            annotations.forEach { it.destroy() }
        }
    }

    private fun destroyObject(value: PDFObject) {
        if (!value.isNull) value.destroy()
    }

    private fun isAppTextKey(key: String) = key == quotationKey || key == syncedKey

    private fun readString(annotation: PDFAnnotation, key: String): String {
        val obj = annotation.`object`
        try {
            val value = obj.get(key)
            try {
                if (!value.isString) return ""
                return if (isAppTextKey(key)) {
                    PdfTextString.decodeAppText(value.asByteString())
                } else value.asString()
            } finally { destroyObject(value) }
        } finally { destroyObject(obj) }
    }

    private fun writeString(annotation: PDFAnnotation, key: String, value: String) {
        val obj = annotation.`object`
        try {
            if (isAppTextKey(key)) {
                val encoded = pdf().newByteString(PdfTextString.encode(value))
                try { obj.put(key, encoded) } finally { destroyObject(encoded) }
            } else obj.put(key, value)
        } finally { destroyObject(obj) }
    }

    private fun verifyTextEncoding(annotation: PDFAnnotation, key: String) {
        val obj = annotation.`object`
        try {
            val value = obj.get(key)
            try {
                ensure(value.isString, "metadata-$key-is-string")
                val bytes = value.asByteString()
                ensure(bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte(),
                    "metadata-$key-utf16be-marker")
            } finally { destroyObject(value) }
        } finally { destroyObject(obj) }
    }

    private fun storeText(
        annotation: PDFAnnotation,
        state: HighlightText,
    ) {
        annotation.setContents(state.contents)
        writeString(annotation, modeKey, state.mode.name)
        writeString(annotation, quotationKey, state.extractedQuotation)
        writeString(annotation, syncedKey, state.contents)
    }

    private fun loadText(annotation: PDFAnnotation): HighlightText {
        val contents = annotation.contents ?: ""

        val automatic =
            readString(annotation, modeKey) == TextMode.AUTOMATIC.name &&
                readString(annotation, syncedKey) == contents

        return HighlightText(
            extractedQuotation = readString(annotation, quotationKey),
            contents = contents,
            mode = if (automatic) TextMode.AUTOMATIC else TextMode.MANUAL,
        )
    }

    private data class Selection(
        val quads: Array<Quad>,
        val quotation: String,
    )

    private fun selection(
        page: PDFPage,
        lineIndices: List<Int>,
        paragraphStarts: Set<Int> = emptySet(),
    ): Selection {
        ensure(lineIndices.isNotEmpty(), "nonempty-selection")
        val text = page.toStructuredText()
        try {
            val actualLines = text.blocks.flatMap { block ->
                block.lines.map { line ->
                    buildString { line.chars.forEach { appendCodePoint(it.c) } }
                }
            }
            val quads = mutableListOf<Quad>()
            val paragraphs = mutableListOf<MutableList<String>>()
            for (index in lineIndices) {
                val matches = actualLines.filter { it == lines[index] }
                ensure(matches.size == 1, "actual-text-line-$index")
                val actual = matches.single()
                // StructuredText.search is intentionally not used: 1.28.5 JNI defect.
                val hits = page.search(actual, StructuredText.SEARCH_EXACT)
                ensure(hits.size == 1 && hits.single().isNotEmpty(), "actual-search-geometry-$index")
                quads.addAll(hits.single())
                if (paragraphs.isEmpty() || index in paragraphStarts) paragraphs.add(mutableListOf())
                paragraphs.last().add(actual)
            }
            ensure(quads.all { it.isValid && !it.isInfinite }, "valid-selection-quads")
            return Selection(quads.toTypedArray(), QuotationNormalizer.fromParagraphs(paragraphs))
        } finally { text.destroy() }
    }

    private fun rgb(hex: Int) = floatArrayOf(
        ((hex shr 16) and 255) / 255f,
        ((hex shr 8) and 255) / 255f,
        (hex and 255) / 255f,
    )

    private fun highlightId(slot: Int) = "${prefix}highlight-$slot"

    private fun createHighlight(
        page: PDFPage,
        id: String,
        indices: List<Int>,
        color: Int,
        paragraphStarts: Set<Int> = emptySet(),
    ) {
        val selected = selection(page, indices, paragraphStarts)
        val annotation = page.createAnnotation(PDFAnnotation.TYPE_HIGHLIGHT)
        try {
            writeString(annotation, "NM", id)
            annotation.setQuadPoints(selected.quads)
            annotation.setColor(rgb(color))
            annotation.setOpacity(0.5f)
            annotation.setFlags(annotation.flags or PDFAnnotation.IS_PRINT)
            storeText(annotation, HighlightText.created(selected.quotation))
            annotation.update()
        } finally { annotation.destroy() }
    }

    private fun createAnnotations() {
        onPage(0) { page ->
            createHighlight(page, highlightId(1), listOf(0), palette[0])
            createHighlight(page, highlightId(2), listOf(1, 2), palette[1])
            createHighlight(page, "${prefix}temporary", listOf(3), palette[2])
            val note = page.createAnnotation(PDFAnnotation.TYPE_TEXT)
            try {
                writeString(note, "NM", "${prefix}note")
                note.setRect(Rect(480f, 290f, 504f, 314f))
                note.setIcon("Note")
                note.setColor(rgb(palette[3]))
                note.setContents(noteContents)
                note.setFlags(note.flags or PDFAnnotation.IS_PRINT)
                note.update()
            } finally { note.destroy() }
            page.update()
        }
        onPage(1) { page ->
            for (slot in 3..6) {
                createHighlight(page, highlightId(slot), listOf(slot + 2), palette[slot - 1])
            }
            createHighlight(page, wrappedId, listOf(9, 10), palette[0])
            createHighlight(page, paragraphsId, listOf(11, 12), palette[4], setOf(12))
            page.update()
        }
    }

    private fun changeBoundaries(
        page: PDFPage,
        annotation: PDFAnnotation,
        indices: List<Int>,
        paragraphStarts: Set<Int> = emptySet(),
    ) {
        val selected = selection(page, indices, paragraphStarts)
        val next = loadText(annotation).boundariesChanged(selected.quotation)
        annotation.setQuadPoints(selected.quads)
        storeText(annotation, next)
        annotation.update()
    }

    private fun recolorAndExtend() {
        onPage(0) { page ->
            onAnnotation(page, highlightId(1)) {
                it.setColor(rgb(palette[2]))
                it.update()
            }

            onAnnotation(page, highlightId(2)) {
                changeBoundaries(page, it, listOf(1, 2, 3))
            }

            page.update()
        }
    }

    private fun correctExtendAndDelete() {
        onPage(0) { page ->
            onAnnotation(page, highlightId(2)) {
                storeText(it, loadText(it).commitContentsEdit(correction))
                changeBoundaries(page, it, listOf(1, 2, 3, 4))
                ensure(it.contents == correction, "manual-correction-survives-boundary-change")
            }
            onAnnotation(page, "${prefix}temporary") { page.deleteAnnotation(it) }
            page.update()
        }
        onPage(1) { page ->
            onAnnotation(page, wrappedId) {
                storeText(it, loadText(it).commitContentsEdit(wrappedCorrection))
                changeBoundaries(page, it, listOf(9, 10, 11), setOf(11))
                ensure(it.contents == wrappedCorrection, "manual-newline-survives-boundary-change")
            }
            page.update()
        }
    }

    private fun rounded(value: Float) =
        (value * 1000f).roundToInt()

    private fun quadValues(quad: Quad) = listOf(
        quad.ul_x, quad.ul_y,
        quad.ur_x, quad.ur_y,
        quad.ll_x, quad.ll_y,
        quad.lr_x, quad.lr_y,
    ).map(::rounded)

    private data class AnnotationState(
        val page: Int,
        val id: String,
        val type: Int,
        val contents: String,
        val color: List<Int>,
        val opacity: Int,
        val rect: List<Int>,
        val quads: List<List<Int>>,
        val mode: String,
        val quotation: String,
    )

    private fun annotations(): List<AnnotationState> = buildList {
        for (index in 0 until pdf().countPages()) {
            onPage(index) { page ->
                val values = page.annotations ?: emptyArray()

                try {
                    for (annotation in values) {
                        val rect = annotation.bounds

                        add(
                            AnnotationState(
                                page = index,
                                id = readString(annotation, "NM"),
                                type = annotation.type,
                                contents = annotation.contents ?: "",
                                color = annotation.color.map(::rounded),
                                opacity = rounded(annotation.opacity),
                                rect = listOf(
                                    rect.x0, rect.y0,
                                    rect.x1, rect.y1,
                                ).map(::rounded),
                                quads = if (annotation.hasQuadPoints()) {
                                    annotation.quadPoints.map(::quadValues)
                                } else {
                                    emptyList()
                                },
                                mode = readString(annotation, modeKey),
                                quotation = readString(
                                    annotation, quotationKey,
                                ),
                            ),
                        )
                    }
                } finally {
                    values.forEach { it.destroy() }
                }
            }
        }
    }.sortedWith(compareBy({ it.page }, { it.id }, { it.type }))

    private fun preservedDocumentFields(): List<String> = buildList {
        add(pdf().countPages().toString())
        add(pdf().getMetaData(Document.META_INFO_TITLE) ?: "")
        add(pdf().getMetaData(Document.META_INFO_AUTHOR) ?: "")

        for (index in 0 until pdf().countPages()) {
            onPage(index) { page ->
                add(page.bounds.toString())

                val text = page.toStructuredText()
                try {
                    add(text.asText())
                } finally {
                    text.destroy()
                }
            }
        }

        addAll(
            annotations()
                .filter { it.id.startsWith("sg-existing-") }
                .map { it.toString() },
        )
    }

    private fun checkpoint(
        name: String,
        baseline: List<String>,
    ) {
        val before = annotations()
        val output = File(directory, "$name.pdf")

        ensure(!output.exists(), "fresh-output-$name")

        pdf().save(output.absolutePath, "compress")

        FileOutputStream(output, true).use {
            it.fd.sync()
        }

        val previous = document
        document = null
        previous?.destroy()

        document = open(output)

        ensure(
            annotations() == before,
            "annotation-save-reopen-$name",
        )

        ensure(
            preservedDocumentFields() == baseline,
            "preserved-fixture-subset-$name",
        )
    }

    private fun verifyStage(stage: Int) {
        val own = annotations().filter { it.id.startsWith(prefix) }
        ensure(own.size == if (stage < 3) 10 else 9, "stage-$stage-annotation-count")
        ensure(own.map { it.id }.toSet().size == own.size, "stage-$stage-no-duplicate-identities")
        for (slot in 1..6) {
            val annotation = own.single { it.id == highlightId(slot) }
            ensure(annotation.type == PDFAnnotation.TYPE_HIGHLIGHT && annotation.quads.isNotEmpty(),
                "stage-$stage-highlight-$slot-native")
            val color = if (slot == 1 && stage >= 2) palette[2] else palette[slot - 1]
            val expectedColor = rgb(color).map(::rounded)
            ensure(annotation.color.size == 3 &&
                annotation.color.zip(expectedColor).all { (a, b) -> abs(a - b) <= 1 } &&
                abs(annotation.opacity - 500) <= 1, "stage-$stage-highlight-$slot-color-opacity")
            val indices = when (slot) {
                1 -> listOf(0)
                2 -> when (stage) { 1 -> listOf(1, 2); 2 -> listOf(1, 2, 3); else -> listOf(1, 2, 3, 4) }
                else -> listOf(slot + 2)
            }
            val quotation = QuotationNormalizer.fromParagraphs(listOf(indices.map { lines[it] }))
            val manual = slot == 2 && stage >= 3
            ensure(annotation.quotation == quotation, "stage-$stage-highlight-$slot-quotation")
            ensure(annotation.contents == if (manual) correction else quotation,
                "stage-$stage-highlight-$slot-contents")
            ensure(annotation.mode == if (manual) "MANUAL" else "AUTOMATIC",
                "stage-$stage-highlight-$slot-text-mode")
        }
        val note = own.single { it.id == "${prefix}note" }
        ensure(note.type == PDFAnnotation.TYPE_TEXT && note.contents == noteContents,
            "stage-$stage-independent-native-note")

        val wrapped = own.single { it.id == wrappedId }
        val wrapQuote = QuotationNormalizer.fromParagraphs(listOf(listOf(lines[9], lines[10])))
        if (stage < 3) {
            ensure(wrapped.contents == wrapQuote && '\n' !in wrapped.contents && '\r' !in wrapped.contents,
                "stage-$stage-soft-wrap-no-stored-linebreak")
            ensure(wrapped.mode == "AUTOMATIC" && wrapped.quotation == wrapQuote && wrapped.quads.size == 2,
                "stage-$stage-soft-wrap-two-text-lines")
        } else {
            ensure(wrapped.contents == wrappedCorrection && wrapped.mode == "MANUAL" && wrapped.quads.size == 3,
                "stage-$stage-manual-newline-and-boundary-preserved")
            val extended = QuotationNormalizer.fromParagraphs(listOf(listOf(lines[9], lines[10]), listOf(lines[11])))
            ensure(wrapped.quotation == extended, "stage-$stage-manual-highlight-normalized-quotation")
        }
        val paragraphs = own.single { it.id == paragraphsId }
        ensure(paragraphs.contents == "${lines[11]}\n\n${lines[12]}" &&
            paragraphs.mode == "AUTOMATIC" && paragraphs.quads.size == 2,
            "stage-$stage-explicit-paragraph-boundary-preserved")
        for (entry in listOf(wrapped, paragraphs)) {
            val slotColor = if (entry.id == wrappedId) palette[0] else palette[4]
            ensure(entry.type == PDFAnnotation.TYPE_HIGHLIGHT && entry.color.size == 3 &&
                entry.color.zip(rgb(slotColor).map(::rounded)).all { (a, b) -> abs(a - b) <= 1 } &&
                abs(entry.opacity - 500) <= 1, "stage-$stage-${entry.id}-native-color")
        }
        for (entry in own.filter { it.type == PDFAnnotation.TYPE_HIGHLIGHT }) {
            onPage(entry.page) { page ->
                onAnnotation(page, entry.id) {
                    verifyTextEncoding(it, quotationKey)
                    verifyTextEncoding(it, syncedKey)
                    ensure(readString(it, syncedKey) == it.contents,
                        "stage-$stage-${entry.id}-synced-text")
                }
            }
        }
    }

    /**
     * Creates only a deterministic test fixture.
     * This is not an alternative document-processing engine.
     */
    private fun createFixture(output: File) {
        fun textHex(value: String): String =
            value.map { character ->
                val code = when (character) {
                    'œ' -> 0x9C
                    'Œ' -> 0x8C
                    else -> {
                        require(character.code <= 255)
                        character.code
                    }
                }
                code.toString(16).padStart(2, '0')
            }.joinToString("")

        fun pageStream(pageLines: List<String>): String =
            buildString {
                append("BT\n/F1 12 Tf\n48 710 Td\n")
                pageLines.forEachIndexed { index, value ->
                    if (index > 0) append("0 -30 Td\n")
                    append("<${textHex(value)}> Tj\n")
                }
                append("ET\n")
            }

        fun streamObject(value: String): String {
            val length = value.toByteArray(Charsets.US_ASCII).size
            return "<< /Length $length >>\nstream\n${value}endstream"
        }

        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R /Outlines 10 0 R >>",
            "<< /Type /Pages /Kids [3 0 R 5 0 R] /Count 2 >>",
            """
            << /Type /Page /Parent 2 0 R
               /MediaBox [0 0 595 842]
               /Resources << /Font << /F1 7 0 R >> >>
               /Contents 4 0 R /Annots [8 0 R 9 0 R] >>
            """.trimIndent(),
            streamObject(pageStream(lines.take(5))),
            """
            << /Type /Page /Parent 2 0 R
               /MediaBox [0 0 595 842]
               /Resources << /Font << /F1 7 0 R >> >>
               /Contents 6 0 R /Annots [12 0 R] >>
            """.trimIndent(),
            streamObject(pageStream(lines.drop(5))),
            """
            << /Type /Font /Subtype /Type1
               /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>
            """.trimIndent(),
            """
            << /Type /Annot /Subtype /Text
               /Rect [460 720 484 744]
               /Contents (Existing note)
               /NM (sg-existing-note)
               /Name /Note /C [0.2 0.3 0.8] /F 4 >>
            """.trimIndent(),
            """
            << /Type /Annot /Subtype /Square
               /Rect [450 560 500 600]
               /Contents (Existing square)
               /NM (sg-existing-square)
               /C [0.2 0.3 0.8] /F 4 >>
            """.trimIndent(),
            "<< /Type /Outlines /First 11 0 R /Last 11 0 R /Count 1 >>",
            """
            << /Title (First page) /Parent 10 0 R
               /Dest [3 0 R /Fit] >>
            """.trimIndent(),
            """
            << /Type /Annot /Subtype /Link
               /Rect [48 50 220 70]
               /A << /S /URI /URI (https://example.org/) >> >>
            """.trimIndent(),
            """
            << /Title (SG synthetic native annotation fixture)
               /Author (SG synthetic test) >>
            """.trimIndent(),
        )

        val bytes = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()

        fun append(value: String) {
            bytes.write(value.toByteArray(Charsets.US_ASCII))
        }

        append("%PDF-1.7\n")

        objects.forEachIndexed { index, value ->
            offsets.add(bytes.size())
            append("${index + 1} 0 obj\n$value\nendobj\n")
        }

        val xref = bytes.size()
        append("xref\n0 ${objects.size + 1}\n")
        append("0000000000 65535 f \n")

        offsets.forEach {
            append("${it.toString().padStart(10, '0')} 00000 n \n")
        }

        append(
            "trailer\n<< /Size ${objects.size + 1} " +
                "/Root 1 0 R /Info 13 0 R >>\n" +
                "startxref\n$xref\n%%EOF\n",
        )

        FileOutputStream(output).use {
            bytes.writeTo(it)
            it.fd.sync()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().use { input ->
            val buffer = ByteArray(65536)

            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }

        return digest.digest().joinToString("") {
            (it.toInt() and 255).toString(16).padStart(2, '0')
        }
    }
}