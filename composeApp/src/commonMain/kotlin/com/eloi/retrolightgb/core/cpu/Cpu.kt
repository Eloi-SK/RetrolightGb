package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.memory.Memory
import kotlin.reflect.KMutableProperty0

class Cpu(val memory: Memory, val isDebug: Boolean = false) {

    val registers = CpuRegisters()
    private val tracer = CpuTracer(registers, memory)
    var traceWriter: ((String) -> Unit)?
        get() = tracer.writer
        set(v) { tracer.writer = v }
    fun dumpTrace() = tracer.dump()

    var a: UByte get() = registers.a; set(v) { registers.a = v }
    var b: UByte get() = registers.b; set(v) { registers.b = v }
    var c: UByte get() = registers.c; set(v) { registers.c = v }
    var d: UByte get() = registers.d; set(v) { registers.d = v }
    var e: UByte get() = registers.e; set(v) { registers.e = v }
    var f: UByte get() = registers.f; set(v) { registers.f = v }
    var h: UByte get() = registers.h; set(v) { registers.h = v }
    var l: UByte get() = registers.l; set(v) { registers.l = v }
    var pc: UShort get() = registers.pc; set(v) { registers.pc = v }
    var sp: UShort get() = registers.sp; set(v) { registers.sp = v }
    var t: Int get() = registers.t; set(v) { registers.t = v }
    var m: Int get() = registers.m; set(v) { registers.m = v }
    var lastT: Int get() = registers.lastT; set(v) { registers.lastT = v }
    var lastM: Int get() = registers.lastM; set(v) { registers.lastM = v }
    var imeEnabled: Boolean get() = registers.imeEnabled; set(v) { registers.imeEnabled = v }
    var halted: Boolean get() = registers.halted; set(v) { registers.halted = v }
    val hl: UShort get() = registers.hl
    val bc: UShort get() = registers.bc
    val de: UShort get() = registers.de

    private val cbInstructions: Map<UInt, () -> Unit> = mapOf(
        0x00u to { b = rlcRegister(b) },
        0x01u to { c = rlcRegister(c) },
        0x02u to { d = rlcRegister(d) },
        0x03u to { e = rlcRegister(e) },
        0x04u to { h = rlcRegister(h) },
        0x05u to { l = rlcRegister(l) },
        0x06u to { rlcHl() },
        0x07u to { a = rlcRegister(a) },
        0x08u to { b = rrcRegister(b) },
        0x09u to { c = rrcRegister(c) },
        0x0Au to { d = rrcRegister(d) },
        0x0Bu to { e = rrcRegister(e) },
        0x0Cu to { h = rrcRegister(h) },
        0x0Du to { l = rrcRegister(l) },
        0x0Eu to { rrcHl() },
        0x0Fu to { a = rrcRegister(a) },
        0x10u to { b = rlRegister(b) },
        0x11u to { c = rlRegister(c) },
        0x12u to { d = rlRegister(d) },
        0x13u to { e = rlRegister(e) },
        0x14u to { h = rlRegister(h) },
        0x15u to { l = rlRegister(l) },
        0x16u to { rlHl() },
        0x17u to { a = rlRegister(a) },
        0x18u to { b = rrRegister(b) },
        0x19u to { c = rrRegister(c) },
        0x1Au to { d = rrRegister(d) },
        0x1Bu to { e = rrRegister(e) },
        0x1Cu to { h = rrRegister(h) },
        0x1Du to { l = rrRegister(l) },
        0x1Eu to { rrHl() },
        0x1Fu to { a = rrRegister(a) },
        0x20u to { b = slaRegister(b) },
        0x21u to { c = slaRegister(c) },
        0x22u to { d = slaRegister(d) },
        0x23u to { e = slaRegister(e) },
        0x24u to { h = slaRegister(h) },
        0x25u to { l = slaRegister(l) },
        0x26u to { slaHl() },
        0x27u to { a = slaRegister(a) },
        0x28u to { b = sraRegister(b) },
        0x29u to { c = sraRegister(c) },
        0x2Au to { d = sraRegister(d) },
        0x2Bu to { e = sraRegister(e) },
        0x2Cu to { h = sraRegister(h) },
        0x2Du to { l = sraRegister(l) },
        0x2Eu to { sraHl() },
        0x2Fu to { a = sraRegister(a) },
        0x30u to { b = swapRegister(b) },
        0x31u to { c = swapRegister(c) },
        0x32u to { d = swapRegister(d) },
        0x33u to { e = swapRegister(e) },
        0x34u to { h = swapRegister(h) },
        0x35u to { l = swapRegister(l) },
        0x36u to { swapHl() },
        0x37u to { a = swapRegister(a) },
        0x38u to { b = srlRegister(b) },
        0x39u to { c = srlRegister(c) },
        0x3Au to { d = srlRegister(d) },
        0x3Bu to { e = srlRegister(e) },
        0x3Cu to { h = srlRegister(h) },
        0x3Du to { l = srlRegister(l) },
        0x3Eu to { srlHl() },
        0x3Fu to { a = srlRegister(a) },
        0x40u to { bitNumberRegister(0, b) },
        0x41u to { bitNumberRegister(0, c) },
        0x42u to { bitNumberRegister(0, d) },
        0x43u to { bitNumberRegister(0, e) },
        0x44u to { bitNumberRegister(0, h) },
        0x45u to { bitNumberRegister(0, l) },
        0x46u to { bitNumberHl(0) },
        0x47u to { bitNumberRegister(0, a) },
        0x48u to { bitNumberRegister(1, b) },
        0x49u to { bitNumberRegister(1, c) },
        0x4Au to { bitNumberRegister(1, d) },
        0x4Bu to { bitNumberRegister(1, e) },
        0x4Cu to { bitNumberRegister(1, h) },
        0x4Du to { bitNumberRegister(1, l) },
        0x4Eu to { bitNumberHl(1) },
        0x4Fu to { bitNumberRegister(1, a) },
        0x50u to { bitNumberRegister(2, b) },
        0x51u to { bitNumberRegister(2, c) },
        0x52u to { bitNumberRegister(2, d) },
        0x53u to { bitNumberRegister(2, e) },
        0x54u to { bitNumberRegister(2, h) },
        0x55u to { bitNumberRegister(2, l) },
        0x56u to { bitNumberHl(2) },
        0x57u to { bitNumberRegister(2, a) },
        0x58u to { bitNumberRegister(3, b) },
        0x59u to { bitNumberRegister(3, c) },
        0x5Au to { bitNumberRegister(3, d) },
        0x5Bu to { bitNumberRegister(3, e) },
        0x5Cu to { bitNumberRegister(3, h) },
        0x5Du to { bitNumberRegister(3, l) },
        0x5Eu to { bitNumberHl(3) },
        0x5Fu to { bitNumberRegister(3, a) },
        0x60u to { bitNumberRegister(4, b) },
        0x61u to { bitNumberRegister(4, c) },
        0x62u to { bitNumberRegister(4, d) },
        0x63u to { bitNumberRegister(4, e) },
        0x64u to { bitNumberRegister(4, h) },
        0x65u to { bitNumberRegister(4, l) },
        0x66u to { bitNumberHl(4) },
        0x67u to { bitNumberRegister(4, a) },
        0x68u to { bitNumberRegister(5, b) },
        0x69u to { bitNumberRegister(5, c) },
        0x6Au to { bitNumberRegister(5, d) },
        0x6Bu to { bitNumberRegister(5, e) },
        0x6Cu to { bitNumberRegister(5, h) },
        0x6Du to { bitNumberRegister(5, l) },
        0x6Eu to { bitNumberHl(5) },
        0x6Fu to { bitNumberRegister(5, a) },
        0x70u to { bitNumberRegister(6, b) },
        0x71u to { bitNumberRegister(6, c) },
        0x72u to { bitNumberRegister(6, d) },
        0x73u to { bitNumberRegister(6, e) },
        0x74u to { bitNumberRegister(6, h) },
        0x75u to { bitNumberRegister(6, l) },
        0x76u to { bitNumberHl(6) },
        0x77u to { bitNumberRegister(6, a) },
        0x78u to { bitNumberRegister(7, b) },
        0x79u to { bitNumberRegister(7, c) },
        0x7Au to { bitNumberRegister(7, d) },
        0x7Bu to { bitNumberRegister(7, e) },
        0x7Cu to { bitNumberRegister(7, h) },
        0x7Du to { bitNumberRegister(7, l) },
        0x7Eu to { bitNumberHl(7) },
        0x7Fu to { bitNumberRegister(7, a) },
        0x80u to { b = resetBitRegister(0, b) },
        0x81u to { c = resetBitRegister(0, c) },
        0x82u to { d = resetBitRegister(0, d) },
        0x83u to { e = resetBitRegister(0, e) },
        0x84u to { h = resetBitRegister(0, h) },
        0x85u to { l = resetBitRegister(0, l) },
        0x87u to { a = resetBitRegister(0, a) },
    )

    private val instructions: Map<UInt, () -> Unit> = mapOf(
        0x00u to { nop() },
        0x01u to { loadPairRegisterN16().destructureAssign(::b, ::c) },
        0x02u to { loadRegisterToAddressPair(bc) },
        0x03u to { incRegisterPair(bc).destructureAssign(::b, ::c) },
        0x04u to { b = incRegister(b) },
        0x05u to { b = decRegister(b) },
        0x06u to { b = loadRegisterN8() },
        0x09u to { addHlPair(bc) },
        0x0Au to { loadPairRegisterAddressToA(bc) },
        0x0Bu to { decRegisterPair(bc).destructureAssign(::b, ::c) },
        0x0Cu to { c = incRegister(c) },
        0x0Du to { c = decRegister(c) },
        0x0Eu to { c = loadRegisterN8() },
        0x11u to { loadPairRegisterN16().destructureAssign(::d, ::e) },
        0x12u to { loadRegisterToAddressPair(de) },
        0x13u to { incRegisterPair(de).destructureAssign(::d, ::e) },
        0x14u to { d = incRegister(d) },
        0x15u to { d = decRegister(d) },
        0x16u to { d = loadRegisterN8() },
        0x17u to { rla() },
        0x18u to { jr() },
        0x19u to { addHlPair(de) },
        0x1Au to { loadPairRegisterAddressToA(de) },
        0x1Bu to { decRegisterPair(de).destructureAssign(::d, ::e) },
        0x1Cu to { e = incRegister(e) },
        0x1Du to { e = decRegister(e) },
        0x1Eu to { e = loadRegisterN8() },
        0x20u to { jrNz() },
        0x21u to { loadPairRegisterN16().destructureAssign(::h, ::l) },
        0x22u to { loadHlIncA() },
        0x23u to { incRegisterPair(hl).destructureAssign(::h, ::l) },
        0x24u to { h = incRegister(h) },
        0x25u to { h = decRegister(h) },
        0x26u to { h = loadRegisterN8() },
        0x28u to { jrZ() },
        0x29u to { addHlPair(hl) },
        0x2Au to { loadAHlInc() },
        0x2Bu to { decRegisterPair(hl).destructureAssign(::h, ::l) },
        0x2Cu to { l = incRegister(l) },
        0x2Du to { l = decRegister(l) },
        0x2Eu to { l = loadRegisterN8() },
        0x2Fu to { cpl() },
        0x30u to { jrNc() },
        0x31u to { loadSpN16() },
        0x32u to { loadHlDecA() },
        0x33u to { incSp() },
        0x34u to { incHlAddress() },
        0x36u to { ldHLN8() },
        0x38u to { jrC() },
        0x39u to { addHlPair(sp) },
        0x3Bu to { decSp() },
        0x3Cu to { a = incRegister(a) },
        0x3Du to { a = decRegister(a) },
        0x3Eu to { a = loadRegisterN8() },
        0x40u to { b = loadRegisterToRegister(b) },
        0x41u to { b = loadRegisterToRegister(c) },
        0x42u to { b = loadRegisterToRegister(d) },
        0x43u to { b = loadRegisterToRegister(e) },
        0x44u to { b = loadRegisterToRegister(h) },
        0x45u to { b = loadRegisterToRegister(l) },
        0x46u to { b = loadRegisterFromHlAddress() },
        0x47u to { b = loadRegisterToRegister(a) },
        0x48u to { c = loadRegisterToRegister(b) },
        0x49u to { c = loadRegisterToRegister(c) },
        0x4Au to { c = loadRegisterToRegister(d) },
        0x4Bu to { c = loadRegisterToRegister(e) },
        0x4Cu to { c = loadRegisterToRegister(h) },
        0x4Du to { c = loadRegisterToRegister(l) },
        0x4Eu to { c = loadRegisterFromHlAddress() },
        0x4Fu to { c = loadRegisterToRegister(a) },
        0x50u to { d = loadRegisterToRegister(b) },
        0x51u to { d = loadRegisterToRegister(c) },
        0x52u to { d = loadRegisterToRegister(d) },
        0x53u to { d = loadRegisterToRegister(e) },
        0x54u to { d = loadRegisterToRegister(h) },
        0x55u to { d = loadRegisterToRegister(l) },
        0x56u to { d = loadRegisterFromHlAddress() },
        0x57u to { d = loadRegisterToRegister(a) },
        0x58u to { e = loadRegisterToRegister(b) },
        0x59u to { e = loadRegisterToRegister(c) },
        0x5Au to { e = loadRegisterToRegister(d) },
        0x5Bu to { e = loadRegisterToRegister(e) },
        0x5Cu to { e = loadRegisterToRegister(h) },
        0x5Du to { e = loadRegisterToRegister(l) },
        0x5Eu to { e = loadRegisterFromHlAddress() },
        0x5Fu to { e = loadRegisterToRegister(a) },
        0x60u to { h = loadRegisterToRegister(b) },
        0x61u to { h = loadRegisterToRegister(c) },
        0x62u to { h = loadRegisterToRegister(d) },
        0x63u to { h = loadRegisterToRegister(e) },
        0x64u to { h = loadRegisterToRegister(h) },
        0x65u to { h = loadRegisterToRegister(l) },
        0x66u to { h = loadRegisterFromHlAddress() },
        0x67u to { h = loadRegisterToRegister(a) },
        0x68u to { l = loadRegisterToRegister(b) },
        0x69u to { l = loadRegisterToRegister(c) },
        0x6Au to { l = loadRegisterToRegister(d) },
        0x6Bu to { l = loadRegisterToRegister(e) },
        0x6Cu to { l = loadRegisterToRegister(h) },
        0x6Du to { l = loadRegisterToRegister(l) },
        0x6Eu to { l = loadRegisterFromHlAddress() },
        0x6Fu to { l = loadRegisterToRegister(a) },
        0x70u to { loadRegisterToHlAddress(b) },
        0x71u to { loadRegisterToHlAddress(c) },
        0x72u to { loadRegisterToHlAddress(d) },
        0x73u to { loadRegisterToHlAddress(e) },
        0x74u to { loadRegisterToHlAddress(h) },
        0x75u to { loadRegisterToHlAddress(l) },
        0x76u to { halt() },
        0x77u to { loadRegisterToHlAddress(a) },
        0x78u to { a = loadRegisterToRegister(b) },
        0x79u to { a = loadRegisterToRegister(c) },
        0x7Au to { a = loadRegisterToRegister(d) },
        0x7Bu to { a = loadRegisterToRegister(e) },
        0x7Cu to { a = loadRegisterToRegister(h) },
        0x7Du to { a = loadRegisterToRegister(l) },
        0x7Eu to { a = loadRegisterFromHlAddress() },
        0x7Fu to { a = loadRegisterToRegister(a) },
        0x80u to { addARegister(b) },
        0x81u to { addARegister(c) },
        0x82u to { addARegister(d) },
        0x83u to { addARegister(e) },
        0x84u to { addARegister(h) },
        0x85u to { addARegister(l) },
        0x86u to { addAHl() },
        0x87u to { addARegister(a) },
        0x88u to { adcARegister(b) },
        0x89u to { adcARegister(c) },
        0x8Au to { adcARegister(d) },
        0x8Bu to { adcARegister(e) },
        0x8Cu to { adcARegister(h) },
        0x8Du to { adcARegister(l) },
        0x8Eu to { adcAHl() },
        0x8Fu to { adcARegister(a) },
        0x90u to { subRegister(b) },
        0x91u to { subRegister(c) },
        0x92u to { subRegister(d) },
        0x93u to { subRegister(e) },
        0x94u to { subRegister(h) },
        0x95u to { subRegister(l) },
        0x96u to { subHl() },
        0x97u to { subRegister(a) },
        0x98u to { sbcARegister(b) },
        0x99u to { sbcARegister(c) },
        0x9Au to { sbcARegister(d) },
        0x9Bu to { sbcARegister(e) },
        0x9Cu to { sbcARegister(h) },
        0x9Du to { sbcARegister(l) },
        0x9Eu to { sbcAHl() },
        0x9Fu to { sbcARegister(a) },
        0xA0u to { andARegister(b) },
        0xA1u to { andARegister(c) },
        0xA2u to { andARegister(d) },
        0xA3u to { andARegister(e) },
        0xA4u to { andARegister(h) },
        0xA5u to { andARegister(l) },
        0xA6u to { andAHl() },
        0xA7u to { andARegister(a) },
        0xA8u to { xorARegister(b) },
        0xA9u to { xorARegister(c) },
        0xAAu to { xorARegister(d) },
        0xABu to { xorARegister(e) },
        0xACu to { xorARegister(h) },
        0xADu to { xorARegister(l) },
        0xAEu to { xorAHl() },
        0xAFu to { xorARegister(a) },
        0xB0u to { orARegister(b) },
        0xB1u to { orARegister(c) },
        0xB2u to { orARegister(d) },
        0xB3u to { orARegister(e) },
        0xB4u to { orARegister(h) },
        0xB5u to { orARegister(l) },
        0xB6u to { orAHl() },
        0xB7u to { orARegister(a) },
        0xB8u to { cpRegister(b) },
        0xB9u to { cpRegister(c) },
        0xBAu to { cpRegister(d) },
        0xBBu to { cpRegister(e) },
        0xBCu to { cpRegister(h) },
        0xBDu to { cpRegister(l) },
        0xBEu to { cpHl() },
        0xBFu to { cpRegister(a) },
        0xC0u to { retNz() },
        0xC1u to { popPairRegister().destructureAssign(::b, ::c) },
        0xC2u to { jpNz() },
        0xC3u to { jpA16() },
        0xCAu to { jpZ() },
        0xD2u to { jpNc() },
        0xDAu to { jpC() },
        0xC4u to { callNz() },
        0xC5u to { pushPairRegister(b, c) },
        0xC6u to { addAN8() },
        0xC7u to { rst(0x0000u) },
        0xC8u to { retZ() },
        0xC9u to { ret() },
        0xCCu to { callZ() },
        0xCDu to { call() },
        0xCEu to { adcAN8() },
        0xCFu to { rst(0x0008u) },
        0xD0u to { retNc() },
        0xD1u to { popPairRegister().destructureAssign(::d, ::e) },
        0xD4u to { callNc() },
        0xD5u to { pushPairRegister(d, e) },
        0xD6u to { subN8() },
        0xD7u to { rst(0x0010u) },
        0xD9u to { retI() },
        0xDCu to { callC() },
        0xDEu to { sbcAN8() },
        0xDFu to { rst(0x0018u) },
        0xE0u to { loadFF00N8() },
        0xE1u to { popPairRegister().destructureAssign(::h, ::l) },
        0xE2u to { loadFF00C() },
        0xE5u to { pushPairRegister(h, l) },
        0xE6u to { andAN8() },
        0xEEu to { xorAN8() },
        0xE7u to { rst(0x0020u) },
        0xE9u to { jpHl() },
        0xEAu to { loadN16A() },
        0xEFu to { rst(0x0028u) },
        0xF0u to { loadAFF00N8() },
        0xF1u to { popPairRegister().destructureAssign(::a, ::f) },
        0xF3u to { di() },
        0xF5u to { pushPairRegister(a, f) },
        0xF6u to { orAN8() },
        0xF7u to { rst(0x0030u) },
        0xFAu to { loadAN16() },
        0xFBu to { ei() },
        0xFEu to { cpImmediate() },
        0xFFu to { rst(0x0038u) },
    )

    fun step() {
        lastT = t
        lastM = m
        if (halted) { t += 4; m++; handleInterrupts(); return }
        if (isDebug) tracer.step()
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
        handleInterrupts()
    }

    private fun handleInterrupts() {
        val ifReg = memory.readByte(0xFF0Fu)
        val ieReg = memory.readByte(0xFFFFu)
        val pendingAndEnabled = ifReg and ieReg
        if (pendingAndEnabled == 0u.toUByte()) return
        if (halted) halted = false
        if (!imeEnabled) return
        for (interrupt in InterruptType.getAllTypes()) {
            if (pendingAndEnabled and interrupt.mask != 0u.toUByte()) {
                imeEnabled = false
                memory.clearInterruptRequest(interrupt)
                pushWordToStack(pc)
                pc = interrupt.address
                t += 20; m += 5
                break
            }
        }
    }

    private fun pushWordToStack(value: UShort) {
        val low = (value and 0xFFu).toUByte()
        val high = ((value.toUInt() shr 8) and 0xFFu).toUByte()
        memory.writeByte((sp - 1u).toUShort(), high)
        memory.writeByte((sp - 2u).toUShort(), low)
        sp = (sp - 2u).toUShort()
    }

    internal fun combinateBytes(high: UByte, low: UByte) = registers.combinateBytes(high, low)
    internal fun resetFlags() = registers.resetFlags()
    internal fun setFlag(flag: UByte) = registers.setFlag(flag)
    internal fun unsetFlag(flag: UByte) = registers.unsetFlag(flag)
    internal fun isFlagSet(flag: UByte) = registers.isFlagSet(flag)

    private fun <A, B> Pair<A, B>.destructureAssign(a: KMutableProperty0<A>, b: KMutableProperty0<B>) {
        a.set(this.first)
        b.set(this.second)
    }

    companion object {
        val FLAG_Z = 0b1000_0000u.toUByte()
        val FLAG_N = 0b0100_0000u.toUByte()
        val FLAG_H = 0b0010_0000u.toUByte()
        val FLAG_C = 0b0001_0000u.toUByte()
    }
}