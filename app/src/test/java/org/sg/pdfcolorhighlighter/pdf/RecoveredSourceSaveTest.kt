// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf
import java.io.*
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test
import org.sg.pdfcolorhighlighter.domain.CheckpointFingerprint
class RecoveredSourceSaveTest {
    private fun trial(action:(File,File,SourceSaveJournal,String)->Unit){
        val root=Files.createTempDirectory("recovered-save").toFile()
        try{val target=File(root,"source").apply{writeText("external-C")}
            val local=File(root,"local").apply{writeText("recovered-AB")}
            action(target,local,SourceSaveJournal(File(root,"journal")),"a".repeat(64))
        }finally{root.deleteRecursively()}
    }
    @Test fun explicitCurrentHashCanReplaceRecoveredTarget(){trial{src,local,j,owner->
        val approved=CheckpointFingerprint.of(src)
        val r=j.save("content://test",approved,owner,local,{src.inputStream()},{src.outputStream()},{})
        assertEquals(local.readText(),src.readText());assertEquals(owner,j.ownerFor("content://test",r.sha256))
    }}
    @Test fun externalChangeAfterApprovalRefusesOutput(){trial{src,local,j,owner->
        val approved=CheckpointFingerprint.of(src);src.writeText("changed-again");var opened=false
        try{j.save("content://test",approved,owner,local,{src.inputStream()},{opened=true;src.outputStream()},{ });fail("Expected conflict")}
        catch(_:SourceSaveConflict){}
        assertFalse(opened);assertEquals("changed-again",src.readText());assertEquals("recovered-AB",local.readText())
    }}
    @Test fun identicalRecoveredBytesBindOwnerWithoutRewrite(){trial{src,local,j,owner->
        src.writeBytes(local.readBytes());val approved=CheckpointFingerprint.of(src);var opened=false
        val r=j.save("content://test",approved,owner,local,{src.inputStream()},{opened=true;src.outputStream()},{})
        assertFalse(opened);assertEquals(owner,j.ownerFor("content://test",r.sha256))
    }}
    @Test fun recoveryBackupKeepsCurrentExternalVersion(){trial{src,local,j,owner->
        val expected=CheckpointFingerprint.of(src)
        val r=j.prepare("content://test",expected,owner,local){src.inputStream()}
        assertEquals("external-C",j.beforeFile("content://test",r).readText())
        assertEquals("recovered-AB",j.afterFile("content://test",r).readText())
        assertEquals("external-C",src.readText())
    }}
}
