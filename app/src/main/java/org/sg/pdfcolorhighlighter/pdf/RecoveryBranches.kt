// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.sg.pdfcolorhighlighter.domain.LocalEditIdentity

data class LocalRecoveryChoice(val id:String,val sha256:String,val modified:Long,val sizeBytes:Long,val legacy:Boolean)
data class RecoveryListing(val choices:List<LocalRecoveryChoice>,val rejected:Int)
class RecoveryCopiesAvailable(val listing:RecoveryListing,cause:Exception):Exception("Source unavailable; local copies require explicit selection",cause)

/** Separate new namespace per disk/recovery opening; no deletion or in-place legacy migration. */
class RecoveryBranches(private val root:File,private val legacy:File){
    private val hash=Regex("[0-9a-f]{64}");private val uuid=Regex("[0-9a-f]{32}")
    private fun parent(uri:String)=File(root,LocalEditIdentity.uriKey(uri)).also{
        require(it.canonicalFile.parentFile==root.canonicalFile)
    }
    fun fresh(uri:String):File=File(parent(uri),UUID.randomUUID().toString().replace("-","")).also{
        check(!it.exists());require(it.canonicalFile.parentFile==parent(uri).canonicalFile)
    }
    private fun marker(branch:File,name:String,value:String){
        require(hash.matches(value));require(branch.canonicalFile.parentFile?.parentFile==root.canonicalFile)
        check(branch.isDirectory)
        // Partial marker is conservatively ignored; this is not the revision commit record.
        FileOutputStream(File(branch,name)).use{it.write((value+"\n").toByteArray(Charsets.US_ASCII));it.fd.sync()}
    }
    fun recovered(branch:File,baseline:String)=marker(branch,"recovered.sha256",baseline)
    fun saved(branch:File,head:String)=marker(branch,"source-saved.sha256",head)
    private fun readMarker(branch:File,name:String):String?=runCatching{
        val f=File(branch,name);if(!f.isFile || f.length()!=65L)null else f.readText(Charsets.US_ASCII).trim().takeIf{hash.matches(it)}
    }.getOrNull()
    private fun entries(uri:String):List<Triple<String,File,File?>>{
        val key=LocalEditIdentity.uriKey(uri);val prefix="$key-"
        val results=mutableListOf<Triple<String,File,File?>>()
        for(dir in legacy.listFiles().orEmpty()){
            if(dir.isDirectory && dir.name.startsWith(prefix) && hash.matches(dir.name.removePrefix(prefix)) &&
                dir.canonicalFile.parentFile==legacy.canonicalFile){results.add(Triple("v1:"+dir.name.removePrefix(prefix),dir,null))}
        }
        val p=parent(uri)
        for(branch in p.listFiles().orEmpty()){
            if(!branch.isDirectory || !uuid.matches(branch.name) || branch.canonicalFile.parentFile!=p.canonicalFile)continue
            for(dir in branch.listFiles().orEmpty()){
                if(dir.isDirectory && dir.name.startsWith(prefix) && hash.matches(dir.name.removePrefix(prefix)) &&
                    dir.canonicalFile.parentFile==branch.canonicalFile){
                    results.add(Triple("v2:"+branch.name+":"+dir.name.removePrefix(prefix),dir,branch))
                }
            }
        }
        return results
    }
    fun list(uri:String,disk:String?=null,exclude:File?=null,includeClean:Boolean=false):RecoveryListing{
        val found=mutableListOf<LocalRecoveryChoice>();var rejected=0
        for((id,dir,branch)in entries(uri)){
            if(branch!=null && branch.canonicalFile==exclude?.canonicalFile)continue
            try{
                val store=DurablePdfRevisionStore(dir);val h=store.inspect()
                require(h.baselineSha256==dir.name.substringAfterLast('-'))
                // Discovery validates manifest checksums/sequence/size only; resolve() rehashes the PDF.
                val clean=branch?.let{readMarker(it,"source-saved.sha256")==h.sha256} ?: false
                val imported=branch?.let{readMarker(it,"recovered.sha256")==h.baselineSha256} ?: false
                if(h.sha256==disk || (!includeClean && (clean || (h.sha256==h.baselineSha256 && !imported))))continue
                val modified=maxOf(File(dir,"head-0.bin").lastModified(),File(dir,"head-1.bin").lastModified())
                found.add(LocalRecoveryChoice(id,h.sha256,modified,h.sizeBytes,branch==null))
            }catch(_:Exception){rejected++} // Preserve unknown/corrupt material; report it, never adopt it.
        }
        return RecoveryListing(found.sortedWith(compareByDescending<LocalRecoveryChoice>{it.modified}.thenBy{it.id}).distinctBy{it.sha256},rejected)
    }
    fun resolve(uri:String,id:String,expectedHash:String):File{
        require(hash.matches(expectedHash))
        val entry=entries(uri).singleOrNull{it.first==id} ?: error("Recovery namespace unavailable")
        val store=DurablePdfRevisionStore(entry.second);val h=store.recover()
        require(h.baselineSha256==entry.second.name.substringAfterLast('-') && h.sha256==expectedHash){"Recovery changed after selection"}
        return store.currentFile(h)
    }
}
