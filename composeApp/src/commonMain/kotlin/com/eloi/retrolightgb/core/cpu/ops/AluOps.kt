package com.eloi.retrolightgb.core.cpu.ops

import com.eloi.retrolightgb.core.cpu.Cpu
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

internal fun Cpu.decHlAddress() {
    val value = memory.readByte(hl)
    val new = (value - 1u).toUByte()
    memory.writeByte(hl, new)
    setFlag(FLAG_N)
    if (new == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (value and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    pc++; t += 12; m += 3
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

internal fun Cpu.daa() {
    var value = a.toInt()
    if (!isFlagSet(FLAG_N)) {
        if (isFlagSet(FLAG_H) || (value and 0x0F) > 9) value += 0x06
        if (isFlagSet(FLAG_C) || value > 0x9F) { value += 0x60; setFlag(FLAG_C) }
    } else {
        if (isFlagSet(FLAG_H)) value -= 0x06
        if (isFlagSet(FLAG_C)) value -= 0x60
    }
    unsetFlag(FLAG_H)
    a = (value and 0xFF).toUByte()
    if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    pc++; t += 4; m++
}

internal fun Cpu.scf() {
    unsetFlag(FLAG_N); unsetFlag(FLAG_H); setFlag(FLAG_C)
    pc++; t += 4; m++
}

internal fun Cpu.ccf() {
    val carry = !isFlagSet(FLAG_C)
    unsetFlag(FLAG_N); unsetFlag(FLAG_H)
    if (carry) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    pc++; t += 4; m++
}

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

internal fun Cpu.adcARegister(value: UByte) {
    val carry = if (isFlagSet(FLAG_C)) 1 else 0
    val result = a.toInt() + value.toInt() + carry
    resetFlags()
    if ((result and 0xFF) == 0) setFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) + (value.toInt() and 0x0F) + carry) > 0x0F) setFlag(FLAG_H)
    if (result > 0xFF) setFlag(FLAG_C)
    a = result.toUByte()
    pc++; t += 4; m++
}

internal fun Cpu.adcAHl() {
    val value = memory.readByte(hl)
    val carry = if (isFlagSet(FLAG_C)) 1 else 0
    val result = a.toInt() + value.toInt() + carry
    resetFlags()
    if ((result and 0xFF) == 0) setFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) + (value.toInt() and 0x0F) + carry) > 0x0F) setFlag(FLAG_H)
    if (result > 0xFF) setFlag(FLAG_C)
    a = result.toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.adcAN8() {
    val value = memory.readByte((pc + 1u).toUShort())
    val carry = if (isFlagSet(FLAG_C)) 1 else 0
    val result = a.toInt() + value.toInt() + carry
    resetFlags()
    if ((result and 0xFF) == 0) setFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) + (value.toInt() and 0x0F) + carry) > 0x0F) setFlag(FLAG_H)
    if (result > 0xFF) setFlag(FLAG_C)
    a = result.toUByte()
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.sbcARegister(value: UByte) {
    val carry = if (isFlagSet(FLAG_C)) 1 else 0
    val result = a.toInt() - value.toInt() - carry
    setFlag(FLAG_N)
    if ((result and 0xFF) == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) - (value.toInt() and 0x0F) - carry) < 0) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    a = result.toUByte()
    pc++; t += 4; m++
}

internal fun Cpu.sbcAHl() {
    val value = memory.readByte(hl)
    val carry = if (isFlagSet(FLAG_C)) 1 else 0
    val result = a.toInt() - value.toInt() - carry
    setFlag(FLAG_N)
    if ((result and 0xFF) == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) - (value.toInt() and 0x0F) - carry) < 0) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    a = result.toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.sbcAN8() {
    val value = memory.readByte((pc + 1u).toUShort())
    val carry = if (isFlagSet(FLAG_C)) 1 else 0
    val result = a.toInt() - value.toInt() - carry
    setFlag(FLAG_N)
    if ((result and 0xFF) == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) - (value.toInt() and 0x0F) - carry) < 0) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    a = result.toUByte()
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.subHl() {
    val value = memory.readByte(hl)
    val result = a - value
    setFlag(FLAG_N)
    if (result == 0u) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (result and 0x0Fu == 0u) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    a = result.toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.andAHl() {
    a = a and memory.readByte(hl)
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z)
    setFlag(FLAG_H)
    pc++; t += 8; m += 2
}

internal fun Cpu.xorAHl() {
    a = a xor memory.readByte(hl)
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z)
    pc++; t += 8; m += 2
}

internal fun Cpu.orAHl() {
    a = a or memory.readByte(hl)
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z)
    pc++; t += 8; m += 2
}

internal fun Cpu.addAN8() {
    val value = memory.readByte((pc + 1u).toUShort())
    val result = a.toInt() + value.toInt()
    resetFlags()
    if ((result and 0xFF) == 0) setFlag(FLAG_Z)
    if (((a.toInt() and 0x0F) + (value.toInt() and 0x0F)) > 0x0F) setFlag(FLAG_H)
    if (result > 0xFF) setFlag(FLAG_C)
    a = result.toUByte()
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.subN8() {
    val value = memory.readByte((pc + 1u).toUShort())
    val result = a - value
    setFlag(FLAG_N)
    if (result == 0u) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (result and 0x0Fu == 0u) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    a = result.toUByte()
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.xorAN8() {
    val value = memory.readByte((pc + 1u).toUShort())
    a = a xor value
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.orAN8() {
    val value = memory.readByte((pc + 1u).toUShort())
    a = a or value
    resetFlags()
    if (a == 0u.toUByte()) setFlag(FLAG_Z)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}

internal fun Cpu.cpImmediate() {
    val result = a - memory.readByte((pc + 1u).toUShort())
    setFlag(FLAG_N)
    if (result.toUByte() == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
    if (result.toUByte() and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
    if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
    pc = (pc + 2u).toUShort(); t += 8; m += 2
}