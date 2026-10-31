// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import java.io.*
import java.security.MessageDigest
import java.util.UUID

/** A validated immutable PDF checkpoint, not an annotation command/undo-history implementation. */
data class PdfRevision(
    val sessionId:String,val sequence:Long,val operationId:String,val fileName:String,
    val sizeBytes:Long,val sha256:String,val baselineSha256:String,
)
enum class RevisionStage { PDF_SYNCED, PDF_PUBLISHED, INACTIVE_SLOT_REMOVED, HEAD_PUBLISHED }
class StalePdfRevision:IOException("Stale PDF revision; recover current state before retrying")

/** New, app-private non-cache directory only. Never point at a user's source/provider directory.
 * All native callbacks must be on the serialized native owner and close their objects before return.
 * Published files are immutable; only writer's fresh staging destination may be written.
 * Two manifest slots avoid replacing the active record. File sync+same-directory rename is NOT
 * a hardware-power-loss guarantee (parent-directory fsync is not implemented).
 */
class DurablePdfRevisionStore(
    directory:File,
    private val fault:(RevisionStage)->Unit = {},
) {
    private val root=directory.canonicalFile
    companion object {
        const val MAX_PDF_BYTES=256L*1024*1024
        private const val MAGIC=0x53475256
        private const val VERSION=1
        private const val MAX_MANIFEST=4096
        private val ID=Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private val HASH=Regex("[0-9a-f]{64}")
        private val PDF_NAME=Regex("""r-([0-9]{20})-([0-9a-f]{32})-([0-9a-f]{32})\.pdf""")
    }
    private fun slot(index:Int)=File(root,"head-$index.bin")
    private fun owned(name:String):File {
        require(PDF_NAME.matches(name)){"Invalid revision filename"}
        val file=File(root,name)
        require(file.canonicalFile.parentFile==root && file.canonicalFile.name==name){"Revision path escaped its owner"}
        return file
    }
    private fun <R> locked(action:()->R):R {
        check(root.isDirectory){"Revision directory is unavailable"}
        return RandomAccessFile(File(root,"owner.lock"),"rw").use { lockFile ->
            val lock=lockFile.channel.lock()
            try{action()}finally{lock.release()}
        }
    }
    private fun digest(file:File):Pair<Long,String> {
        require(file.isFile && file.length() in 1..MAX_PDF_BYTES){"Invalid PDF revision size"}
        val sha=MessageDigest.getInstance("SHA-256");var size=0L
        file.inputStream().use { input ->
            val bytes=ByteArray(64*1024)
            while(true){val n=input.read(bytes);if(n<0)break;if(n==0)continue
                if(n.toLong()>MAX_PDF_BYTES-size)throw IOException("PDF revision size limit")
                sha.update(bytes,0,n);size+=n
            }
        }
        require(size>0){"Empty PDF revision"}
        return size to hex(sha.digest())
    }
    private fun hex(bytes:ByteArray)=bytes.joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
    private fun validateRecord(r:PdfRevision) {
        require(ID.matches(r.sessionId) && ID.matches(r.operationId) && r.sequence>=0)
        require(r.sizeBytes in 1..MAX_PDF_BYTES && HASH.matches(r.sha256) && HASH.matches(r.baselineSha256))
        val match=PDF_NAME.matchEntire(r.fileName) ?: throw IOException("Invalid revision name")
        require(match.groupValues[1].toLongOrNull()==r.sequence)
        require(match.groupValues[2]==r.sessionId.replace("-",""))
        require(match.groupValues[3]==r.operationId.replace("-",""))
    }
    private fun encode(r:PdfRevision):ByteArray {
        validateRecord(r)
        val buffer=ByteArrayOutputStream()
        DataOutputStream(buffer).use { out ->
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeUTF(r.sessionId);out.writeLong(r.sequence)
            out.writeUTF(r.operationId);out.writeUTF(r.fileName);out.writeLong(r.sizeBytes)
            out.writeUTF(r.sha256);out.writeUTF(r.baselineSha256)
        }
        val body=buffer.toByteArray();val result=body+MessageDigest.getInstance("SHA-256").digest(body)
        require(result.size<=MAX_MANIFEST);return result
    }
    private fun readSlot(index:Int):PdfRevision? {
        val file=slot(index);if(!file.exists())return null
        require(file.isFile && file.length() in 33L..MAX_MANIFEST.toLong()){ "Invalid revision manifest" }
        val bytes=file.readBytes();val body=bytes.copyOfRange(0,bytes.size-32)
        require(MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(body),bytes.copyOfRange(bytes.size-32,bytes.size))){"Revision manifest checksum"}
        val revision=DataInputStream(ByteArrayInputStream(body)).use { input ->
            require(input.readInt()==MAGIC && input.readInt()==VERSION){"Unknown revision manifest"}
            val r=PdfRevision(input.readUTF(),input.readLong(),input.readUTF(),input.readUTF(),input.readLong(),input.readUTF(),input.readUTF())
            require(input.available()==0){"Trailing revision metadata"};r
        }
        validateRecord(revision);require(revision.sequence%2==index.toLong()){ "Revision slot mismatch" }
        require(digest(owned(revision.fileName))==(revision.sizeBytes to revision.sha256)){"PDF revision fingerprint mismatch"}
        return revision
    }
    private fun records():List<PdfRevision> {
        val all=listOfNotNull(readSlot(0),readSlot(1)).sortedBy{it.sequence}
        check(all.isNotEmpty()){ "No complete revision checkpoint; preserve this directory" }
        if(all.size==2){
            require(all[0].sessionId==all[1].sessionId && all[0].baselineSha256==all[1].baselineSha256){"Mixed revision sessions"}
            require(all[0].sequence<Long.MAX_VALUE && all[1].sequence==all[0].sequence+1){"Ambiguous revision sequence"}
        }
        return all
    }
    fun recover():PdfRevision=locked { records().last() }
    fun currentFile(expected:PdfRevision):File=locked {
        val head=records().last();if(head!=expected)throw StalePdfRevision();owned(head.fileName)
    }
    fun initialize(source:File,validatePdf:(File)->Unit):PdfRevision {
        check(!root.exists()){ "A revision store must be initialized in a fresh directory" }
        root.parentFile?.let{check(it.isDirectory || it.mkdirs())}
        check(root.mkdir()){ "Cannot create revision directory" }
        return locked {
            val session=UUID.randomUUID().toString();val operation=UUID.randomUUID().toString()
            publish(null,session,0,operation,{ target ->
                source.inputStream().use { input -> FileOutputStream(target).use { output ->
                    val buffer=ByteArray(64*1024);var size=0L
                    while(true){val n=input.read(buffer);if(n<0)break;if(n==0)continue
                        if(n.toLong()>MAX_PDF_BYTES-size)throw IOException("PDF revision size limit")
                        output.write(buffer,0,n);size+=n
                    }
                } }
            },validatePdf)
        }
    }
    fun commit(expected:PdfRevision,operationId:String,writePdf:(File)->Unit,validatePdf:(File)->Unit):PdfRevision=locked {
        require(ID.matches(operationId)){"Operation ID must be a UUID"}
        val head=records().last()
        // A stable operation UUID makes a retry after an uncertain publish outcome idempotent.
        // Reusing an ID for different content is a caller bug; IDs identify exact operations.
        if(head.operationId==operationId && head.sessionId==expected.sessionId)return@locked head
        if(head!=expected)throw StalePdfRevision()
        check(head.sequence<Long.MAX_VALUE){"Revision sequence exhausted"}
        publish(head,head.sessionId,head.sequence+1,operationId,writePdf,validatePdf)
    }
    private fun publish(previous:PdfRevision?,session:String,sequence:Long,operation:String,
                        writePdf:(File)->Unit,validatePdf:(File)->Unit):PdfRevision {
        val staging=File(root,".pending-${UUID.randomUUID()}.pdf")
        check(!staging.exists())
        writePdf(staging)
        require(staging.isFile && staging.length() in 1..MAX_PDF_BYTES){"Invalid staged revision"}
        validatePdf(staging) // Caller must close all native validation objects before returning.
        RandomAccessFile(staging,"rw").use{it.fd.sync()}
        fault(RevisionStage.PDF_SYNCED)
        val (size,hash)=digest(staging)
        val name="r-${sequence.toString().padStart(20,'0')}-${session.replace("-","")}-${operation.replace("-","")}.pdf"
        val destination=owned(name);check(!destination.exists())
        check(staging.renameTo(destination)){"Revision publication failed"}
        fault(RevisionStage.PDF_PUBLISHED)
        val revision=PdfRevision(session,sequence,operation,name,size,hash,previous?.baselineSha256 ?: hash)
        val manifest=File(root,".pending-${UUID.randomUUID()}.head")
        FileOutputStream(manifest).use{out->out.write(encode(revision));out.fd.sync()}
        val target=slot((sequence%2).toInt())
        // Delete only the INACTIVE older slot. The other current slot remains intact throughout.
        if(target.exists())check(target.delete()){ "Cannot remove inactive revision slot" }
        fault(RevisionStage.INACTIVE_SLOT_REMOVED)
        check(manifest.renameTo(target)){"Checkpoint publication failed"}
        fault(RevisionStage.HEAD_PUBLISHED)
        val recovered=records().last();check(recovered==revision){"Checkpoint read-back mismatch"}
        return recovered
    }
    /** Call only on the owner worker after native readers of old checkpoints are closed.
     * Retain both head slots and explicit pins. Ignore pending/future/unknown-session material.
     * Does not run automatically inside commit and never deletes a source/provider file.
     */
    fun pruneOld(pinnedFileNames:Set<String> = emptySet()):Boolean=locked {
        val heads=records();val session=heads.last().sessionId.replace("-","")
        pinnedFileNames.forEach{require(PDF_NAME.matches(it))}
        val keep=heads.map{it.fileName}.toSet()+pinnedFileNames
        val oldest=heads.minOf{it.sequence};var complete=true
        for(file in root.listFiles().orEmpty()){
            val m=PDF_NAME.matchEntire(file.name) ?: continue
            val seq=m.groupValues[1].toLongOrNull() ?: continue
            if(m.groupValues[2]==session && seq<oldest && file.name !in keep && file.isFile &&
                file.canonicalFile.parentFile==root && file.canonicalFile.name==file.name){
                if(!file.delete())complete=false
            }
        }
        complete
    }
}
