// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID
import org.sg.pdfcolorhighlighter.domain.LocalEditIdentity
import org.sg.pdfcolorhighlighter.domain.CheckpointFingerprint
class RecoveryBranchesTest{
    private val uri="content://synthetic/m3p"
    private fun trial(body:(File,RecoveryBranches)->Unit){val r=Files.createTempDirectory("m3p-branches").toFile();try{body(r,RecoveryBranches(File(r,"v2"),File(r,"v1")))}finally{r.deleteRecursively()}}
    private fun raw(r:File,name:String,text:String)=File(r,name).apply{writeText(text)}
    private fun store(r:File,c:RecoveryBranches,text:String="A",legacy:Boolean=false):Triple<File,DurablePdfRevisionStore,PdfRevision>{
        val a=raw(r,"source-${UUID.randomUUID()}",text);val hash=CheckpointFingerprint.of(a)
        val parent=if(legacy)File(r,"v1")else c.fresh(uri)
        val d=File(parent,LocalEditIdentity.key(uri,hash));val s=DurablePdfRevisionStore(d)
        return Triple(parent,s,s.initialize(a){} )
    }
    private fun edit(r:File,s:DurablePdfRevisionStore,h:PdfRevision,text:String):PdfRevision=s.commit(h,UUID.randomUUID().toString(),{it.writeText(text)},{})
    @Test fun freshNamespacesDiffer(){trial{_,c->assertNotEquals(c.fresh(uri),c.fresh(uri))}}
    @Test fun neverEditedDiskNotRecovery(){trial{r,c->store(r,c);assertTrue(c.list(uri).choices.isEmpty())}}
    @Test fun committedLocalListed(){trial{r,c->val(_,s,h)=store(r,c);val b=edit(r,s,h,"B");assertEquals(b.sha256,c.list(uri).choices.single().sha256)}}
    @Test fun sameBaselineHeadsStayIndependent(){trial{r,c->val(_,s,h)=store(r,c);val(_,s2,h2)=store(r,c);val x=edit(r,s,h,"B");val y=edit(r,s2,h2,"D");assertEquals(setOf(x.sha256,y.sha256),c.list(uri).choices.map{it.sha256}.toSet())}}
    @Test fun sameAsDiskNotOffered(){trial{r,c->val(_,s,h)=store(r,c);val x=edit(r,s,h,"B");assertTrue(c.list(uri,x.sha256).choices.isEmpty())}}
    @Test fun savedBranchNotOffered(){trial{r,c->val(p,s,h)=store(r,c);val x=edit(r,s,h,"B");c.saved(p,x.sha256);assertTrue(c.list(uri).choices.isEmpty())}}
    @Test fun editAfterSaveOffered(){trial{r,c->val(p,s,h)=store(r,c);val x=edit(r,s,h,"B");c.saved(p,x.sha256);val y=edit(r,s,x,"C");assertEquals(y.sha256,c.list(uri).choices.single().sha256)}}
    @Test fun recoveredBaselineRemainsOffered(){trial{r,c->val(p,_,h)=store(r,c,"B");c.recovered(p,h.sha256);assertEquals(h.sha256,c.list(uri).choices.single().sha256)}}
    @Test fun excludeActiveNamespace(){trial{r,c->val(p,s,h)=store(r,c);edit(r,s,h,"B");assertTrue(c.list(uri,exclude=p).choices.isEmpty())}}
    @Test fun staleSelectionRejected(){trial{r,c->val(_,s,h)=store(r,c);val x=edit(r,s,h,"B");val choice=c.list(uri).choices.single();edit(r,s,x,"C");try{c.resolve(uri,choice.id,choice.sha256);fail()}catch(_:IllegalArgumentException){}}}
    @Test fun legacyReadableWithoutMigration(){trial{r,c->val(_,s,h)=store(r,c,legacy=true);val x=edit(r,s,h,"B");val choice=c.list(uri).choices.single();assertTrue(choice.legacy);assertEquals(x.sha256,CheckpointFingerprint.of(c.resolve(uri,choice.id,choice.sha256)))}}
    @Test fun malformedMarkerDoesNotHideWork(){trial{r,c->val(p,s,h)=store(r,c);edit(r,s,h,"B");File(p,"source-saved.sha256").writeText("partial");assertEquals(1,c.list(uri).choices.size)}}
    @Test fun equalContentDeduplicatedInMenuOnly(){trial{r,c->val(_,s,h)=store(r,c);val(_,s2,h2)=store(r,c);val x=edit(r,s,h,"B");edit(r,s2,h2,"B");assertEquals(1,c.list(uri).choices.size);assertEquals(x.sha256,s.currentFile(x).let{CheckpointFingerprint.of(it)})}}
    @Test fun anotherUriNotMixed(){trial{r,c->val(_,s,h)=store(r,c);edit(r,s,h,"B");assertTrue(c.list("content://other/document").choices.isEmpty())}}
    @Test fun cleanMarkerAfterRecoveredSaveSuppressesMenu(){trial{r,c->val(p,_,h)=store(r,c,"B");c.recovered(p,h.sha256);c.saved(p,h.sha256);assertTrue(c.list(uri).choices.isEmpty())}}
    @Test fun incompleteNamespaceReportedNotDeleted(){trial{r,c->val(p,_,h)=store(r,c);val d=File(p,LocalEditIdentity.key(uri,h.sha256));File(d,"head-0.bin").writeText("broken");assertEquals(1,c.list(uri).rejected);assertTrue(d.isDirectory)}}

    @Test fun savedCopyAvailableWhenSourceUnavailable(){trial{r,c->val(p,s,h)=store(r,c);val x=edit(r,s,h,"B");c.saved(p,x.sha256);assertEquals(x.sha256,c.list(uri,includeClean=true).choices.single().sha256)}}
    @Test fun baselineCopyAvailableWhenSourceUnavailable(){trial{r,c->val(_,_,h)=store(r,c);assertEquals(h.sha256,c.list(uri,includeClean=true).choices.single().sha256)}}
    @Test fun discoveryDoesNotAuthorizeCorruptSnapshot(){trial{r,c->val(_,s,h)=store(r,c);val x=edit(r,s,h,"B");val file=s.currentFile(x);file.writeText("X");val choice=c.list(uri).choices.single();try{c.resolve(uri,choice.id,choice.sha256);fail()}catch(_:IllegalArgumentException){}}}
}
