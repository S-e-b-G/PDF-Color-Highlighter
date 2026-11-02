// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.io.File


object CheckpointFingerprint {
    fun of(file:File):String {
        val hash=java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use{input->val bytes=ByteArray(65536);var total=0L
            while(true){val n=input.read(bytes);if(n<0)break;if(n==0)error("Non-progressing checkpoint")
                total+=n;require(total<=256L*1024*1024);hash.update(bytes,0,n)}
            require(total>0)
        }
        return hash.digest().joinToString(""){(it.toInt() and 255).toString(16).padStart(2,'0')}
    }
}
data class CheckpointChange(val before:File,val after:File,
    val beforeSha256:String=CheckpointFingerprint.of(before),val afterSha256:String=CheckpointFingerprint.of(after))
class CheckpointHistory(private val maximum:Int=16,private val bytes:Long=64L*1024*1024) {
    init{require(maximum>0 && bytes>0)}
    private val done=mutableListOf<CheckpointChange>()
    private val undone=mutableListOf<CheckpointChange>()
    val undo:CheckpointChange? get()=done.lastOrNull()
    val redo:CheckpointChange? get()=undone.lastOrNull()
    val pins:Set<String> get()=(done+undone).flatMap{listOf(it.before.name,it.after.name)}.toSet()
    fun added(change:CheckpointChange){
        done.add(change);undone.clear()
        // Always keep the latest command, even when one very large PDF exceeds the budget.
        while(done.size>1 && (done.size>maximum || done.flatMap{listOf(it.before,it.after)}.distinct().sumOf{it.length()}>bytes))done.removeAt(0)
    }
    fun didUndo(c:CheckpointChange){check(undo==c);done.removeAt(done.lastIndex);undone.add(c)}
    fun didRedo(c:CheckpointChange){check(redo==c);undone.removeAt(undone.lastIndex);done.add(c)}
}
