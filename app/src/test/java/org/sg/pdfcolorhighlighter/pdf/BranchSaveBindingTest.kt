// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.nio.file.Files
import java.security.MessageDigest
class BranchSaveBindingTest{
    private fun hash(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
    @Test fun branchNoopDoesNotNeedUniqueLegacyOwner(){
        val root=Files.createTempDirectory("branch-save").toFile()
        try{
            val journal=SourceSaveJournal(File(root,"journal"));val uri="content://synthetic/branches"
            val original="source".toByteArray();val target="target".toByteArray();var disk=original
            val local=File(root,"local.pdf").apply{writeBytes(target)}
            fun input():InputStream=ByteArrayInputStream(disk)
            fun output():OutputStream=object:ByteArrayOutputStream(){override fun close(){disk=toByteArray();super.close()}}
            val ownerA=hash("ownerA".toByteArray());val ownerB=hash("ownerB".toByteArray())
            journal.save(uri,hash(original),ownerA,local,::input,::output,{})
            disk=original;journal.save(uri,hash(original),ownerB,local,::input,::output,{})
            try{journal.ownerFor(uri,hash(target));fail("Expected historical alias ambiguity")}catch(_:IllegalArgumentException){}
            val receipt=journal.saveBranch(uri,hash(target),ownerA,local,::input,::output,{})
            assertEquals(hash(target),receipt.sha256);assertArrayEquals(target,disk)
        }finally{root.deleteRecursively()}
    }
    @Test fun branchModeStillRejectsSourceConflict(){
        val root=Files.createTempDirectory("branch-conflict").toFile()
        try{
            val journal=SourceSaveJournal(File(root,"journal"));val local=File(root,"local.pdf").apply{writeText("target")}
            var writes=0
            try{journal.saveBranch("content://synthetic/branches",hash("old".toByteArray()),hash("owner".toByteArray()),local,
                {ByteArrayInputStream("changed".toByteArray())},{writes++;ByteArrayOutputStream()},{});fail()}
            catch(_:SourceSaveConflict){}
            assertEquals(0,writes)
        }finally{root.deleteRecursively()}
    }
}
