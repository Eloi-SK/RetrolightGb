package com.eloi.retrolightgb.core.cpu

class CpuRegisters {
    var a: UByte = 0u
    var b: UByte = 0u
    var c: UByte = 0u
    var d: UByte = 0u
    var e: UByte = 0u
    var f: UByte = 0u
    var h: UByte = 0u
    var l: UByte = 0u
    var pc: UShort = 0u
    var sp: UShort = 0u

    var t: Int = 0
    var m: Int = 0
    var lastT: Int = 0
    var lastM: Int = 0
    var imeEnabled: Boolean = false
    var halted: Boolean = false

    val hl: UShort get() = combinateBytes(h, l)
    val bc: UShort get() = combinateBytes(b, c)
    val de: UShort get() = combinateBytes(d, e)

    fun combinateBytes(high: UByte, low: UByte): UShort =
        ((high.toUInt() shl 8) or low.toUInt()).toUShort()

    fun resetFlags() { f = 0u }
    fun setFlag(flag: UByte) { f = f or flag }
    fun unsetFlag(flag: UByte) { f = f and flag.inv() }
    fun isFlagSet(flag: UByte): Boolean = (f and flag) != 0u.toUByte()
}
