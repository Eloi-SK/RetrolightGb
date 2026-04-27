package com.eloi.retrolightgb.core.cpu.ops

import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_C
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_H
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_N
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_Z

internal fun Cpu.rlcRegister(value: UByte): UByte {
    val bit7 = (value.toInt() shr 7) and 0x01
    val result = ((value.toInt() shl 1) or bit7) and 0xFF
    resetFlags()
    if (bit7 != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result.toUByte()
}

internal fun Cpu.rlcHl() {
    val value = memory.readByte(hl)
    val bit7 = (value.toInt() shr 7) and 0x01
    val result = ((value.toInt() shl 1) or bit7) and 0xFF
    resetFlags()
    if (bit7 != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    memory.writeByte(hl, result.toUByte())
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.rrcRegister(value: UByte): UByte {
    val bit0 = value.toInt() and 0x01
    val result = ((value.toInt() ushr 1) or (bit0 shl 7)) and 0xFF
    resetFlags()
    if (bit0 != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result.toUByte()
}

internal fun Cpu.rrcHl() {
    val value = memory.readByte(hl)
    val bit0 = value.toInt() and 0x01
    val result = ((value.toInt() ushr 1) or (bit0 shl 7)) and 0xFF
    resetFlags()
    if (bit0 != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    memory.writeByte(hl, result.toUByte())
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.rlRegister(value: UByte): UByte {
    val oldCarry = if (isFlagSet(FLAG_C)) 1 else 0
    val newCarry = (value.toInt() shr 7) and 0x01
    val result = ((value.toInt() shl 1) or oldCarry) and 0xFF
    if (newCarry == 0) unsetFlag(FLAG_C) else setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    unsetFlag(FLAG_N); unsetFlag(FLAG_H)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result.toUByte()
}

internal fun Cpu.rlHl() {
    val value = memory.readByte(hl)
    val oldCarry = if (isFlagSet(FLAG_C)) 1 else 0
    val newCarry = (value.toInt() shr 7) and 0x01
    val result = ((value.toInt() shl 1) or oldCarry) and 0xFF
    if (newCarry == 0) unsetFlag(FLAG_C) else setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    unsetFlag(FLAG_N); unsetFlag(FLAG_H)
    memory.writeByte(hl, result.toUByte())
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.rrRegister(value: UByte): UByte {
    val oldCarry = if (isFlagSet(FLAG_C)) 1 else 0
    val newCarry = value.toInt() and 0x01
    val result = ((value.toInt() ushr 1) or (oldCarry shl 7)) and 0xFF
    if (newCarry == 0) unsetFlag(FLAG_C) else setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    unsetFlag(FLAG_N); unsetFlag(FLAG_H)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result.toUByte()
}

internal fun Cpu.rrHl() {
    val value = memory.readByte(hl)
    val oldCarry = if (isFlagSet(FLAG_C)) 1 else 0
    val newCarry = value.toInt() and 0x01
    val result = ((value.toInt() ushr 1) or (oldCarry shl 7)) and 0xFF
    if (newCarry == 0) unsetFlag(FLAG_C) else setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    unsetFlag(FLAG_N); unsetFlag(FLAG_H)
    memory.writeByte(hl, result.toUByte())
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.slaRegister(value: UByte): UByte {
    val carry = (value.toInt() shr 7) and 0x01
    val result = (value.toInt() shl 1) and 0xFF
    resetFlags()
    if (carry != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result.toUByte()
}

internal fun Cpu.slaHl() {
    val value = memory.readByte(hl)
    val carry = (value.toInt() shr 7) and 0x01
    val result = (value.toInt() shl 1) and 0xFF
    resetFlags()
    if (carry != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    memory.writeByte(hl, result.toUByte())
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.sraRegister(value: UByte): UByte {
    val carry = value.toInt() and 0x01
    val result = ((value.toInt() ushr 1) or (value.toInt() and 0x80)) and 0xFF
    resetFlags()
    if (carry != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result.toUByte()
}

internal fun Cpu.sraHl() {
    val value = memory.readByte(hl)
    val carry = value.toInt() and 0x01
    val result = ((value.toInt() ushr 1) or (value.toInt() and 0x80)) and 0xFF
    resetFlags()
    if (carry != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    memory.writeByte(hl, result.toUByte())
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.srlRegister(value: UByte): UByte {
    val carry = value.toInt() and 0x01
    val result = (value.toInt() ushr 1) and 0xFF
    resetFlags()
    if (carry != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result.toUByte()
}

internal fun Cpu.srlHl() {
    val value = memory.readByte(hl)
    val carry = value.toInt() and 0x01
    val result = (value.toInt() ushr 1) and 0xFF
    resetFlags()
    if (carry != 0) setFlag(FLAG_C)
    if (result == 0) setFlag(FLAG_Z)
    memory.writeByte(hl, result.toUByte())
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.bitNumberHl(bit: Int) {
    val bitValue = (memory.readByte(hl).toInt() shr bit) and 0x01
    setFlag(FLAG_H); unsetFlag(FLAG_N)
    if (bitValue == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.bitNumberRegister(bit: Int, value: UByte) {
    val bitValue = (value.toInt() shr bit) and 0x01
    setFlag(FLAG_H); unsetFlag(FLAG_N)
    if (bitValue == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.resetBitRegister(bit: Int, value: UByte): UByte {
    val result = (value.toInt() and ((1 shl bit).inv() and 0xFF)).toUByte()
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result
}

internal fun Cpu.resetBitHl(bit: Int) {
    val result = (memory.readByte(hl).toInt() and ((1 shl bit).inv() and 0xFF)).toUByte()
    memory.writeByte(hl, result)
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.setBitRegister(bit: Int, value: UByte): UByte {
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return (value.toInt() or (1 shl bit)).toUByte()
}

internal fun Cpu.setBitHl(bit: Int) {
    val result = (memory.readByte(hl).toInt() or (1 shl bit)).toUByte()
    memory.writeByte(hl, result)
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}

internal fun Cpu.swapRegister(value: UByte): UByte {
    val result = (((value.toUInt() shl 4) or (value.toUInt() shr 4)) and 0xFFu).toUByte()
    resetFlags()
    if (result == 0u.toUByte()) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return result
}

internal fun Cpu.swapHl() {
    val value = memory.readByte(hl)
    val result = ((value.toUInt() shl 4) or (value.toUInt() shr 4)) and 0xFFu
    memory.writeByte(hl, result.toUByte())
    resetFlags()
    if (result == 0u) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 16; m += 4
}