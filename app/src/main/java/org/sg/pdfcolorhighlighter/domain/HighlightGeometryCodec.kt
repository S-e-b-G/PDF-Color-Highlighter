// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Bounded ASCII metadata, not executable data or a replacement for standard PDF QuadPoints. */
object HighlightGeometryCodec {
    const val MAX_CHARS=4_000_000
    fun encode(quads:List<GlyphQuad>):String {
        require(quads.size in 1..25000)
        val result="SGQ1|"+quads.joinToString(";"){q->listOf(q.ul.x,q.ul.y,q.ur.x,q.ur.y,q.ll.x,q.ll.y,q.lr.x,q.lr.y).joinToString(",")}
        require(result.length<=MAX_CHARS);return result
    }
    fun decode(value:String):List<GlyphQuad> {
        require(value.length in 6..MAX_CHARS && value.startsWith("SGQ1|"))
        val rows=value.substring(5).split(';',limit=25002);require(rows.size in 1..25000)
        return rows.map{row->val fields=row.split(',',limit=10);require(fields.size==8)
            val p=fields.map{it.toFloat().also{x->require(x.isFinite())}}
            GlyphQuad(TextPoint(p[0],p[1]),TextPoint(p[2],p[3]),TextPoint(p[4],p[5]),TextPoint(p[6],p[7])).also{require(it.area>=.000001)}
        }
    }
}
