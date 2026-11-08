// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

data class DocumentAppearancePlan(val baseHash:String,val totalPages:Int,
    val changes:List<Pair<ManagedHighlight,ManagedHighlight>>){
    val highlights:Int get()=changes.size
    val affectedPages:Int get()=changes.map{it.first.edit.page}.distinct().size
}
data class DocumentAppearanceReview(val id:String,val totalPages:Int,val highlights:Int,val affectedPages:Int)
object DocumentAppearanceRepair{
    const val MAX_ITEMS=5000
    private val hash=Regex("[0-9a-f]{64}")
    fun plan(baseHash:String,totalPages:Int,items:List<ManagedHighlight>):DocumentAppearancePlan{
        require(hash.matches(baseHash) && totalPages>0 && items.size<=MAX_ITEMS)
        require(items.all{it.edit.page in 0 until totalPages})
        require(items.map{it.edit.page to it.edit.id}.distinct().size==items.size)
        val changes=items.mapNotNull{before->
            val paint=NativeHighlightAppearance.forQuads(before.edit.quads)
            val after=before.copy(edit=before.edit.copy(appearance=paint))
            if(NativeAppearanceEquivalence.same(before.edit.appearance,paint))null else before to after
        }
        return DocumentAppearancePlan(baseHash,totalPages,changes).also{verify(it,baseHash)}
    }
    fun verify(plan:DocumentAppearancePlan,currentHash:String){
        require(hash.matches(plan.baseHash) && plan.baseHash==currentHash){"Repair review is stale"}
        require(plan.totalPages>0 && plan.changes.size<=MAX_ITEMS)
        require(plan.changes.map{it.first.edit.page to it.first.edit.id}.distinct().size==plan.changes.size)
        for((before,after)in plan.changes){
            require(before.edit.page in 0 until plan.totalPages)
            require(after==before.copy(edit=before.edit.copy(appearance=after.edit.appearance))){"Only appearance may change"}
        }
    }
}
