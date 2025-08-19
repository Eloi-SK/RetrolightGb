package com.eloi.retrolightgb.core.cpu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eloi.retrolightgb.core.memory.Memory
import kotlin.reflect.KMutableProperty0

class Cpu(val memory: Memory, val isDebug: Boolean = false) {
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

    val hl: UShort
        get() = combinateBytes(h, l)
    val bc: UShort
        get() = combinateBytes(b, c)
    val de: UShort
        get() = combinateBytes(d, e)

    var registersDump by mutableStateOf(buildRegistersDump())
    var flagsDump by mutableStateOf(buildFlagsDump())

    private val cbInstructions: Map<UInt, () -> Unit> = mapOf(
        0x10u to { b = rlRegister(registerName = "B", registerValue = b) },
        0x11u to { c = rlRegister(registerName = "C", registerValue = c) },
        0x12u to { d = rlRegister(registerName = "D", registerValue = d) },
        0x13u to { e = rlRegister(registerName = "E", registerValue = e) },
        0x14u to { h = rlRegister(registerName = "H", registerValue = h) },
        0x15u to { l = rlRegister(registerName = "L", registerValue = l) },
        0x17u to { a = rlRegister(registerName = "A", registerValue = a) },
        0x40u to { bitNumberRegister(bit = 0, registerName = "B", registerValue = b) },
        0x41u to { bitNumberRegister(bit = 0, registerName = "C", registerValue = c) },
        0x42u to { bitNumberRegister(bit = 0, registerName = "D", registerValue = d) },
        0x43u to { bitNumberRegister(bit = 0, registerName = "E", registerValue = e) },
        0x44u to { bitNumberRegister(bit = 0, registerName = "H", registerValue = h) },
        0x45u to { bitNumberRegister(bit = 0, registerName = "L", registerValue = l) },
        0x47u to { bitNumberRegister(bit = 0, registerName = "A", registerValue = a) },
        0x48u to { bitNumberRegister(bit = 1, registerName = "B", registerValue = b) },
        0x49u to { bitNumberRegister(bit = 1, registerName = "C", registerValue = c) },
        0x4Au to { bitNumberRegister(bit = 1, registerName = "D", registerValue = d) },
        0x4Bu to { bitNumberRegister(bit = 1, registerName = "E", registerValue = e) },
        0x4Cu to { bitNumberRegister(bit = 1, registerName = "H", registerValue = h) },
        0x4Du to { bitNumberRegister(bit = 1, registerName = "L", registerValue = l) },
        0x4Fu to { bitNumberRegister(bit = 1, registerName = "A", registerValue = a) },
        0x50u to { bitNumberRegister(bit = 2, registerName = "B", registerValue = b) },
        0x51u to { bitNumberRegister(bit = 2, registerName = "C", registerValue = c) },
        0x52u to { bitNumberRegister(bit = 2, registerName = "D", registerValue = d) },
        0x53u to { bitNumberRegister(bit = 2, registerName = "E", registerValue = e) },
        0x54u to { bitNumberRegister(bit = 2, registerName = "H", registerValue = h) },
        0x55u to { bitNumberRegister(bit = 2, registerName = "L", registerValue = l) },
        0x57u to { bitNumberRegister(bit = 2, registerName = "A", registerValue = a) },
        0x58u to { bitNumberRegister(bit = 3, registerName = "B", registerValue = b) },
        0x59u to { bitNumberRegister(bit = 3, registerName = "C", registerValue = c) },
        0x5Au to { bitNumberRegister(bit = 3, registerName = "D", registerValue = d) },
        0x5Bu to { bitNumberRegister(bit = 3, registerName = "E", registerValue = e) },
        0x5Cu to { bitNumberRegister(bit = 3, registerName = "H", registerValue = h) },
        0x5Du to { bitNumberRegister(bit = 3, registerName = "L", registerValue = l) },
        0x5Fu to { bitNumberRegister(bit = 3, registerName = "A", registerValue = a) },
        0x60u to { bitNumberRegister(bit = 4, registerName = "B", registerValue = b) },
        0x61u to { bitNumberRegister(bit = 4, registerName = "C", registerValue = c) },
        0x62u to { bitNumberRegister(bit = 4, registerName = "D", registerValue = d) },
        0x63u to { bitNumberRegister(bit = 4, registerName = "E", registerValue = e) },
        0x64u to { bitNumberRegister(bit = 4, registerName = "H", registerValue = h) },
        0x65u to { bitNumberRegister(bit = 4, registerName = "L", registerValue = l) },
        0x67u to { bitNumberRegister(bit = 4, registerName = "A", registerValue = a) },
        0x68u to { bitNumberRegister(bit = 5, registerName = "B", registerValue = b) },
        0x69u to { bitNumberRegister(bit = 5, registerName = "C", registerValue = c) },
        0x6Au to { bitNumberRegister(bit = 5, registerName = "D", registerValue = d) },
        0x6Bu to { bitNumberRegister(bit = 5, registerName = "E", registerValue = e) },
        0x6Cu to { bitNumberRegister(bit = 5, registerName = "H", registerValue = h) },
        0x6Du to { bitNumberRegister(bit = 5, registerName = "L", registerValue = l) },
        0x6Fu to { bitNumberRegister(bit = 5, registerName = "A", registerValue = a) },
        0x70u to { bitNumberRegister(bit = 6, registerName = "B", registerValue = b) },
        0x71u to { bitNumberRegister(bit = 6, registerName = "C", registerValue = c) },
        0x72u to { bitNumberRegister(bit = 6, registerName = "D", registerValue = d) },
        0x73u to { bitNumberRegister(bit = 6, registerName = "E", registerValue = e) },
        0x74u to { bitNumberRegister(bit = 6, registerName = "H", registerValue = h) },
        0x75u to { bitNumberRegister(bit = 6, registerName = "L", registerValue = l) },
        0x77u to { bitNumberRegister(bit = 6, registerName = "A", registerValue = a) },
        0x78u to { bitNumberRegister(bit = 7, registerName = "B", registerValue = b) },
        0x79u to { bitNumberRegister(bit = 7, registerName = "C", registerValue = c) },
        0x7Au to { bitNumberRegister(bit = 7, registerName = "D", registerValue = d) },
        0x7Bu to { bitNumberRegister(bit = 7, registerName = "E", registerValue = e) },
        0x7Cu to { bitNumberRegister(bit = 7, registerName = "H", registerValue = h) },
        0x7Du to { bitNumberRegister(bit = 7, registerName = "L", registerValue = l) },
        0x7Fu to { bitNumberRegister(bit = 7, registerName = "A", registerValue = a) },
    )
    
    private val instructions: Map<UInt, () -> Unit> = mapOf(
        0x00u to ::nop,
        0x06u to { b = loadRegisterN8(registerName = "B") },
        0x0Eu to { c = loadRegisterN8(registerName = "C") },
        0x16u to { d = loadRegisterN8(registerName = "D") },
        0x1Eu to { e = loadRegisterN8(registerName = "E") },
        0x26u to { h = loadRegisterN8(registerName = "H") },
        0x2Eu to { l = loadRegisterN8(registerName = "L") },
        0x3Eu to { a = loadRegisterN8(registerName = "A") },
        0x40u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "B", reg2Value = b) },
        0x41u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "C", reg2Value = c) },
        0x42u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "D", reg2Value = d) },
        0x43u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "E", reg2Value = e) },
        0x44u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "H", reg2Value = h) },
        0x45u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "L", reg2Value = l) },
        0x47u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "A", reg2Value = a) },
        0x48u to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "B", reg2Value = b) },
        0x49u to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "C", reg2Value = c) },
        0x4Au to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "D", reg2Value = d) },
        0x4Bu to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "E", reg2Value = e) },
        0x4Cu to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "H", reg2Value = h) },
        0x4Du to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "L", reg2Value = l) },
        0x4Fu to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "A", reg2Value = a) },
        0x50u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "B", reg2Value = b) },
        0x51u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "C", reg2Value = c) },
        0x52u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "D", reg2Value = d) },
        0x53u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "E", reg2Value = e) },
        0x54u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "H", reg2Value = h) },
        0x55u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "L", reg2Value = l) },
        0x57u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "A", reg2Value = a) },
        0x58u to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "B", reg2Value = b) },
        0x59u to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "C", reg2Value = c) },
        0x5Au to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "D", reg2Value = d) },
        0x5Bu to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "E", reg2Value = e) },
        0x5Cu to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "H", reg2Value = h) },
        0x5Du to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "L", reg2Value = l) },
        0x5Fu to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "A", reg2Value = a) },
        0x60u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "B", reg2Value = b) },
        0x61u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "C", reg2Value = c) },
        0x62u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "D", reg2Value = d) },
        0x63u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "E", reg2Value = e) },
        0x64u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "H", reg2Value = h) },
        0x65u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "L", reg2Value = l) },
        0x67u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "A", reg2Value = a) },
        0x68u to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "B", reg2Value = b) },
        0x69u to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "C", reg2Value = c) },
        0x6Au to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "D", reg2Value = d) },
        0x6Bu to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "E", reg2Value = e) },
        0x6Cu to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "H", reg2Value = h) },
        0x6Du to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "L", reg2Value = l) },
        0x6Fu to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "A", reg2Value = a) },
        0x78u to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "B", reg2Value = b) },
        0x79u to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "C", reg2Value = c) },
        0x7Au to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "D", reg2Value = d) },
        0x7Bu to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "E", reg2Value = e) },
        0x7Cu to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "H", reg2Value = h) },
        0x7Du to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "L", reg2Value = l) },
        0x7Fu to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "A", reg2Value = a) },
        0x01u to { loadPairRegisterN16(pairRegisterName = "BC").destructureAssign(::b, ::c) },
        0x11u to { loadPairRegisterN16(pairRegisterName = "DE").destructureAssign(::d, ::e) },
        0x21u to { loadPairRegisterN16(pairRegisterName = "HL").destructureAssign(::h, ::l) },
        0x0Au to { loadPairRegisterAddressToA(pairName = "BC", pairValue = bc) },
        0x1Au to { loadPairRegisterAddressToA(pairName = "DE", pairValue = de) },
        0x31u to ::loadSpN16,
        0x04u to { b = incRegister(registerName = "B", registerValue = b) },
        0x0Cu to { c = incRegister(registerName = "C", registerValue = c) },
        0x14u to { d = incRegister(registerName = "D", registerValue = d) },
        0x1Cu to { e = incRegister(registerName = "E", registerValue = e) },
        0x24u to { h = incRegister(registerName = "H", registerValue = h) },
        0x2Cu to { l = incRegister(registerName = "L", registerValue = l) },
        0x3Cu to { a = incRegister(registerName = "A", registerValue = a) },
        0x05u to { b = decRegister(registerName = "B", registerValue = b) },
        0x0Du to { c = decRegister(registerName = "C", registerValue = c) },
        0x15u to { d = decRegister(registerName = "D", registerValue = d) },
        0x1Du to { e = decRegister(registerName = "E", registerValue = e) },
        0x25u to { h = decRegister(registerName = "H", registerValue = h) },
        0x2Du to { l = decRegister(registerName = "L", registerValue = l) },
        0x3Du to { a = decRegister(registerName = "A", registerValue = a) },
        0x03u to { incRegisterPair(pairName = "BC", pairValue = bc).destructureAssign(::b, ::c) },
        0x13u to { incRegisterPair(pairName = "DE", pairValue = de).destructureAssign(::d, ::e) },
        0x23u to { incRegisterPair(pairName = "HL", pairValue = hl).destructureAssign(::h, ::l) },
        0x33u to ::incSp,
        0xC5u to { pushPairRegister(pairRegisterName = "BC", high = b, low = c) },
        0xD5u to { pushPairRegister(pairRegisterName = "DE", high = d, low = e) },
        0xE5u to { pushPairRegister(pairRegisterName = "HL", high = h, low = l) },
        0xF5u to { pushPairRegister(pairRegisterName = "AF", high = a, low = f) },
        0xC1u to { popPairRegister(pairRegisterName = "BC").destructureAssign(::b, ::c) },
        0xD1u to { popPairRegister(pairRegisterName = "DE").destructureAssign(::d, ::e) },
        0xE1u to { popPairRegister(pairRegisterName = "HL").destructureAssign(::h, ::l) },
        0xF1u to { popPairRegister(pairRegisterName = "AF").destructureAssign(::a, ::f) },
        0xA8u to { xorARegister(registerName = "B", registerValue = b) },
        0xA9u to { xorARegister(registerName = "C", registerValue = c) },
        0xAAu to { xorARegister(registerName = "D", registerValue = d) },
        0xABu to { xorARegister(registerName = "E", registerValue = e) },
        0xACu to { xorARegister(registerName = "H", registerValue = h) },
        0xADu to { xorARegister(registerName = "L", registerValue = l) },
        0xAFu to { xorARegister(registerName = "A", registerValue = a) },
        0x17u to ::rla,
        0xCDu to ::call,
        0xC9u to ::ret,
        0x22u to ::loadHlIncA,
        0x32u to ::loadHlDecA,
        0x70u to { loadRegisterToHlAddress(registerName = "B", registerValue = b) },
        0x71u to { loadRegisterToHlAddress(registerName = "C", registerValue = c) },
        0x72u to { loadRegisterToHlAddress(registerName = "D", registerValue = d) },
        0x73u to { loadRegisterToHlAddress(registerName = "E", registerValue = e) },
        0x74u to { loadRegisterToHlAddress(registerName = "H", registerValue = h) },
        0x75u to { loadRegisterToHlAddress(registerName = "L", registerValue = l) },
        0x77u to { loadRegisterToHlAddress(registerName = "A", registerValue = a) },
        0x20u to ::jrNz,
        0xE0u to ::loadFF00N8,
        0xE2u to ::loadFF00C,
    )

    fun step() {
        val opcode = memory.readByte(pc)
        if (opcode == 0xCBu.toUByte()) {
            val cbOpcode = memory.readByte((pc + 1u).toUShort())
            cbInstructions[cbOpcode.toUInt()]?.invoke()
                ?: throw NotImplementedError("CB prefix opcode 0x${cbOpcode.toHexString().uppercase()} " +
                        "not implemented in PC 0x${pc.toHexString()}")
        } else {
            instructions[opcode.toUInt()]?.invoke()
                ?: throw NotImplementedError(
                    "Opcode 0x${opcode.toHexString().uppercase()} not implemented in PC 0x${pc.toHexString()}"
                )
        }
        registersDump = buildRegistersDump()
        flagsDump = buildFlagsDump()
    }

    private fun buildRegistersDump(): String =
        "A: 0x${a.toHexString()}, B: 0x${b.toHexString()}, C: 0x${c.toHexString()}, D: 0x${d.toHexString()}, " +
                "E: 0x${e.toHexString()}, H: 0x${h.toHexString()}, L: 0x${l.toHexString()}"

    private fun buildFlagsDump(): String =
        "Z: ${isFlagSet(FLAG_Z)}, N: ${isFlagSet(FLAG_N)}, H: ${isFlagSet(FLAG_H)}, C: ${isFlagSet(FLAG_C)}"

    private fun combinateBytes(high: UByte, low: UByte): UShort =
        ((high.toUInt() shl 8) or low.toUInt()).toUShort()

    private fun resetFlags() {
        f = 0u
    }

    private fun setFlag(flag: UByte) {
        f = f or flag
    }

    private fun unsetFlag(flag: UByte) {
        f = f and flag.inv()
    }

    private fun <A, B> Pair<A, B>.destructureAssign(a: KMutableProperty0<A>, b: KMutableProperty0<B>) {
        a.set(this.first)
        b.set(this.second)
    }

    private fun isFlagSet(flag: UByte): Boolean = (f and flag) != 0u.toUByte()

    private fun nop() {
        if (isDebug)
            println("$${pc.toHexString()} NOP")

        pc++
        t += 4
        m++
    }

    private fun loadRegisterN8(registerName: String): UByte {
        val newValue = memory.readByte((pc + 1u).toUShort())

        if (isDebug)
            println("$${pc.toHexString()} LD $registerName, $${newValue.toHexString()}")

        pc = (pc + 2u).toUShort()
        t += 8
        m += 2

        return newValue
    }

    private fun loadPairRegisterN16(pairRegisterName: String): Pair<UByte, UByte> {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())

        val value = combinateBytes(high, low)

        if (isDebug)
            println("$${pc.toHexString()} LD $pairRegisterName, $${value.toHexString()}")

        pc = (pc + 3u).toUShort()
        t += 12
        m += 3

        return high to low
    }

    private fun loadSpN16() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        sp = combinateBytes(high, low)

        if (isDebug)
            println("$${pc.toHexString()} LD SP, $${sp.toHexString()}")

        pc = (pc + 3u).toUShort()
        t += 12
        m += 3
    }

    private fun incSp() {
        sp++

        if (isDebug)
            println("$${pc.toHexString()} INC SP")

        pc++
        t += 8
        m += 2
    }

    private fun incRegisterPair(pairName: String, pairValue: UShort): Pair<UByte, UByte> {
        val newValue = pairValue + 1u
        val high = (newValue shr 8).toUByte()
        val low = (newValue and 0xFFu).toUByte()

        if (isDebug)
            println("$${pc.toHexString()} INC $pairName")

        pc++
        t += 8
        m += 2

        return high to low
    }

    private fun incRegister(registerName: String, registerValue: UByte): UByte {
        val newValue = (registerValue + 1u).toUByte()
        unsetFlag(FLAG_N)
        if (newValue == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        if (newValue and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)

        if (isDebug)
            println("$${pc.toHexString()} INC $registerName")

        pc++
        t += 4
        m++

        return newValue
    }

    private fun decRegister(registerName: String, registerValue: UByte): UByte {
        val newValue = (registerValue - 1u).toUByte()
        setFlag(FLAG_N)
        if (newValue == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        if (newValue and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)

        if (isDebug)
            println("$${pc.toHexString()} DEC $registerName")

        pc++
        t += 4
        m++

        return newValue
    }

    private fun pushPairRegister(pairRegisterName: String, high: UByte, low: UByte) {
        sp--
        memory.writeByte(sp, high)
        sp--
        memory.writeByte(sp, low)

        if (isDebug)
            println("$${pc.toHexString()} PUSH $pairRegisterName")

        pc++
        t += 16
        m += 4
    }

    private fun popPairRegister(pairRegisterName: String): Pair<UByte, UByte> {
        val low = memory.readByte(sp)
        val high = memory.readByte((sp + 1u).toUShort())

        if (isDebug)
            println("$${pc.toHexString()} POP $pairRegisterName")

        pc++
        sp = (sp + 2u).toUShort()
        t += 12
        m += 3

        return high to low
    }

    private fun xorARegister(registerName: String, registerValue: UByte) {
        a = a xor registerValue
        resetFlags()

        if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)

        if (isDebug)
            println("$${pc.toHexString()} XOR $registerName")

        pc++
        t += 4
        m++
    }

    private fun rla() {
        val oldCarryFlag = isFlagSet(FLAG_C)
        val carryFlag = (0b1000_0000.toUInt() and a.toUInt()) != 0u

        if (carryFlag) setFlag(FLAG_C) else unsetFlag(FLAG_C)

        var result = a.toUInt() shl 1
        result = result and 0b1111_1110.toUInt()

        if (oldCarryFlag) result = result or 0b0000_0001.toUInt()

        if (result == 0u) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        unsetFlag(FLAG_N)
        unsetFlag(FLAG_H)
        a = result.toUByte()

        if (isDebug)
            println("$${pc.toHexString()} RLA")

        pc++
        t += 4
        m++
    }

    private fun call() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val targetAddress = combinateBytes(high, low)
        val returnAddress = (pc + 3u).toUShort()

        sp--
        memory.writeByte(sp, (returnAddress.toInt() shr 8).toUByte())
        sp--
        memory.writeByte(sp, (returnAddress.toInt() and 0xFF).toUByte())

        if (isDebug)
            println("$${pc.toHexString()} CALL $${targetAddress.toHexString()}")

        pc = targetAddress
        t += 24
        m += 6
    }

    private fun ret() {
        val low = memory.readByte(sp)
        sp++
        val high = memory.readByte(sp)

        if (isDebug)
            println("$${pc.toHexString()} RET")

        pc = combinateBytes(high, low)
        t += 16
        m += 4
    }

    private fun loadRegisterToRegister(reg1Name: String, reg2Name: String, reg2Value: UByte): UByte {
        if (isDebug)
            println("$${pc.toHexString()} LD $reg1Name, $reg2Name")

        pc++
        t += 4
        m++

        return reg2Value
    }

    private fun loadRegisterToHlAddress(registerName: String, registerValue: UByte) {
        memory.writeByte(hl, registerValue)

        if (isDebug)
            println("$${pc.toHexString()} LD (HL), $registerName")

        pc++
        t += 8
        m += 2
    }

    private fun loadPairRegisterAddressToA(pairName: String, pairValue: UShort) {
        a = memory.readByte(pairValue)

        if (isDebug)
            println("$${pc.toHexString()} LD A, ($pairName)")

        pc++
        t += 8
        m += 2
    }

    private fun loadHlIncA() {
        memory.writeByte(hl, a)
        val inc = hl + 1u
        h = (inc shr 8).toUByte()
        l = (inc and 0xFFu).toUByte()

        if (isDebug)
            println("$${pc.toHexString()} LD (HL+), A")

        pc++
        t += 8
        m += 2
    }

    private fun loadHlDecA() {
        memory.writeByte(hl, a)
        val dec = hl - 1u
        h = (dec shr 8).toUByte()
        l = (dec and 0xFFu).toUByte()

        if (isDebug)
            println("$${pc.toHexString()} LD (HL-), A")

        pc++
        t += 8
        m += 2
    }

    private fun loadFF00N8() {
        val value = memory.readByte((pc + 1u).toUShort())
        val address = combinateBytes(high = 0xFFu, low = value)
        memory.writeByte(address, a)

        if(isDebug)
            println("$${pc.toHexString()} LD (\$FF00+$${value.toHexString()}), A")

        pc = (pc + 2u).toUShort()
        t += 12
        m += 3
    }

    private fun loadFF00C() {
        val address = combinateBytes(high = 0xFFu, low = c)
        memory.writeByte(address, a)

        if (isDebug)
            println("$${pc.toHexString()} LD (\$FF00+C), A")

        pc++
        t += 8
        m += 2
    }

    private fun jrNz() {
        val offset = memory.readByte((pc + 1u).toUShort()).toByte()

        if (isFlagSet(FLAG_Z).not()) {
            if (isDebug)
                println("$${pc.toHexString()} JR NZ, $${(pc.toInt() + 2 + offset.toInt()).toUShort()}")

            pc = (pc.toInt() + 2 + offset.toInt()).toUShort()
            t += 12
            m += 3
        } else {
            if (isDebug)
                println("$${pc.toHexString()} JR NZ, $${(pc + 2u).toUShort()}")

            pc = (pc + 2u).toUShort()
            t += 8
            m += 2
        }
    }

    private fun rlRegister(registerName: String, registerValue: UByte): UByte {
        val oldValue = registerValue
        val oldCarry = if (isFlagSet(FLAG_C)) 1 else 0

        val newCarry = (oldValue.toInt() shr 7) and 0x01
        val result = ((oldValue.toInt() shl 1) or oldCarry) and 0xFF

        result.toUByte()
        if (newCarry == 0) unsetFlag(FLAG_C) else setFlag(FLAG_C)
        if (result == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        unsetFlag(FLAG_N)
        unsetFlag(FLAG_H)

        if (isDebug)
            println("$${pc.toHexString()} RL $registerName")

        pc = (pc + 2u).toUShort()
        t += 8
        m += 2

        return  result.toUByte()
    }

    private fun bitNumberRegister(bit: Int, registerName: String, registerValue: UByte) {
        val bitValue = (registerValue.toInt() shr bit) and 0x01

        setFlag(FLAG_H)
        unsetFlag(FLAG_N)
        if (bitValue == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)

        if (isDebug)
            println("$${pc.toHexString()} BIT $bit, $registerName")

        pc = (pc + 2u).toUShort()
        t += 8
        m += 2
    }

    companion object {
        val FLAG_Z = 0b1000_0000u.toUByte() // Zero flag
        val FLAG_N = 0b0100_0000u.toUByte() // Subtract flag
        val FLAG_H = 0b0010_0000u.toUByte() // Half carry flag
        val FLAG_C = 0b0001_0000u.toUByte() // Carry flag
    }
}