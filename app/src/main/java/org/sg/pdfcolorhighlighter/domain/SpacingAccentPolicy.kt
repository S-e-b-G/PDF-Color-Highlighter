// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.text.Normalizer
import kotlin.math.*

/** A quotation interpretation over real copied glyphs. Never changes glyph order or geometry. */
data class SpacingAccentPair(val accent:Int,val base:Int,val quotation:String) {
    val first:Int get()=minOf(accent,base)
    val last:Int get()=maxOf(accent,base)
}
class SpacingAccentLayout(val pairs:List<SpacingAccentPair>) {
    private val membership:Map<Int,SpacingAccentPair> = buildMap {
        for(pair in pairs){put(pair.accent,pair);put(pair.base,pair)}
    }
    fun pairAt(index:Int):SpacingAccentPair?=membership[index]
    fun joins(left:Int,right:Int):Boolean {
        val pair=membership[left] ?: return false
        return membership[right]===pair
    }
}
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
        // Deliberately bounded Latin reconstruction, not arbitrary script/font repair.
        if(letter.codePoint !in 65..90 && letter.codePoint !in 97..122)return null
        val cp=combining(mark.codePoint) ?: return null
        val composed=Normalizer.normalize(letter.text+String(Character.toChars(cp)),Normalizer.Form.NFC)
        if(composed.codePointCount(0,composed.length)!=1 || composed==letter.text)return null
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
    fun prepare(glyphs:List<TextGlyph>):SpacingAccentLayout {
        val proposed=mutableListOf<SpacingAccentPair>()
        for(index in glyphs.indices){
            if(combining(glyphs[index].codePoint)==null)continue
            // Adjacent logical glyphs only. Never consume actual whitespace to find a base.
            val candidates=listOfNotNull(candidate(glyphs,index,index-1),candidate(glyphs,index,index+1))
            if(candidates.size==1)proposed.add(candidates.single())
        }
        val counts=mutableMapOf<Int,Int>()
        for(p in proposed){counts[p.accent]=(counts[p.accent] ?: 0)+1;counts[p.base]=(counts[p.base] ?: 0)+1}
        return SpacingAccentLayout(proposed.filter{counts[it.accent]==1 && counts[it.base]==1})
    }
    fun inferredSpaceBefore(line:PageTextLine,index:Int):Boolean =
        SelectionUnicodePolicy.inferredSpaceBefore(line,index) && !line.spacingAccents.joins(index-1,index)

    fun quotation(line:PageTextLine,first:Int,last:Int):String {
        require(first>=0 && last<line.glyphs.size && first<=last)
        return buildString {
            for(index in first..last){
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
