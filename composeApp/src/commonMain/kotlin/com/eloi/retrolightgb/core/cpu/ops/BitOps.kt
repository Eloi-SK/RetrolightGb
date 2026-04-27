package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_C
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_H
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_N
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_Z

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