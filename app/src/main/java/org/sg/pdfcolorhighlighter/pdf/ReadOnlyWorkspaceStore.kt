// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID

class WorkspaceSizeLimit : IOException()

data class SourceFingerprint(val sizeBytes: Long, val sha256: String)
data class WorkspaceBaseline(
    val sourceUri: String,
    val displayName: String?,
    val fingerprint: SourceFingerprint,
    val createdMillis: Long,
)
data class ReadOnlyWorkspace(val directory: File, val baseline: WorkspaceBaseline) {
    val inputFile: File get() = File(directory, "baseline.pdf")
}
enum class SourceComparison { SAME, DIFFERENT, UNAVAILABLE }

/** One owner/worker only. Never stores or deletes annotation edits. */
class ReadOnlyWorkspaceStore(private val root: File) {
    companion object Constants {
        const val MAX_SOURCE_BYTES = 256L * 1024 * 1024
        private const val MANIFEST = "baseline.properties"
        private const val ELIGIBLE = "readonly-retention-v1"
        private const val STAGING = "readonly-staging-v1"
        private const val MARKER = "SG-READONLY-BASELINE-1\n"
        private val live = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        private val ID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    fun create(sourceUri: String, displayName: String?, maximumBytes: Long = MAX_SOURCE_BYTES,
               openInput: () -> InputStream): ReadOnlyWorkspace {
        require(sourceUri.isNotBlank() && maximumBytes in 1..MAX_SOURCE_BYTES)
        check(root.isDirectory || root.mkdirs()) { "Workspace directory unavailable" }
        cleanAbandonedStaging()
        val id = UUID.randomUUID().toString()
        val staging = File(root, ".creating-$id")
        live.add(staging.absolutePath)
        if (!staging.mkdir()) {
            live.remove(staging.absolutePath)
            error("Workspace staging unavailable")
        }
        var published: File? = null
        try {
            syncBytes(File(staging, STAGING), MARKER.toByteArray(Charsets.US_ASCII))
            val copied = openInput().use { incoming ->
                val local = File(staging, "baseline.pdf")
                FileOutputStream(local).use { outgoing ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        val count = incoming.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        if (count.toLong() > maximumBytes - size) throw WorkspaceSizeLimit()
                        outgoing.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        size += count
                    }
                    require(size > 0) { "Empty source" }
                    outgoing.fd.sync()
                    SourceFingerprint(size, hex(digest.digest()))
                }
            }
            check(fingerprint(File(staging, "baseline.pdf")) == copied) { "Staged source verification failed" }
            val baseline = WorkspaceBaseline(sourceUri, displayName, copied,
                System.currentTimeMillis().coerceAtLeast(0))
            writeManifest(File(staging, MANIFEST), baseline)
            check(readManifest(staging) == baseline) { "Baseline identity verification failed" }
            val directory = File(root, id)
            check(!directory.exists() && staging.renameTo(directory)) { "Workspace publication failed" }
            published = directory
            live.add(directory.absolutePath)
            live.remove(staging.absolutePath)
            return ReadOnlyWorkspace(directory, baseline)
        } catch (error: Exception) {
            // These paths were created only by this call and have never held native edits.
            (published ?: staging).deleteRecursively()
            live.remove(staging.absolutePath)
            published?.let { live.remove(it.absolutePath) }
            throw error
        }
    }

    /** Call only after the engine has accepted and rendered this read-only baseline. */
    fun accept(workspace: ReadOnlyWorkspace) {
        requireOwned(workspace.directory)
        check(readManifest(workspace.directory) == workspace.baseline)
        syncBytes(File(workspace.directory, ELIGIBLE), MARKER.toByteArray(Charsets.US_ASCII))
    }

    /** Reads a committed identity and rehashes its complete local source. */
    fun inspect(directory: File): ReadOnlyWorkspace {
        requireOwned(directory)
        val baseline = readManifest(directory)
        check(fingerprint(File(directory, "baseline.pdf")) == baseline.fingerprint) {
            "Workspace baseline fingerprint mismatch"
        }
        return ReadOnlyWorkspace(directory, baseline)
    }

    fun release(workspace: ReadOnlyWorkspace?) {
        workspace?.let { live.remove(it.directory.absolutePath) }
    }

    fun discardCandidate(workspace: ReadOnlyWorkspace?) {
        if (workspace == null) return
        requireOwned(workspace.directory)
        if (File(workspace.directory, ELIGIBLE).exists()) return
        check(readManifest(workspace.directory) == workspace.baseline)
        check(workspace.directory.deleteRecursively()) { "Candidate cleanup failed" }
        release(workspace)
    }

    /** Retain the live source plus the newest other eligible read-only baseline. */
    fun pruneReadOnly(current: ReadOnlyWorkspace): Boolean {
        requireOwned(current.directory)
        val candidates = root.listFiles().orEmpty().mapNotNull { directory ->
            if (!directory.isDirectory || !ID.matches(directory.name) || !eligible(directory)) null
            else try { directory to readManifest(directory) } catch (_: Exception) { null }
        }.sortedWith(compareByDescending<Pair<File, WorkspaceBaseline>> { it.second.createdMillis }
            .thenByDescending { it.first.name })
        val other = candidates.firstOrNull { it.first != current.directory }?.first
        var complete = true
        candidates.forEach { (directory, _) ->
            if (directory != current.directory && directory != other && directory.absolutePath !in live) {
                // Unknown extra files may be future recovery data: never delete that directory.
                val known = setOf("baseline.pdf", MANIFEST, ELIGIBLE, STAGING)
                if (directory.listFiles().orEmpty().any { it.name !in known || !it.isFile }) {
                    complete = false
                } else if (!directory.deleteRecursively()) complete = false
            }
        }
        return complete
    }

    /** Helper for a future save pipeline, not authorization to overwrite any URI. */
    fun compareSource(workspace: ReadOnlyWorkspace, openInput: () -> InputStream): SourceComparison =
        try {
            val current = openInput().use { fingerprint(it) }
            if (current == workspace.baseline.fingerprint) SourceComparison.SAME else SourceComparison.DIFFERENT
        } catch (_: Exception) { SourceComparison.UNAVAILABLE }

    private fun cleanAbandonedStaging() {
        root.listFiles().orEmpty().forEach { directory ->
            if (directory.absolutePath !in live && directory.isDirectory && directory.name.startsWith(".creating-") &&
                ID.matches(directory.name.removePrefix(".creating-")) &&
                marker(File(directory, STAGING)) &&
                directory.listFiles().orEmpty().all {
                    it.isFile && it.name in setOf(STAGING, "baseline.pdf", MANIFEST)
                }) {
                check(directory.deleteRecursively()) { "Abandoned staging cleanup failed" }
            }
        }
    }

    private fun requireOwned(directory: File) {
        require(ID.matches(directory.name) && directory.canonicalFile.parentFile == root.canonicalFile) {
            "Unowned workspace directory"
        }
    }

    private fun eligible(directory: File): Boolean = marker(File(directory, ELIGIBLE))
    private fun marker(file: File): Boolean = file.isFile && file.length() == MARKER.length.toLong() &&
        file.readText(Charsets.US_ASCII) == MARKER

    private fun writeManifest(file: File, baseline: WorkspaceBaseline) {
        val p = Properties().apply {
            setProperty("format", "1")
            setProperty("kind", "immutable-readonly-baseline")
            setProperty("sourceUri", baseline.sourceUri)
            baseline.displayName?.let { setProperty("displayName", it) }
            setProperty("sourceBytes", baseline.fingerprint.sizeBytes.toString())
            setProperty("sourceSha256", baseline.fingerprint.sha256)
            setProperty("createdMillis", baseline.createdMillis.toString())
        }
        FileOutputStream(file).use { output ->
            p.store(output, "Local read-only PDF baseline; not annotation recovery")
            output.fd.sync()
        }
        require(file.length() <= 1024 * 1024) { "Baseline identity too large" }
    }

    private fun readManifest(directory: File): WorkspaceBaseline {
        val file = File(directory, MANIFEST)
        require(file.isFile && file.length() in 1..(1024L * 1024))
        val p = Properties().apply { file.inputStream().use { load(it) } }
        require(p.getProperty("format") == "1" && p.getProperty("kind") == "immutable-readonly-baseline")
        val uri = p.getProperty("sourceUri") ?: error("Missing source URI")
        val size = p.getProperty("sourceBytes")?.toLong() ?: error("Missing source size")
        val hash = p.getProperty("sourceSha256") ?: error("Missing source digest")
        val time = p.getProperty("createdMillis")?.toLong() ?: error("Missing creation time")
        require(uri.isNotBlank() && size in 1..MAX_SOURCE_BYTES && time >= 0 && Regex("[0-9a-f]{64}").matches(hash))
        return WorkspaceBaseline(uri, p.getProperty("displayName"), SourceFingerprint(size, hash), time)
    }

    private fun syncBytes(file: File, bytes: ByteArray) {
        FileOutputStream(file).use { output -> output.write(bytes); output.fd.sync() }
    }

    private fun fingerprint(file: File): SourceFingerprint = file.inputStream().use { fingerprint(it) }
    private fun fingerprint(input: InputStream): SourceFingerprint {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var size = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            if (count.toLong() > MAX_SOURCE_BYTES - size) throw WorkspaceSizeLimit()
            digest.update(buffer, 0, count)
            size += count
        }
        return SourceFingerprint(size, hex(digest.digest()))
    }
    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
