package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_C
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_H
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_N
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_Z

internal fun Cpu.nop() {
    pc++
    t += 4
    m++
}

internal fun Cpu.rla() {
    val oldCarryFlag = isFlagSet(FLAG_C)
    val carryFlag = (0b1000_0000.toUInt() and a.toUInt()) != 0u
    if (carryFlag) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    var result = a.toUInt() shl 1
    result = result and 0b1111_1110.toUInt()
    if (oldCarryFlag) result = result or 0b0000_0001.toUInt()
    unsetFlag(FLAG_Z)
    unsetFlag(FLAG_N)
    unsetFlag(FLAG_H)
    a = result.toUByte()
    pc++
    t += 4
    m++
}

internal fun Cpu.jr() {
    val offset = memory.readByte((pc + 1u).toUShort()).toByte()
    pc = (pc.toInt() + 2 + offset.toInt()).toUShort()
    t += 12
    m += 3
}

internal fun Cpu.jrZ() {
    val offset = memory.readByte((pc + 1u).toUShort()).toByte()
    if (isFlagSet(FLAG_Z)) {
        pc = (pc.toInt() + 2 + offset.toInt()).toUShort()
        t += 12
        m += 3
    } else {
        pc = (pc + 2u).toUShort()
        t += 8
        m += 2
    }
}

internal fun Cpu.jrNz() {
    val offset = memory.readByte((pc + 1u).toUShort()).toByte()
    if (!isFlagSet(FLAG_Z)) {
        pc = (pc.toInt() + 2 + offset.toInt()).toUShort()
        t += 12
        m += 3
    } else {
        pc = (pc + 2u).toUShort()
        t += 8
        m += 2
    }
}

internal fun Cpu.jpA16() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    pc = combinateBytes(high, low)
    t += 16
    m += 4
}

internal fun Cpu.jpZ() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    val target = combinateBytes(high, low)
    if (isFlagSet(FLAG_Z)) { pc = target; t += 16; m += 4 }
    else { pc = (pc + 3u).toUShort(); t += 12; m += 3 }
}

internal fun Cpu.jpNz() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    val target = combinateBytes(high, low)
    if (!isFlagSet(FLAG_Z)) { pc = target; t += 16; m += 4 }
    else { pc = (pc + 3u).toUShort(); t += 12; m += 3 }
}

internal fun Cpu.jpNc() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    val target = combinateBytes(high, low)
    if (!isFlagSet(FLAG_C)) { pc = target; t += 16; m += 4 }
    else { pc = (pc + 3u).toUShort(); t += 12; m += 3 }
}

internal fun Cpu.jpC() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    val target = combinateBytes(high, low)
    if (isFlagSet(FLAG_C)) { pc = target; t += 16; m += 4 }
    else { pc = (pc + 3u).toUShort(); t += 12; m += 3 }
}

internal fun Cpu.jpHl() {
    pc = hl
    t += 4
    m++
}

internal fun Cpu.call() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    val targetAddress = combinateBytes(high, low)
    val returnAddress = (pc + 3u).toUShort()
    sp--
    memory.writeByte(sp, (returnAddress.toInt() shr 8).toUByte())
    sp--
    memory.writeByte(sp, (returnAddress.toInt() and 0xFF).toUByte())
    pc = targetAddress
    t += 24
    m += 6
}

internal fun Cpu.ret() {
    val low = memory.readByte(sp)
    val high = memory.readByte((sp + 1u).toUShort())
    sp = (sp + 2u).toUShort()
    pc = combinateBytes(high, low)
    t += 16
    m += 4
}

internal fun Cpu.retZ() {
    if (isFlagSet(FLAG_Z)) {
        val low = memory.readByte(sp)
        val high = memory.readByte((sp + 1u).toUShort())
        pc = combinateBytes(high, low)
        sp = (sp + 2u).toUShort()
        t += 20
        m += 5
    } else {
        pc++
        t += 8
        m += 2
    }
}

internal fun Cpu.retNz() {
    if (!isFlagSet(FLAG_Z)) {
        val low = memory.readByte(sp)
        val high = memory.readByte((sp + 1u).toUShort())
        pc = combinateBytes(high, low)
        sp = (sp + 2u).toUShort()
        t += 20
        m += 5
    } else {
        pc++
        t += 8
        m += 2
    }
}

internal fun Cpu.retI() {
    val low = memory.readByte(sp)
    val high = memory.readByte((sp + 1u).toUShort())
    sp = (sp + 2u).toUShort()
    imeEnabled = true
    pc = combinateBytes(high, low)
    t += 16
    m += 4
}

internal fun Cpu.rst(address: UShort) {
    val returnAddress = (pc + 1u).toUShort()
    sp--
    memory.writeByte(sp, (returnAddress.toInt() shr 8).toUByte())
    sp--
    memory.writeByte(sp, (returnAddress.toInt() and 0xFF).toUByte())
    pc = address
    t += 16
    m += 4
}

internal fun Cpu.di() {
    imeEnabled = false
    pc++
    t += 4
    m++
}

internal fun Cpu.ei() {
    imeEnabled = true
    pc++
    t += 4
    m++
}