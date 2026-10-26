// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Bounded symbol-only data, opened/reviewed by the user. No ordinary text/font-ID export. */
object ExceptionalGlyphDiagnostics {
    const val MAX_SAMPLES=8
    private fun hex(cp:Int)="U+"+cp.toString(16).uppercase().padStart(4,'0')
    private fun geometry(line:PageTextLine,index:Int):String =
        listOf(index-1,index+1).filter{it in line.glyphs.indices}.joinToString(",") { other ->
            val a=line.glyphs[index].quad.bounds;val b=line.glyphs[other].quad.bounds
            val h=maxOf(a.height,b.height).coerceAtLeast(.001f)
            val overlap=minOf(a.right,b.right)-maxOf(a.left,b.left)
            val dx=((a.left+a.right)-(b.left+b.right))/2/h
            val dy=((a.top+a.bottom)-(b.top+b.bottom))/2/h
            java.lang.String.format(java.util.Locale.ROOT,"g%d(dx/h=%.2f,dy/h=%.2f,ov/h=%.2f)",other,dx,dy,overlap/h)
        }
    fun describe(line:PageTextLine):String {
        val samples=mutableListOf<String>();var total=0
        for((index,g) in line.glyphs.withIndex()){
            val label=when{
                !g.unicodeReliable -> "unmapped-ID-hidden"
                g.codePoint<32 && g.codePoint!=9 -> hex(g.codePoint)+"(control)"
                g.codePoint==127 -> hex(g.codePoint)+"(control)"
                g.codePoint==0xFFFD -> "U+FFFD(replacement)"
                g.codePoint==0x200D -> "U+200D(joiner)"
                g.codePoint in 0x1F1E6..0x1F1FF -> "regional-indicator"
                SelectionUnicodePolicy.spacingAccent(g.codePoint) || g.codePoint==94 || g.codePoint==126 -> {
                    val pair=line.spacingAccents.pairAt(index)
                    hex(g.codePoint)+(if(pair!=null)"(paired:g${pair.base})"else"(unpaired:"+geometry(line,index)+")")
                }
                else -> null
            }
            if(label!=null){total++;if(samples.size<MAX_SAMPLES)samples.add("g$index=$label")}
        }
        if(total>MAX_SAMPLES)samples.add("+${total-MAX_SAMPLES}")
        return samples.joinToString("; ")
    }
}
