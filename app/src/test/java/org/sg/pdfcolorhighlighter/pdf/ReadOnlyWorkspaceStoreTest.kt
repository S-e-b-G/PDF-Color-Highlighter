// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Properties

class ReadOnlyWorkspaceStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun store() = ReadOnlyWorkspaceStore(temporary.root)
    private fun create(store: ReadOnlyWorkspaceStore, text: String = "synthetic bytes") =
        store.create("content://synthetic/é", "Élève 📚.pdf") { ByteArrayInputStream(text.toByteArray()) }
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Expected rejection") } catch (_: Exception) { }
    }
    private fun directories() = temporary.root.listFiles().orEmpty().filter { it.isDirectory }

    @Test fun createsDurableCopy() {
        val s = store(); val w = create(s)
        assertEquals("synthetic bytes", w.inputFile.readText()); assertTrue(w.directory.isDirectory)
    }
    @Test fun neverChangesSourceBytes() {
        val bytes = "source untouched".toByteArray(); val original = bytes.copyOf()
        store().create("content://source", null) { ByteArrayInputStream(bytes) }
        assertArrayEquals(original, bytes)
    }
    @Test fun identityRoundTripsUnicode() {
        val s = store(); val w = create(s)
        assertEquals(w.baseline, s.inspect(w.directory).baseline)
        assertEquals("Élève 📚.pdf", w.baseline.displayName)
    }
    @Test fun sameSizeDifferentBytesHaveDifferentFingerprint() {
        val s = store(); assertNotEquals(create(s, "abc").baseline.fingerprint, create(s, "abd").baseline.fingerprint)
    }
    @Test fun newStoreCanInspectExistingWorkspace() {
        val s = store(); val w = create(s); s.accept(w); s.release(w)
        assertEquals(w.baseline, store().inspect(w.directory).baseline)
    }
    @Test fun sizeLimitRejectsAndRemovesPartialCopy() {
        rejects { store().create("content://limit", null, 4) { ByteArrayInputStream(ByteArray(5)) } }
        assertTrue(directories().isEmpty())
    }
    @Test fun interruptedInputRemovesPartialCopy() {
        rejects { store().create("content://failed", null) {
            object : InputStream() { override fun read(): Int = throw IOException("Synthetic failure") }
        } }
        assertTrue(directories().isEmpty())
    }
    @Test fun emptySourceIsRejected() {
        rejects { store().create("content://empty", null) { ByteArrayInputStream(byteArrayOf()) } }
        assertTrue(directories().isEmpty())
    }
    @Test fun tamperedLocalBytesAreDetected() {
        val s = store(); val w = create(s); w.inputFile.writeText("changed")
        rejects { s.inspect(w.directory) }
    }
    @Test fun unknownManifestVersionIsRejected() {
        val s = store(); val w = create(s); val file = File(w.directory, "baseline.properties")
        val p = Properties().apply { file.inputStream().use { load(it) }; setProperty("format", "99") }
        file.outputStream().use { p.store(it, null) }
        rejects { s.inspect(w.directory) }
    }
    @Test fun retainsTwoEligibleSnapshots() {
        val s = store()
        repeat(4) { val w = create(s, "copy $it"); s.accept(w); s.release(w); assertTrue(s.pruneReadOnly(w)) }
        assertEquals(2, directories().size)
    }
    @Test fun unacceptedCandidateIsNotPruned() {
        val s = store(); val candidate = create(s, "candidate"); s.release(candidate)
        repeat(3) { val w = create(s, "accepted $it"); s.accept(w); s.release(w); s.pruneReadOnly(w) }
        assertTrue(candidate.inputFile.exists())
    }
    @Test fun unknownDirectoryIsPreserved() {
        val unknown = File(temporary.root, "future-recovery").apply { mkdir() }
        File(unknown, "edits.journal").writeText("keep")
        val s = store(); val w = create(s); s.accept(w); s.pruneReadOnly(w)
        assertTrue(File(unknown, "edits.journal").exists())
    }
    @Test fun sourceComparisonDetectsChangedBytes() {
        val s = store(); val w = create(s, "abc")
        assertEquals(SourceComparison.DIFFERENT, s.compareSource(w) { ByteArrayInputStream("abd".toByteArray()) })
    }
    @Test fun sourceComparisonReportsUnavailable() {
        val s = store(); val w = create(s)
        assertEquals(SourceComparison.UNAVAILABLE, s.compareSource(w) { throw IOException("Unavailable") })
    }
    @Test fun sourceComparisonRecognizesSameBytes() {
        val s = store(); val w = create(s, "abc")
        assertEquals(SourceComparison.SAME, s.compareSource(w) { ByteArrayInputStream("abc".toByteArray()) })
    }
    @Test fun liveWorkspaceIsNeverPruned() {
        val s = store(); val live = create(s, "live"); s.accept(live)
        repeat(3) { val w = create(s, "other $it"); s.accept(w); s.release(w); s.pruneReadOnly(w) }
        assertTrue(live.inputFile.exists()); s.release(live)
    }
    @Test fun unknownExtraFilePreventsPruning() {
        val s = store(); val protectedCopy = create(s, "protectedCopy"); s.accept(protectedCopy); s.release(protectedCopy)
        val identity = File(protectedCopy.directory, "baseline.properties")
        val metadata = Properties().apply { identity.inputStream().use { load(it) }; setProperty("createdMillis", "0") }
        identity.outputStream().use { metadata.store(it, null) }
        File(protectedCopy.directory, "future-edits.journal").writeText("preserve")
        repeat(3) { val w = create(s, "new $it"); s.accept(w); s.release(w); s.pruneReadOnly(w) }
        assertTrue(File(protectedCopy.directory, "future-edits.journal").exists())
    }
    @Test fun outsideRootCannotBeInspected() {
        val outside = File(temporary.root.parentFile, java.util.UUID.randomUUID().toString())
        rejects { store().inspect(outside) }
    }
    @Test fun rejectedCandidateCanBeDiscardedButAcceptedOneCannot() {
        val s = store(); val rejected = create(s); s.discardCandidate(rejected)
        assertFalse(rejected.directory.exists())
        val accepted = create(s); s.accept(accepted); s.discardCandidate(accepted)
        assertTrue(accepted.inputFile.exists()); s.release(accepted)
    }
}
