// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain
import kotlin.math.abs

/** Appearance no-op detection ONLY. Never use for logical quads, stale-state checks or source hashes. */
object NativeAppearanceEquivalence{
    const val TOLERANCE_PT=.001f
    private fun same(a:TextPoint,b:TextPoint)=abs(a.x-b.x)<=TOLERANCE_PT && abs(a.y-b.y)<=TOLERANCE_PT
    fun same(a:List<GlyphQuad>,b:List<GlyphQuad>):Boolean{
        if(a.size!=b.size)return false
        for(i in a.indices){val x=a[i];val y=b[i]
            if(!same(x.ul,y.ul) || !same(x.ur,y.ur) || !same(x.ll,y.ll) || !same(x.lr,y.lr))return false
        }
        return true
    }
}
