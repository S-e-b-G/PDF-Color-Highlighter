// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import java.lang.ref.WeakReference
import kotlin.math.*

/** Conservative automatic preview quotation assembly. NEVER apply to manually edited PDF Contents. */
object SelectionQuotationPolicy{
    private enum class Marker{BULLET,DASH,STAR,NUMBER,LETTER}
    private const val MAX_PREPARED_LINES=2048
    private val numbered=Regex("""^(?:\d{1,3}[.)]|\(\d{1,3}\))\s+""")
    private val lettered=Regex("""^(?:[A-Za-z][.)]|\([A-Za-z]\))\s+""")
    private var prepared=WeakReference<Prepared>(null)
    private fun marker(line:PageTextLine):Marker?{
        if(!line.selectable || line.glyphs.take(64).any{SelectionUnicodePolicy.unsupported(it)})return null
        val prefix=buildString{
            line.glyphs.take(64).forEachIndexed{i,g->if(SelectionUnicodePolicy.inferredSpaceBefore(line,i))append(' ');append(g.text)}
        }.replace('\u00A0',' ').replace('\u202F',' ').trimStart()
        if(prefix.isEmpty())return null
        if(SelectionUnicodePolicy.bullet(prefix.codePointAt(0)))return Marker.BULLET
        if(prefix.length>1 && prefix[1].isWhitespace()){
            if(prefix.first() in "-–—")return Marker.DASH
            if(prefix.first()=='*')return Marker.STAR
        }
        if(numbered.containsMatchIn(prefix))return Marker.NUMBER
        if(lettered.containsMatchIn(prefix))return Marker.LETTER
        return null
    }
    private fun sane(b:TextBounds)=b.width.isFinite() && b.height.isFinite() && b.width>0f && b.height>0f
    private fun column(a:TextBounds,b:TextBounds):Boolean{
        if(!sane(a) || !sane(b))return false
        val h=max(a.height,b.height)
        if(h/min(a.height,b.height)>1.75f)return false
        val overlap=min(a.right,b.right)-max(a.left,b.left)
        return overlap>=min(a.width,b.width)*.2f || abs(a.left-b.left)<=h*1.5f
    }
    private class Prepared(val owner:PageTextSnapshot){
        private val bounded=owner.lines.size<=MAX_PREPARED_LINES
        private val bounds=if(bounded)owner.lines.map{it.bounds}else emptyList()
        private val markers=if(bounded)owner.lines.map{SelectionQuotationPolicy.marker(it)}else emptyList()
        private val items:BooleanArray=if(bounded)BooleanArray(owner.lines.size)else BooleanArray(0)
        private val listBlocks=mutableSetOf<Pair<Int,Int>>()
        private val normalGap:Float?
        init{
            if(bounded){
                for(i in owner.lines.indices){
                    val kind=markers[i] ?: continue;val a=bounds[i]
                    items[i]=kind==Marker.BULLET || owner.lines.indices.any{j->
                        i!=j && markers[j]==kind && SelectionQuotationPolicy.column(a,bounds[j]) &&
                            abs(a.left-bounds[j].left)<=max(a.height,bounds[j].height)*1.5f &&
                            abs(a.top-bounds[j].top)<=max(a.height,bounds[j].height)*20f
                    }
                    if(items[i])listBlocks.add(owner.lines[i].block to owner.lines[i].run)
                }
            }
            val samples=mutableListOf<Float>()
            fun sample(i:Int,j:Int){
                val a=bounds[i];val b=bounds[j]
                if(!owner.lines[i].selectable || !owner.lines[j].selectable || !SelectionQuotationPolicy.column(a,b))return
                val h=max(a.height,b.height);val gap=(b.top-a.bottom)/h
                if(b.top>a.top+min(a.height,b.height)*.35f && gap in 0f..2.5f)samples.add(gap)
            }
            if(bounded){
                owner.lines.indices.groupBy{owner.lines[it].block to owner.lines[it].run}.values.forEach{group->
                    group.sortedBy{bounds[it].top}.zipWithNext().forEach{(a,b)->sample(a,b)}
                }
                if(samples.size<3){
                    samples.clear()
                    for(i in owner.lines.indices){
                        val a=bounds[i]
                        if(!sane(a) || !owner.lines[i].selectable)continue
                        val next=owner.lines.indices.filter{j->
                            j!=i && owner.lines[j].selectable && bounds[j].top>a.top+a.height*.35f &&
                                bounds[j].top<=a.bottom+a.height*4f && SelectionQuotationPolicy.column(a,bounds[j])
                        }.minByOrNull{bounds[it].top}
                        if(next!=null)sample(i,next)
                    }
                }
            }
            normalGap=if(samples.size>=3)samples.sorted()[samples.size/2]else null
        }
        fun separator(previous:Int,next:Int):String{
            if(!bounded)return if(SelectionQuotationPolicy.marker(owner.lines[next])==Marker.BULLET)"\n" else ""
            if(items[next])return "\n"
            val p=owner.lines[previous];val n=owner.lines[next];val a=bounds[previous];val b=bounds[next]
            if(!SelectionQuotationPolicy.column(a,b))return ""
            val h=max(a.height,b.height);val gap=(b.top-a.bottom)/h
            if(gap<=0f)return ""
            val listExitGap=max(.75f,(normalGap ?: .5f)+.4f)
            if(p.block!=n.block && (p.block to p.run) in listBlocks && gap>=listExitGap)return "\n\n"
            val signal=abs(b.left-a.left)>=.5f*h || a.width<b.width*.85f
            val threshold:Float=when{
                p.block==n.block -> max(1.2f,(normalGap ?: .5f)+.8f)
                normalGap!=null && signal -> max(.4f,normalGap+.3f)
                normalGap!=null -> max(.75f,normalGap+.5f)
                signal -> .75f
                else -> 1f
            }
            return if(gap>=threshold) "\n\n" else ""
        }
    }
    @Synchronized private fun prepare(snapshot:PageTextSnapshot):Prepared{
        val old=prepared.get();if(old!=null && old.owner===snapshot)return old
        return Prepared(snapshot).also{prepared=WeakReference(it)}
    }
    fun compose(snapshot:PageTextSnapshot,lineIndices:List<Int>,visualLines:List<String>):String{
        require(lineIndices.size==visualLines.size && lineIndices.all{it in snapshot.lines.indices})
        if(visualLines.isEmpty())return ""
        val policy=prepare(snapshot);val result=StringBuilder();var group=mutableListOf(visualLines.first())
        for(index in 1 until visualLines.size){
            val separator=policy.separator(lineIndices[index-1],lineIndices[index])
            if(separator.isNotEmpty()){
                result.append(QuotationNormalizer.fromParagraphs(listOf(group)));result.append(separator)
                group=mutableListOf()
            }
            group.add(visualLines[index])
        }
        result.append(QuotationNormalizer.fromParagraphs(listOf(group)))
        return result.toString()
    }
}
