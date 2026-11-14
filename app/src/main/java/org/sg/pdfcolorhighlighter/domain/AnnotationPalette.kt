// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

/** Preference slots and saved native PDF RGB are distinct identities. No alpha in these values. */
data class AnnotationPalette(val colors:List<Int>) {
    init { require(colors.size==6 && colors.all{it in 0..0xffffff} && colors.distinct().size==6) }
    operator fun get(slot:Int)=colors[slot]
    fun match(rgb:Int):Int?=colors.indexOf(rgb).takeIf{it>=0}
    companion object {
        val DEFAULT=AnnotationPalette(listOf(0xFFAD5B,0x00FF00,0xFC3636,0xFFFF00,0xD1DFEB,0xFCD7FB))
        fun parse(value:String):Int? {
            val s=value.trim().removePrefix("#")
            return if(Regex("[0-9A-Fa-f]{6}").matches(s))s.toInt(16)else null
        }
        fun hex(rgb:Int):String { require(rgb in 0..0xffffff);return rgb.toString(16).uppercase(java.util.Locale.ROOT).padStart(6,'0') }
        fun draft(values:List<String>):AnnotationPalette? {
            if(values.size!=6)return null
            val parsed=values.map{parse(it) ?: return null}
            return runCatching{AnnotationPalette(parsed)}.getOrNull()
        }
        // Canonical null preserves legacy constructors and default-palette copy(slot=...) calls.
        fun storedColor(rgb:Int,slot:Int):Int?=rgb.takeUnless{it==DEFAULT[slot]}
        fun decode(bytes:ByteArray):AnnotationPalette {
            require(bytes.size<=128)
            val t=bytes.toString(Charsets.US_ASCII)
            require(t.startsWith("SGPALETTE1\n") && t.endsWith("\n"))
            return requireNotNull(draft(t.removePrefix("SGPALETTE1\n").removeSuffix("\n").split('\n')))
        }
        fun encode(p:AnnotationPalette)=("SGPALETTE1\n"+p.colors.joinToString("\n"){hex(it)}+"\n").toByteArray(Charsets.US_ASCII)
    }
}
