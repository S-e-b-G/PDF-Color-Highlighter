// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Reviewed legacy circle-marker profile, never a blanket replacement-character conversion. */
object KnownSymbolPolicy {
    fun normalize(codePoint:Int,fontName:String?,unicodeReliable:Boolean,atLineStart:Boolean=false):Int {
        if(!unicodeReliable || fontName==null || (codePoint!=0x000F && codePoint!=0xFFFD))return codePoint
        val name=if(fontName.length>7 && fontName[6]=='+' && fontName.take(6).all{it in 'A'..'Z'})
            fontName.substring(7) else fontName
        if(name!="AdvP4C4E74")return codePoint
        // Keep the reviewed /C15 control mapping. Do not reinterpret other controls.
        if(codePoint==0x000F)return 0x25CF
        // Native MuPDF may expose a circle from this family as U+FFFD, without CID/GID flags.
        // Its filled/hollow identity is then unavailable: normalize a leading marker to a
        // generic bullet, rather than pretending the exact source glyph was recovered.
        // Unknown/interior replacement characters remain explicit selection refusals.
        return if(atLineStart)0x2022 else codePoint
    }
}
