// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf

import java.io.*
import java.security.MessageDigest
import java.util.UUID
import org.sg.pdfcolorhighlighter.domain.LocalEditIdentity

class SourceSaveConflict:IOException("Source changed; reopen or use Save As")
data class SourceSaveIntent(val id:String,val uriKey:String,val ownerHash:String,
    val beforeHash:String,val afterHash:String,val beforeBytes:Long,val afterBytes:Long)

/** App-private non-cache storage only. Backups/intents are immutable and never auto-pruned.
 * Published before provider output; matching target bytes link to the ORIGINAL edit owner.
 * A prepared link is NOT a claim of atomic/remote/power-loss-safe completion.
 */
class SourceSaveJournal(directory:File){
    companion object{private val SAVE_LOCK=Any()}
    private val root=directory.canonicalFile
    private val hash=Regex("[0-9a-f]{64}")
    private val idPattern=Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private fun hex(b:ByteArray)=b.joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
    private fun digest(input:InputStream,output:OutputStream?=null):Pair<Long,String>{
        val sha=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536);var total=0L
        while(true){val n=input.read(buffer);if(n<0)break;if(n==0)throw IOException("Non-progressing source")
            if(n.toLong()>VerifiedDocumentExport.MAX_BYTES-total)throw IOException("Source size limit")
            output?.write(buffer,0,n);sha.update(buffer,0,n);total+=n
        }
        require(total>0){"Empty source"};return total to hex(sha.digest())
    }
    fun sourceHash(open:()->InputStream):String=open().use{digest(it).second}
    private fun parent(uri:String)=File(root,LocalEditIdentity.uriKey(uri)).also{require(it.canonicalFile.parentFile==root)}
    private fun operation(uri:String,id:String):File{
        require(idPattern.matches(id));val p=parent(uri)
        return File(p,id).also{require(it.canonicalFile.parentFile==p.canonicalFile)}
    }
    private fun copySynced(file:File,open:()->InputStream):Pair<Long,String>{
        check(!file.exists())
        val value=open().use{i->FileOutputStream(file).use{o->digest(i,o).also{o.fd.sync()}}}
        check(file.inputStream().use{digest(it)}==value){"Backup verification failed"};return value
    }
    private fun validate(r:SourceSaveIntent){
        require(idPattern.matches(r.id) && listOf(r.uriKey,r.ownerHash,r.beforeHash,r.afterHash).all{hash.matches(it)})
        require(r.beforeBytes in 1..VerifiedDocumentExport.MAX_BYTES && r.afterBytes in 1..VerifiedDocumentExport.MAX_BYTES)
    }
    private fun encode(r:SourceSaveIntent):ByteArray{
        validate(r);val bytes=ByteArrayOutputStream()
        DataOutputStream(bytes).use{o->o.writeInt(0x53475356);o.writeInt(1)
            for(s in listOf(r.id,r.uriKey,r.ownerHash,r.beforeHash,r.afterHash))o.writeUTF(s)
            o.writeLong(r.beforeBytes);o.writeLong(r.afterBytes)}
        val body=bytes.toByteArray();return body+MessageDigest.getInstance("SHA-256").digest(body)
    }
    private fun read(dir:File):SourceSaveIntent{
        val f=File(dir,"intent.bin");require(f.isFile && f.length() in 33L..4096L)
        val all=f.readBytes();val body=all.copyOfRange(0,all.size-32)
        require(MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(body),all.copyOfRange(all.size-32,all.size))){"Save intent checksum"}
        val r=DataInputStream(ByteArrayInputStream(body)).use{i->
            require(i.readInt()==0x53475356 && i.readInt()==1)
            SourceSaveIntent(i.readUTF(),i.readUTF(),i.readUTF(),i.readUTF(),i.readUTF(),i.readLong(),i.readLong()).also{require(i.available()==0)}}
        validate(r);require(dir.name==r.id && requireNotNull(dir.parentFile).name==r.uriKey);return r
    }
    fun prepare(uri:String,expected:String,owner:String,local:File,openSource:()->InputStream):SourceSaveIntent{
        require(uri.isNotBlank() && hash.matches(expected) && hash.matches(owner))
        val p=parent(uri);check(p.isDirectory || p.mkdirs())
        val dir=operation(uri,UUID.randomUUID().toString());check(dir.mkdir())
        val before=copySynced(File(dir,"before.pdf"),openSource)
        if(before.second!=expected)throw SourceSaveConflict()
        val after=copySynced(File(dir,"after.pdf")){local.inputStream()}
        val r=SourceSaveIntent(dir.name,LocalEditIdentity.uriKey(uri),owner,before.second,after.second,before.first,after.first)
        val pending=File(dir,".intent.pending")
        FileOutputStream(pending).use{it.write(encode(r));it.fd.sync()}
        check(pending.renameTo(File(dir,"intent.bin")));check(read(dir)==r);return r
    }
    fun beforeFile(uri:String,r:SourceSaveIntent)=verified(uri,r,"before.pdf",r.beforeBytes,r.beforeHash)
    fun afterFile(uri:String,r:SourceSaveIntent)=verified(uri,r,"after.pdf",r.afterBytes,r.afterHash)
    private fun verified(uri:String,r:SourceSaveIntent,name:String,size:Long,sha:String):File{
        require(r.uriKey==LocalEditIdentity.uriKey(uri));val dir=operation(uri,r.id);check(read(dir)==r)
        val file=File(dir,name);require(file.isFile && file.length()==size)
        check(file.inputStream().use{digest(it)}==(size to sha)){"Save backup fingerprint mismatch"};return file
    }
    fun ownerFor(uri:String,current:String):String?{
        require(hash.matches(current));val p=parent(uri);if(!p.exists())return null;require(p.isDirectory)
        val matches=p.listFiles().orEmpty().filter{it.isDirectory && idPattern.matches(it.name) && File(it,"intent.bin").exists()}
            .map{read(it)}.filter{it.afterHash==current}
        if(matches.isEmpty())return null
        val owners=matches.map{it.ownerHash}.distinct();require(owners.size==1){"Ambiguous saved-source owner"}
        matches.forEach{beforeFile(uri,it);afterFile(uri,it)};return owners.single()
    }
    fun save(uri:String,expected:String,owner:String,local:File,readSource:()->InputStream,
             writeSource:()->OutputStream,validatePdf:(File)->Unit):VerifiedDocumentExport.Receipt=synchronized(SAVE_LOCK){
        require(uri.isNotBlank() && hash.matches(expected) && hash.matches(owner))
        if(sourceHash(readSource)!=expected)throw SourceSaveConflict()
        val localHash=local.inputStream().use{digest(it)}
        if(localHash.second==expected){
            if(owner!=expected && ownerFor(uri,expected)!=owner)prepare(uri,expected,owner,local,readSource)
            return@synchronized VerifiedDocumentExport.Receipt(localHash.first,localHash.second)
        }
        val r=prepare(uri,expected,owner,local,readSource)
        check(r.afterHash==localHash.second && r.afterBytes==localHash.first){"Local checkpoint changed during backup"}
        val after=afterFile(uri,r);validatePdf(after);afterFile(uri,r)
        // No generic provider CAS exists: recheck late and warn against concurrent external writers.
        if(sourceHash(readSource)!=expected)throw SourceSaveConflict()
        VerifiedDocumentExport.copy(after,writeSource,readSource).also{check(it.sha256==r.afterHash && it.sizeBytes==r.afterBytes)}
    }
}