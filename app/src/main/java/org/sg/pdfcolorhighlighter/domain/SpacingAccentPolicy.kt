// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.text.Normalizer
import kotlin.math.*

data class SpacingAccentPair(val accent:Int,val base:Int,val quotation:String) {
    val first:Int get()=minOf(accent,base)
    val last:Int get()=maxOf(accent,base)
}
class SpacingAccentLayout(val pairs:List<SpacingAccentPair>,val ignoredSpaces:Set<Int> = emptySet()) {
    private val membership:Map<Int,SpacingAccentPair> = buildMap {
        for(pair in pairs){put(pair.accent,pair);put(pair.base,pair)}
    }
    fun pairAt(index:Int):SpacingAccentPair?=membership[index]
    fun joins(left:Int,right:Int):Boolean {
        val pair=membership[left] ?: return false
        return membership[right]===pair
    }
}
/** Automatic quotation interpretation; never changes real glyph objects, order or geometry. */
object SpacingAccentPolicy {
    private fun combining(cp:Int):Int?=when(cp){
        0x0060,0x02CB -> 0x0300
        0x00B4,0x02CA -> 0x0301
        0x005E,0x02C6 -> 0x0302
        0x007E,0x02DC -> 0x0303
        0x00A8 -> 0x0308
        0x00B8 -> 0x0327
        else -> null
    }
    private fun candidate(glyphs:List<TextGlyph>,accent:Int,base:Int):SpacingAccentPair? {
        if(base !in glyphs.indices)return null
        val mark=glyphs[accent];val letter=glyphs[base]
        if(!mark.unicodeReliable || !letter.unicodeReliable)return null
        val cp=combining(mark.codePoint) ?: return null
        val dotless=letter.codePoint==0x0131
        if(letter.codePoint !in 65..90 && letter.codePoint !in 97..122 && !dotless)return null
        // Typographic dotless-i is restored ONLY inside a unique overlapping accent pair.
        // An ordinary unaccented U+0131 remains U+0131, including Turkish text.
        val baseText=if(dotless)"i"else letter.text
        val composed=Normalizer.normalize(baseText+String(Character.toChars(cp)),Normalizer.Form.NFC)
        if(composed.codePointCount(0,composed.length)!=1 || composed==baseText)return null
        val a=mark.quad.bounds;val b=letter.quad.bounds
        if(a.width<=0f || b.width<=0f || a.height<=0f || b.height<=0f)return null
        val overlap=min(a.right,b.right)-max(a.left,b.left)
        if(overlap<min(a.width,b.width)*.60f)return null
        if(abs((a.left+a.right)/2-(b.left+b.right)/2)>max(a.width,b.width)*.25f)return null
        val h=max(a.height,b.height)
        if(abs(a.top-b.top)>h*.65f || abs(a.bottom-b.bottom)>h*.65f)return null
        if(cp==0x0327){
            if((a.top+a.bottom)/2<(b.top+b.bottom)/2-h*.15f)return null
        }else if((a.top+a.bottom)/2>(b.top+b.bottom)/2+h*.15f)return null
        return SpacingAccentPair(accent,base,composed)
    }
    private fun effectiveBounds(glyphs:List<TextGlyph>,layout:SpacingAccentLayout,index:Int):TextBounds =
        glyphs[layout.pairAt(index)?.base ?: index].quad.bounds
    private fun nearLetterBoundary(glyphs:List<TextGlyph>,layout:SpacingAccentLayout,left:Int,right:Int):Boolean {
        if(left !in glyphs.indices || right !in glyphs.indices)return false
        if(layout.pairAt(left)==null && layout.pairAt(right)==null)return false
        val aGlyph=glyphs[left];val bGlyph=glyphs[right]
        if(!aGlyph.unicodeReliable || !bGlyph.unicodeReliable || aGlyph.whitespace || bGlyph.whitespace)return false
        val aBase=glyphs[layout.pairAt(left)?.base ?: left]
        val bBase=glyphs[layout.pairAt(right)?.base ?: right]
        if(!Character.isLetter(aBase.codePoint) || !Character.isLetter(bBase.codePoint))return false
        val a=effectiveBounds(glyphs,layout,left);val b=effectiveBounds(glyphs,layout,right)
        if(a.height<=0f || b.height<=0f)return false
        val h=max(a.height,b.height)
        if(h/min(a.height,b.height)>1.75f || abs(a.top-b.top)>h*.25f)return false
        // True word gaps in the supplied justified text exceed this tight contact tolerance.
        return b.left-a.right in (-.10f*h)..(.10f*h)
    }
    fun prepare(glyphs:List<TextGlyph>):SpacingAccentLayout {
        val proposed=mutableListOf<SpacingAccentPair>()
        for(index in glyphs.indices){
            if(combining(glyphs[index].codePoint)==null)continue
            val candidates=listOfNotNull(candidate(glyphs,index,index-1),candidate(glyphs,index,index+1))
            if(candidates.size==1)proposed.add(candidates.single())
        }
        val counts=mutableMapOf<Int,Int>()
        for(p in proposed){counts[p.accent]=(counts[p.accent] ?: 0)+1;counts[p.base]=(counts[p.base] ?: 0)+1}
        val pairs=proposed.filter{counts[it.accent]==1 && counts[it.base]==1}
        val layout=SpacingAccentLayout(pairs)
        val ignored=mutableSetOf<Int>()
        for(index in 1 until glyphs.lastIndex){
            val g=glyphs[index]
            // Native SYNTHETIC ASCII space only. Explicit spaces, NBSP and unknown provenance stay.
            if(g.codePoint==32 && g.synthetic && g.unicodeReliable &&
                nearLetterBoundary(glyphs,layout,index-1,index+1))ignored.add(index)
        }
        return SpacingAccentLayout(pairs,ignored.toSet())
    }
    fun ignoredSyntheticSpace(line:PageTextLine,index:Int):Boolean=index in line.spacingAccents.ignoredSpaces
    fun inferredSpaceBefore(line:PageTextLine,index:Int):Boolean {
        if(ignoredSyntheticSpace(line,index) || ignoredSyntheticSpace(line,index-1))return false
        return SelectionUnicodePolicy.inferredSpaceBefore(line,index) &&
            !line.spacingAccents.joins(index-1,index) &&
            !nearLetterBoundary(line.glyphs,line.spacingAccents,index-1,index)
    }
    fun quotation(line:PageTextLine,first:Int,last:Int):String {
        require(first>=0 && last<line.glyphs.size && first<=last)
        return buildString {
            for(index in first..last){
                if(index>first && index<last && ignoredSyntheticSpace(line,index))continue
                val pair=line.spacingAccents.pairAt(index)
                if(pair!=null && pair.first>=first && pair.last<=last){
                    if(index!=pair.first)continue
                    if(isNotEmpty() && inferredSpaceBefore(line,index))append(' ')
                    append(pair.quotation)
                }else{
                    if(isNotEmpty() && inferredSpaceBefore(line,index))append(' ')
                    append(line.glyphs[index].text)
                }
            }
        }
    }
}
