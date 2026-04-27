package com.eloi.retrolightgb.core.cpu.ops

import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_C
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_H
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_N
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_Z

internal fun Cpu.nop() {
    pc++
    t += 4
    m++
}

internal fun Cpu.rlca() {
    val bit7 = (a.toInt() shr 7) and 0x01
    a = ((a.toInt() shl 1) or bit7).toUByte()
    resetFlags()
    if (bit7 != 0) setFlag(FLAG_C)
    pc++; t += 4; m++
}

internal fun Cpu.rrca() {
    val bit0 = a.toInt() and 0x01
    a = ((a.toInt() ushr 1) or (bit0 shl 7)).toUByte()
    resetFlags()
    if (bit0 != 0) setFlag(FLAG_C)
    pc++; t += 4; m++
}

internal fun Cpu.rra() {
    val oldCarry = if (isFlagSet(FLAG_C)) 1 else 0
    val newCarry = a.toInt() and 0x01
    a = ((a.toInt() ushr 1) or (oldCarry shl 7)).toUByte()
    resetFlags()
    if (newCarry != 0) setFlag(FLAG_C)
    pc++; t += 4; m++
}

internal fun Cpu.stop() {
    pc = (pc + 2u).toUShort(); t += 4; m++
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

private fun Cpu.jrIf(condition: Boolean) {
    val offset = memory.readByte((pc + 1u).toUShort()).toByte()
    if (condition) {
        pc = (pc.toInt() + 2 + offset.toInt()).toUShort()
        t += 12; m += 3
    } else {
        pc = (pc + 2u).toUShort()
        t += 8; m += 2
    }
}

internal fun Cpu.jrZ()  = jrIf(isFlagSet(FLAG_Z))
internal fun Cpu.jrNz() = jrIf(!isFlagSet(FLAG_Z))
internal fun Cpu.jrC()  = jrIf(isFlagSet(FLAG_C))
internal fun Cpu.jrNc() = jrIf(!isFlagSet(FLAG_C))

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

private fun Cpu.callIf(condition: Boolean) {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    if (condition) {
        val targetAddress = combinateBytes(high, low)
        val returnAddress = (pc + 3u).toUShort()
        sp--; memory.writeByte(sp, (returnAddress.toInt() shr 8).toUByte())
        sp--; memory.writeByte(sp, (returnAddress.toInt() and 0xFF).toUByte())
        pc = targetAddress
        t += 24; m += 6
    } else {
        pc = (pc + 3u).toUShort()
        t += 12; m += 3
    }
}

internal fun Cpu.callNz() = callIf(!isFlagSet(FLAG_Z))
internal fun Cpu.callZ()  = callIf(isFlagSet(FLAG_Z))
internal fun Cpu.callNc() = callIf(!isFlagSet(FLAG_C))
internal fun Cpu.callC()  = callIf(isFlagSet(FLAG_C))

internal fun Cpu.ret() {
    val low = memory.readByte(sp)
    val high = memory.readByte((sp + 1u).toUShort())
    sp = (sp + 2u).toUShort()
    pc = combinateBytes(high, low)
    t += 16
    m += 4
}

private fun Cpu.retIf(condition: Boolean) {
    if (condition) {
        val low = memory.readByte(sp)
        val high = memory.readByte((sp + 1u).toUShort())
        pc = combinateBytes(high, low)
        sp = (sp + 2u).toUShort()
        t += 20; m += 5
    } else {
        pc++
        t += 8; m += 2
    }
}

internal fun Cpu.retZ()  = retIf(isFlagSet(FLAG_Z))
internal fun Cpu.retNz() = retIf(!isFlagSet(FLAG_Z))
internal fun Cpu.retNc() = retIf(!isFlagSet(FLAG_C))

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

internal fun Cpu.halt() {
    halted = true
    pc++; t += 4; m++
}

internal fun Cpu.decSp() { sp--; pc++; t += 8; m += 2 }

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