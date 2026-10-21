// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.util.AtomicFile
import org.sg.pdfcolorhighlighter.data.RecentDocumentsStore
import org.sg.pdfcolorhighlighter.domain.*
import java.io.File
import java.util.UUID

/** Isolated synthetic test; never accesses production recents or user PDFs. */
class RecentStorageInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val directory = File(
            targetContext.cacheDir, "recent-storage-proof-${UUID.randomUUID()}"
        )
        var failure: Throwable? = null
        try {
            check(directory.mkdirs())
            val file = File(directory, "recents.bin")
            val store = RecentDocumentsStore(file)
            check(store.read().isEmpty())

            val first = RecentDocument(
                "content://synthetic/A", "Élève 📚", 123, 2, 10,
                PageViewportState(3f, 0.7f, 0.8f, true, false, 0.25f)
            )
            store.write(listOf(first))
            check(RecentDocumentsStore(file).read() == listOf(first))

            val atomic = AtomicFile(file)
            val pending = atomic.startWrite()
            pending.write(byteArrayOf(1, 2, 3))
            atomic.failWrite(pending)
            check(store.read() == listOf(first)) {
                "Failed write damaged committed state"
            }

            val second = first.copy(
                uri = "content://synthetic/B", displayName = "Second", page = 4
            )
            store.write(RecentDocuments.opened(store.read(), second))
            check(store.read() == listOf(second, first))

            file.writeBytes(byteArrayOf(0, 1, 2))
            check(runCatching { store.read() }.isFailure)
            check(file.readBytes().contentEquals(byteArrayOf(0, 1, 2))) {
                "Corrupt evidence was erased"
            }
        } catch (error: Throwable) {
            failure = error
        } finally {
            directory.deleteRecursively()
        }

        val error = failure
        finish(
            if (error == null) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            Bundle().apply {
                putString(
                    "result",
                    if (error == null) "PASS: recent storage"
                    else "FAIL: recent storage"
                )
                if (error != null) putString("details", error.stackTraceToString())
            },
        )
    }
}
