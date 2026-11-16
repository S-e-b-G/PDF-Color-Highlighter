// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.pdf
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract as D
import android.provider.OpenableColumns

/** Real persisted tree access. No source/path guess, overwrite, automatic deletion or rebind. */
class SafFallbackTarget(private val app:Context,private val tree:Uri):FallbackTarget {
    private val resolver get()=app.contentResolver
    override fun authorized()=tree.scheme=="content" && D.isTreeUri(tree) && resolver.persistedUriPermissions.any{it.uri==tree && it.isReadPermission && it.isWritePermission}
    override fun createNew(baseName:String,original:String):String {
        check(authorized());val root=D.buildDocumentUriUsingTree(tree,D.getTreeDocumentId(tree))
        val children=D.buildChildDocumentsUriUsingTree(tree,D.getTreeDocumentId(tree))
        val names=hashSetOf<String>();val ids=hashSetOf<String>();var rows=0
        resolver.query(children,arrayOf(D.Document.COLUMN_DOCUMENT_ID,D.Document.COLUMN_DISPLAY_NAME),null,null,null)?.use{c->
            while(c.moveToNext()){check(++rows<=4096 && !c.isNull(0));ids.add(c.getString(0));if(!c.isNull(1))names.add(c.getString(1))}
        } ?: error("Cannot inspect authorized folder")
        val name=AutomaticFallback.freshName(baseName,names);check(authorized())
        val made=D.createDocument(resolver,root,"application/pdf",name) ?: error("No new fallback document")
        val id=D.getDocumentId(made);val source=Uri.parse(original)
        check(id !in ids && made!=source)
        if(made.authority==source.authority && D.isDocumentUri(app,source))check(id!=D.getDocumentId(source))
        check(D.isChildDocument(resolver,root,made))
        resolver.query(made,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->check(c.moveToFirst() && c.getString(0)==name)} ?: error("Cannot inspect fallback result")
        return made.toString()
    }
    override fun read(id:String):java.io.InputStream {check(authorized());return resolver.openInputStream(Uri.parse(id)) ?: error("No fallback input")}
    override fun write(id:String):java.io.OutputStream {check(authorized());return resolver.openOutputStream(Uri.parse(id),"wt") ?: error("No fallback output")}
}
