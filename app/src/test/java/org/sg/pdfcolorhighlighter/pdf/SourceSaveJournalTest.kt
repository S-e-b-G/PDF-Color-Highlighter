// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.nio.file.Files
import org.sg.pdfcolorhighlighter.domain.CheckpointFingerprint

class SourceSaveJournalTest{
    private val uri="content://synthetic/original"
    private class Fixture(val root:File){
        val source=File(root,"source.pdf").apply{writeText("old-source")}
        val local=File(root,"local.pdf").apply{writeText("new-local")}
        val directory=File(root,"journal");val journal=SourceSaveJournal(directory)
        val owner=CheckpointFingerprint.of(source)
    }
    private fun fixture(action:(Fixture)->Unit){val dir=Files.createTempDirectory("source-save-test").toFile();try{action(Fixture(dir))}finally{dir.deleteRecursively()}}
    private fun rejects(action:()->Unit){try{action();fail("Expected rejection")}catch(_:Exception){}}
    private fun save(f:Fixture,expected:String=f.owner,output:()->OutputStream={f.source.outputStream()},validate:(File)->Unit={})=
        f.journal.save(uri,expected,f.owner,f.local,{f.source.inputStream()},output,validate)
    private fun prepare(f:Fixture)=f.journal.prepare(uri,f.owner,f.owner,f.local){f.source.inputStream()}
    private fun intents(f:Fixture)=f.directory.walkTopDown().filter{it.name=="intent.bin"}.count()
    @Test fun writesExactBytes(){fixture{f->save(f);assertArrayEquals(f.local.readBytes(),f.source.readBytes())}}
    @Test fun reportsHash(){fixture{f->assertEquals(CheckpointFingerprint.of(f.local),save(f).sha256)}}
    @Test fun reportsSize(){fixture{f->assertEquals(f.local.length(),save(f).sizeBytes)}}
    @Test fun beforeBackupExact(){fixture{f->val before=f.source.readBytes();val r=prepare(f);assertArrayEquals(before,f.journal.beforeFile(uri,r).readBytes())}}
    @Test fun afterBackupExact(){fixture{f->val r=prepare(f);assertArrayEquals(f.local.readBytes(),f.journal.afterFile(uri,r).readBytes())}}
    @Test fun prepareDoesNotWriteSource(){fixture{f->val before=f.source.readBytes();prepare(f);assertArrayEquals(before,f.source.readBytes())}}
    @Test fun conflictNeverOpensOutput(){fixture{f->f.source.writeText("external");var opened=false;rejects{save(f,output={opened=true;f.source.outputStream()})};assertFalse(opened);assertEquals("external",f.source.readText())}}
    @Test fun conflictDoesNotPublishIntent(){fixture{f->f.source.writeText("external");rejects{prepare(f)};assertEquals(0,intents(f))}}
    @Test fun noOpAvoidsOutput(){fixture{f->f.local.writeBytes(f.source.readBytes());var opened=false;save(f,output={opened=true;f.source.outputStream()});assertFalse(opened);assertEquals(0,intents(f))}}
    @Test fun validationPrecedesOutput(){fixture{f->var opened=false;rejects{save(f,output={opened=true;f.source.outputStream()},validate={throw IOException("bad PDF")})};assertFalse(opened);assertEquals("old-source",f.source.readText())}}
    @Test fun publishedIntentPrecedesOutput(){fixture{f->save(f,output={assertEquals(1,intents(f));f.source.outputStream()})}}
    @Test fun savedHashLinksOriginalOwner(){fixture{f->val r=save(f);assertEquals(f.owner,f.journal.ownerFor(uri,r.sha256))}}
    @Test fun unrelatedHashHasNoLink(){fixture{f->save(f);assertNull(f.journal.ownerFor(uri,"a".repeat(64)))}}
    @Test fun unrelatedUriHasNoLink(){fixture{f->val r=save(f);assertNull(f.journal.ownerFor("content://other",r.sha256))}}
    @Test fun unavailableOutputRetainsLocalAndIntent(){fixture{f->val bytes=f.local.readBytes();rejects{save(f,output={throw IOException("unavailable")})};assertArrayEquals(bytes,f.local.readBytes());assertEquals(1,intents(f))}}
    @Test fun partialWriteFailsWithoutLosingLocal(){fixture{f->rejects{save(f,output={object:OutputStream(){val out=f.source.outputStream();override fun write(b:Int){out.write(b);out.flush();throw IOException("partial")};override fun close(){out.close()}}})};assertTrue(f.source.length()<f.local.length());assertEquals("new-local",f.local.readText())}}
    @Test fun closeFailureNotSuccessful(){fixture{f->rejects{save(f,output={object:ByteArrayOutputStream(){override fun close(){throw IOException("close")}}})};assertEquals("new-local",f.local.readText())}}
    @Test fun ignoredWriteFailsVerification(){fixture{f->rejects{save(f,output={ByteArrayOutputStream()})};assertEquals("old-source",f.source.readText())}}
    @Test fun untruncatedTailFailsVerification(){fixture{f->f.source.writeText("old-source-with-longer-tail");val expected=CheckpointFingerprint.of(f.source);rejects{save(f,expected,output={object:OutputStream(){val out=RandomAccessFile(f.source,"rw");override fun write(b:Int){out.write(b)};override fun close(){out.close()}}})}}}
    @Test fun lateConflictNeverOpensOutput(){fixture{f->var opened=false;rejects{save(f,output={opened=true;f.source.outputStream()},validate={f.source.writeText("racing writer")})};assertFalse(opened);assertEquals("racing writer",f.source.readText())}}
    @Test fun repeatedSaveDoesNotDuplicateIntent(){fixture{f->val r=save(f);save(f,r.sha256);assertEquals(1,intents(f))}}
    @Test fun nextSaveKeepsOwner(){fixture{f->val r=save(f);f.local.writeText("newer-local");val next=save(f,r.sha256);assertEquals(f.owner,f.journal.ownerFor(uri,next.sha256));assertEquals(2,intents(f))}}
    @Test fun shorterReplacementTruncates(){fixture{f->f.local.writeText("X");save(f);assertEquals("X",f.source.readText());assertEquals(1L,f.source.length())}}
    @Test fun corruptIntentBlocksAdoption(){fixture{f->val r=save(f);val file=f.directory.walkTopDown().single{it.name=="intent.bin"};val bytes=file.readBytes();bytes[0]=(bytes[0].toInt() xor 1).toByte();file.writeBytes(bytes);rejects{f.journal.ownerFor(uri,r.sha256)}}}
    @Test fun corruptAfterBackupBlocksAdoption(){fixture{f->val r=prepare(f);f.journal.afterFile(uri,r).writeText("bad");rejects{f.journal.ownerFor(uri,r.afterHash)}}}
    @Test fun corruptBeforeBackupBlocksAdoption(){fixture{f->val r=prepare(f);f.journal.beforeFile(uri,r).writeText("bad");rejects{f.journal.ownerFor(uri,r.afterHash)}}}
    @Test fun crossUriBackupRejected(){fixture{f->val r=prepare(f);rejects{f.journal.afterFile("content://other",r)}}}
    @Test fun invalidHashRejected(){fixture{f->rejects{save(f,"bad")};assertEquals("old-source",f.source.readText())}}
    @Test fun unpublishedIntentNotAdopted(){fixture{f->val r=prepare(f);val file=f.directory.walkTopDown().single{it.name=="intent.bin"};assertTrue(file.renameTo(File(file.parentFile,".intent.pending")));assertNull(f.journal.ownerFor(uri,r.afterHash))}}
    @Test fun mixedOwnersRefused(){fixture{f->val r=prepare(f);f.journal.prepare(uri,f.owner,"a".repeat(64),f.local){f.source.inputStream()};rejects{f.journal.ownerFor(uri,r.afterHash)}}}
}
