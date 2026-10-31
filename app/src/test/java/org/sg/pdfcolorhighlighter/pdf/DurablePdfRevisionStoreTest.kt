// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.UUID

class DurablePdfRevisionStoreTest {
    @get:Rule val temporary=TemporaryFolder()
    private fun folder()=File(temporary.root,"session")
    private fun source()=File(temporary.root,"input.pdf").also{it.writeText("BASELINE")}
    private fun store(fault:(RevisionStage)->Unit = {})=DurablePdfRevisionStore(folder(),fault)
    private fun initialize(s:DurablePdfRevisionStore=store()):PdfRevision=s.initialize(source()){}
    private fun id()=UUID.randomUUID().toString()
    private fun next(s:DurablePdfRevisionStore,expected:PdfRevision,text:String="EDIT",operation:String=id())=
        s.commit(expected,operation,{it.writeText(text)},{})
    private fun rejects(block:()->Unit){try{block();fail("Expected rejection")}catch(_:Exception){}}
    @Test fun initialCopyIsVerifiedAndIndependent(){val input=source();val s=store();val r=s.initialize(input){};assertEquals(0L,r.sequence);assertEquals("BASELINE",s.currentFile(r).readText());assertNotEquals(input.canonicalFile,s.currentFile(r).canonicalFile)}
    @Test fun sourceBytesNeverChange(){val input=source();val bytes=input.readBytes();val s=store();val r=s.initialize(input){};next(s,r);assertArrayEquals(bytes,input.readBytes())}
    @Test fun freshStoreRecoversCurrentHead(){val s=store();val a=initialize(s);val b=next(s,a);assertEquals(b,store().recover());assertEquals("EDIT",store().currentFile(b).readText())}
    @Test fun sequencesAndBaselineIdentityRemainConsistent(){val s=store();val a=initialize(s);val b=next(s,a);val c=next(s,b,"EDIT2");assertEquals(2L,c.sequence);assertEquals(a.baselineSha256,c.baselineSha256);assertNotEquals(b.sha256,c.sha256)}
    @Test fun staleExpectedHeadCannotOverwriteNewerEdit(){val s=store();val a=initialize(s);val b=next(s,a);rejects{next(s,a,"STALE")};assertEquals(b,s.recover())}
    @Test fun writerFailurePreservesCommittedHead(){val s=store();val a=initialize(s);rejects{s.commit(a,id(),{it.writeText("PARTIAL");throw IOException("writer failure")},{})};assertEquals(a,s.recover())}
    @Test fun validatorFailurePreservesCommittedHead(){val s=store();val a=initialize(s);rejects{s.commit(a,id(),{it.writeText("BAD")},{throw IOException("validation failure")})};assertEquals(a,s.recover())}
    @Test fun emptyRevisionIsRejected(){val s=store();val a=initialize(s);rejects{s.commit(a,id(),{it.writeBytes(byteArrayOf())},{})};assertEquals(a,s.recover())}
    @Test fun interruptionAfterPdfSyncPreservesOldHead(){val normal=store();val a=initialize(normal);val failing=store{if(it==RevisionStage.PDF_SYNCED)throw IOException("simulated interruption")};rejects{next(failing,a)};assertEquals(a,normal.recover())}
    @Test fun interruptionAfterPdfPublicationPreservesOldHead(){val normal=store();val a=initialize(normal);val failing=store{if(it==RevisionStage.PDF_PUBLISHED)throw IOException("simulated interruption")};rejects{next(failing,a)};assertEquals(a,normal.recover())}
    @Test fun interruptionAfterInactiveSlotRemovalKeepsCurrentHead(){val normal=store();val a=initialize(normal);val b=next(normal,a);val failing=store{if(it==RevisionStage.INACTIVE_SLOT_REMOVED)throw IOException("simulated interruption")};rejects{next(failing,b,"EDIT2")};assertEquals(b,normal.recover())}
    @Test fun publishedCheckpointSurvivesUncertainReturn(){val normal=store();val a=initialize(normal);val operation=id();val failing=store{if(it==RevisionStage.HEAD_PUBLISHED)throw IOException("simulated lost acknowledgement")};rejects{next(failing,a,"EDIT",operation)};val recovered=normal.recover();assertEquals(1L,recovered.sequence);assertEquals(operation,recovered.operationId);assertEquals("EDIT",normal.currentFile(recovered).readText())}
    @Test fun retryOfLatestOperationDoesNotExecuteWriterAgain(){val s=store();val a=initialize(s);val operation=id();val b=next(s,a,"EDIT",operation);var calls=0;val retry=s.commit(a,operation,{calls++;it.writeText("WRONG")},{});assertEquals(b,retry);assertEquals(0,calls)}
    @Test fun olderOperationRetryAfterAnotherCommitIsStale(){val s=store();val a=initialize(s);val operation=id();val b=next(s,a,"EDIT",operation);val c=next(s,b,"EDIT2");rejects{next(s,a,"WRONG",operation)};assertEquals(c,s.recover())}
    @Test fun corruptManifestIsNotSilentlyIgnored(){val s=store();val a=initialize(s);next(s,a);File(folder(),"head-1.bin").writeBytes(byteArrayOf(1,2,3));rejects{store().recover()};assertTrue(File(folder(),"head-0.bin").exists())}
    @Test fun modifiedPublishedPdfIsDetected(){val s=store();val a=initialize(s);s.currentFile(a).writeText("TAMPERED");rejects{store().recover()}}
    @Test fun pruneKeepsBothCommittedSlotsAndCurrentPdf(){val s=store();var r=initialize(s);repeat(5){r=next(s,r,"EDIT$it")};assertTrue(s.pruneOld());val pdfs=folder().listFiles()!!.filter{it.name.startsWith("r-") && it.extension=="pdf"};assertEquals(2,pdfs.size);assertEquals(r,s.recover())}
    @Test fun explicitPinsAndUnknownMaterialArePreserved(){val s=store();val a=initialize(s);val b=next(s,a);next(s,b,"EDIT2");val unknown=File(folder(),"future-recovery.journal").also{it.writeText("KEEP")};assertTrue(s.pruneOld(setOf(a.fileName)));assertTrue(File(folder(),a.fileName).exists());assertEquals("KEEP",unknown.readText())}
    @Test fun existingDirectoryCannotBeReinitialized(){val s=store();val a=initialize(s);rejects{s.initialize(source()){} };assertEquals(a,s.recover())}
    @Test fun invalidOperationIdentityIsRejectedWithoutChangingHead(){val s=store();val a=initialize(s);rejects{next(s,a,"BAD","../outside")};assertEquals(a,s.recover());rejects{s.pruneOld(setOf("../outside.pdf"))}}
}
