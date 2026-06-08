package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.readBoolean
import com.eloi.retrolightgb.core.writeBoolean
import okio.BufferedSink
import okio.BufferedSource

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
    var pendingIme: Boolean = false
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

    fun saveState(sink: BufferedSink) {
        sink.writeByte(a.toInt()); sink.writeByte(b.toInt()); sink.writeByte(c.toInt()); sink.writeByte(d.toInt())
        sink.writeByte(e.toInt()); sink.writeByte(f.toInt()); sink.writeByte(h.toInt()); sink.writeByte(l.toInt())
        sink.writeShort(pc.toInt()); sink.writeShort(sp.toInt())
        sink.writeInt(t); sink.writeInt(m); sink.writeInt(lastT); sink.writeInt(lastM)
        sink.writeBoolean(imeEnabled); sink.writeBoolean(pendingIme); sink.writeBoolean(halted)
    }

    fun loadState(source: BufferedSource) {
        a = source.readByte().toUByte(); b = source.readByte().toUByte(); c = source.readByte().toUByte(); d = source.readByte().toUByte()
        e = source.readByte().toUByte(); f = source.readByte().toUByte(); h = source.readByte().toUByte(); l = source.readByte().toUByte()
        pc = source.readShort().toUShort(); sp = source.readShort().toUShort()
        t = source.readInt(); m = source.readInt(); lastT = source.readInt(); lastM = source.readInt()
        imeEnabled = source.readBoolean(); pendingIme = source.readBoolean(); halted = source.readBoolean()
    }
}
