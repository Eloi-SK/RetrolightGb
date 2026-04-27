package com.eloi.retrolightgb.core.cpu.ops

import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_C
import com.eloi.retrolightgb.core.cpu.Cpu.Companion.FLAG_H

internal fun Cpu.loadRegisterFromHlAddress(): UByte {
    val value = memory.readByte(hl)
    pc++; t += 8; m += 2
    return value
}

internal fun Cpu.loadRegisterN8(): UByte {
    val value = memory.readByte((pc + 1u).toUShort())
    pc = (pc + 2u).toUShort(); t += 8; m += 2
    return value
}

internal fun Cpu.loadPairRegisterN16(): Pair<UByte, UByte> {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    pc = (pc + 3u).toUShort(); t += 12; m += 3
    return high to low
}

internal fun Cpu.loadN16A() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    memory.writeByte(combinateBytes(high, low), a)
    pc = (pc + 3u).toUShort(); t += 16; m += 4
}

internal fun Cpu.loadAN16() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    a = memory.readByte(combinateBytes(high, low))
    pc = (pc + 3u).toUShort(); t += 16; m += 4
}

internal fun Cpu.loadSpN16() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    sp = combinateBytes(high, low)
    pc = (pc + 3u).toUShort(); t += 12; m += 3
}

internal fun Cpu.loadRegisterToRegister(value: UByte): UByte {
    pc++; t += 4; m++
    return value
}

internal fun Cpu.loadRegisterToAddressPair(address: UShort) {
    memory.writeByte(address, a)
    pc++; t += 8; m += 2
}

internal fun Cpu.loadRegisterToHlAddress(value: UByte) {
    memory.writeByte(hl, value)
    pc++; t += 8; m += 2
}

internal fun Cpu.loadPairRegisterAddressToA(address: UShort) {
    a = memory.readByte(address)
    pc++; t += 8; m += 2
}

internal fun Cpu.loadHlIncA() {
    memory.writeByte(hl, a)
    val inc = hl + 1u
    h = (inc shr 8).toUByte(); l = (inc and 0xFFu).toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.loadHlDecA() {
    memory.writeByte(hl, a)
    val dec = hl - 1u
    h = (dec shr 8).toUByte(); l = (dec and 0xFFu).toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.loadAFF00N8() {
    val value = memory.readByte((pc + 1u).toUShort())
    a = memory.readByte(combinateBytes(high = 0xFFu, low = value))
    pc = (pc + 2u).toUShort(); t += 12; m += 3
}

internal fun Cpu.loadFF00N8() {
    val value = memory.readByte((pc + 1u).toUShort())
    memory.writeByte(combinateBytes(high = 0xFFu, low = value), a)
    pc = (pc + 2u).toUShort(); t += 12; m += 3
}

internal fun Cpu.loadFF00C() {
    memory.writeByte(combinateBytes(high = 0xFFu, low = c), a)
    pc++; t += 8; m += 2
}

internal fun Cpu.loadAFF00C() {
    a = memory.readByte(combinateBytes(high = 0xFFu, low = c))
    pc++; t += 8; m += 2
}

internal fun Cpu.ldHLN8() {
    memory.writeByte(hl, memory.readByte((pc + 1u).toUShort()))
    pc = (pc + 2u).toUShort(); t += 12; m += 3
}

internal fun Cpu.loadAHlInc() {
    a = memory.readByte(hl)
    val inc = hl + 1u
    h = (inc shr 8).toUByte(); l = (inc and 0xFFu).toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.loadN16Sp() {
    val low = memory.readByte((pc + 1u).toUShort())
    val high = memory.readByte((pc + 2u).toUShort())
    val address = combinateBytes(high, low)
    memory.writeByte(address, (sp and 0xFFu).toUByte())
    memory.writeByte((address + 1u).toUShort(), (sp.toUInt() shr 8).toUByte())
    pc = (pc + 3u).toUShort(); t += 20; m += 5
}

internal fun Cpu.loadAHlDec() {
    a = memory.readByte(hl)
    val dec = hl - 1u
    h = (dec shr 8).toUByte(); l = (dec and 0xFFu).toUByte()
    pc++; t += 8; m += 2
}

internal fun Cpu.loadSpHl() {
    sp = hl
    pc++; t += 8; m += 2
}

internal fun Cpu.loadHlSpE8() {
    val offset = memory.readByte((pc + 1u).toUShort()).toByte().toInt()
    val base = sp.toInt()
    val result = base + offset
    resetFlags()
    if (((base xor offset xor result) and 0x10) != 0) setFlag(FLAG_H)
    if (((base xor offset xor result) and 0x100) != 0) setFlag(FLAG_C)
    h = ((result shr 8) and 0xFF).toUByte()
    l = (result and 0xFF).toUByte()
    pc = (pc + 2u).toUShort(); t += 12; m += 3
}

internal fun Cpu.pushPairRegister(high: UByte, low: UByte) {
    sp--; memory.writeByte(sp, high)
    sp--; memory.writeByte(sp, low)
    pc++; t += 16; m += 4
}

internal fun Cpu.popPairRegister(): Pair<UByte, UByte> {
    val low = memory.readByte(sp)
    val high = memory.readByte((sp + 1u).toUShort())
    pc++; sp = (sp + 2u).toUShort(); t += 12; m += 3
    return high to low
}