package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_C
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_H
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_N
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_Z

internal fun Cpu.incRegister(value: UByte): UByte {
    val new = (value + 1u).toUByte()
    unsetFlag(FLAG_N)
    if (new == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (new and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    pc++; t += 4; m++
    return new
}

internal fun Cpu.decRegister(value: UByte): UByte {
    val new = (value - 1u).toUByte()
    setFlag(FLAG_N)
    if (new == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (value and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    pc++; t += 4; m++
    return new
}

internal fun Cpu.incRegisterPair(value: UShort): Pair<UByte, UByte> {
    val new = value + 1u
    pc++; t += 8; m += 2
    return (new shr 8).toUByte() to (new and 0xFFu).toUByte()
}

internal fun Cpu.decRegisterPair(value: UShort): Pair<UByte, UByte> {
    val new = value - 1u
    pc++; t += 8; m += 2
    return (new shr 8).toUByte() to (new and 0xFFu).toUByte()
}

internal fun Cpu.incHlAddress() {
    val value = memory.readByte(hl)
    val new = (value + 1u).toUByte()
    memory.writeByte(hl, new)
    unsetFlag(FLAG_N)
    if (new == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if ((value and 0x0Fu) == 0x0Fu.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    pc++; t += 12; m += 3
}

internal fun Cpu.incSp() { sp++; pc++; t += 8; m += 2 }

internal fun Cpu.cpl() {
    a = a.inv()
    setFlag(FLAG_N); setFlag(FLAG_H)
    pc++; t += 4; m++
}

internal fun Cpu.xorARegister(value: UByte) {
    a = a xor value
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    pc++; t += 4; m++
}

internal fun Cpu.orARegister(value: UByte) {
    a = a or value
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    pc++; t += 4; m++
}

internal fun Cpu.andARegister(value: UByte) {
    a = a and value
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    setFlag(FLAG_H)
    pc++; t += 4; m++
}

internal fun Cpu.andAN8() {
    val value = memory.readByte((pc + 1u).toUShort())
    a = a and value
    if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    unsetFlag(FLAG_N); setFlag(FLAG_H); unsetFlag(FLAG_C)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.addARegister(value: UByte) {
    val result = a.toInt() + value.toInt()
    resetFlags()
    if ((result and 0xFF) == 0) setFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) + (value.toInt() and 0x0F)) > 0x0F) setFlag(FLAG_H)
    if (result > 0xFF) setFlag(FLAG_C)
    a = result.toUByte()
    pc++; t += 4; m++
}

internal fun Cpu.addAHl() {
    val value = memory.readByte(hl)
    val result = a.toInt() + value.toInt()
    resetFlags()
    if ((result and 0xFF) == 0) setFlag(FLAG_Z)
    if (result > 0xFF) setFlag(FLAG_C)
    if (((a.toInt() and 0x0F) + (value.toInt() and 0x0F)) > 0x0F) setFlag(FLAG_H)
    a = result.toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.addHlPair(value: UShort) {
    val result = hl.toInt() + value.toInt()
    unsetFlag(FLAG_N)
    if (((hl.toInt() and 0x0FFF) + (value.toInt() and 0x0FFF)) > 0x0FFF) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result > 0xFFFF) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    val final = result.toUShort()
    h = (final.toInt() shr 8).toUByte()
    l = (final.toInt() and 0xFF).toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.subRegister(value: UByte) {
    val result = a - value
    setFlag(FLAG_N)
    if (result == 0u) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (result and 0x0Fu == 0u) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    a = result.toUByte()
    pc++; t += 4; m++
}

internal fun Cpu.cpHl() {
    val result = a - memory.readByte(hl)
    setFlag(FLAG_N)
    if (result.toUByte() == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (result.toUByte() and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    pc++; t += 8; m += 2
}

internal fun Cpu.cpRegister(value: UByte) {
    val result = a - value
    setFlag(FLAG_N)
    if (result.toUByte() == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (result.toUByte() and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    pc++; t += 4; m++
}

internal fun Cpu.cpImmediate() {
    val result = a - memory.readByte((pc + 1u).toUShort())
    setFlag(FLAG_N)
    if (result.toUByte() == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (result.toUByte() and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}