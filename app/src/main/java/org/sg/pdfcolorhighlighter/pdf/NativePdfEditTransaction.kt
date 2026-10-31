// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFDocument
import java.io.File

/** Invoke only on the native owner worker. Mutation receives a separate borrowed candidate,
 * NEVER the live reader document; close its Page/Annotation children before returning and
 * retain no native object. This adapter closes the candidate/validator document itself.
 * Copy permission remains the responsibility of operations that extract document text.
 */
object NativePdfEditTransaction {
    private inline fun <R> withEditable(file:File,action:(PDFDocument)->R):R {
        val opened=Document.openDocument(file.absolutePath)
        try {
            require(opened is PDFDocument){"Not a PDF candidate"}
            require(!opened.needsPassword() && opened.isUnencryptedPDF()){ "Encrypted PDF editing is not enabled" }
            require(opened.countSignatures()==0){"Signed PDF editing is not enabled"}
            require(opened.countPages()>0 && opened.hasPermission(Document.PERMISSION_ANNOTATE)){"PDF annotation permission unavailable"}
            return action(opened)
        }finally{opened.destroy()}
    }
    fun validateEditable(file:File){withEditable(file){Unit}}
    fun commit(store:DurablePdfRevisionStore,expected:PdfRevision,operationId:String,
               mutation:(PDFDocument)->Unit):PdfRevision {
        val current=store.recover()
        if(current.operationId==operationId && current.sessionId==expected.sessionId)return current
        if(current!=expected)throw StalePdfRevision()
        val input=store.currentFile(expected)
        var pages=0
        return store.commit(expected,operationId,writePdf={output->
            withEditable(input){pdf->
                pages=pdf.countPages()
                mutation(pdf)
                require(pdf.countPages()==pages){"An annotation transaction must not alter the page count"}
                pdf.save(output.absolutePath,"compress")
            }
        },validatePdf={output->withEditable(output){require(it.countPages()==pages){"Candidate page count changed"}}})
    }
}
