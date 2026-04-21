package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.memory.Memory
import kotlin.concurrent.Volatile
import kotlin.reflect.KMutableProperty0

class Cpu(val memory: Memory, val isDebug: Boolean = false) {

    private val traceArray = arrayOfNulls<String>(TRACE_BUFFER_SIZE)
    private var traceHead = 0
    private var traceFull = false
    var traceWriter: ((String) -> Unit)? = null

    private fun trace(msg: String) {
        val writer = traceWriter
        if (writer != null) {
            writer(msg)
        } else {
            traceArray[traceHead] = msg
            traceHead++
            if (traceHead >= TRACE_BUFFER_SIZE) { traceHead = 0; traceFull = true }
        }
    }

    private fun traceStep() {
        val b0 = memory.readByte(pc).toHexString().uppercase()
        val b1 = memory.readByte((pc + 1u).toUShort()).toHexString().uppercase()
        val b2 = memory.readByte((pc + 2u).toUShort()).toHexString().uppercase()
        val b3 = memory.readByte((pc + 3u).toUShort()).toHexString().uppercase()
        trace("A:${a.toHexString().uppercase()} F:${f.toHexString().uppercase()} B:${b.toHexString().uppercase()} C:${c.toHexString().uppercase()} D:${d.toHexString().uppercase()} E:${e.toHexString().uppercase()} H:${h.toHexString().uppercase()} L:${l.toHexString().uppercase()} SP:${sp.toHexString().uppercase()} PC:${pc.toHexString().uppercase()} PCMEM:$b0,$b1,$b2,$b3")
    }

    fun dumpTrace(): String {
        val count = if (traceFull) TRACE_BUFFER_SIZE else traceHead
        val start = if (traceFull) traceHead else 0
        return buildString {
            for (i in 0 until count) {
                if (i > 0) appendLine()
                append(traceArray[(start + i) % TRACE_BUFFER_SIZE] ?: "")
            }
        }
    }

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

    val hl: UShort
        get() = combinateBytes(h, l)
    val bc: UShort
        get() = combinateBytes(b, c)
    val de: UShort
        get() = combinateBytes(d, e)

    private val cbInstructions: Map<UInt, () -> Unit> = mapOf(
        0x10u to { b = rlRegister(registerName = "B", registerValue = b) },
        0x11u to { c = rlRegister(registerName = "C", registerValue = c) },
        0x12u to { d = rlRegister(registerName = "D", registerValue = d) },
        0x13u to { e = rlRegister(registerName = "E", registerValue = e) },
        0x14u to { h = rlRegister(registerName = "H", registerValue = h) },
        0x15u to { l = rlRegister(registerName = "L", registerValue = l) },
        0x17u to { a = rlRegister(registerName = "A", registerValue = a) },
        0x30u to { b = swapRegister(registerName = "B", registerValue = b) },
        0x31u to { c = swapRegister(registerName = "C", registerValue = c) },
        0x32u to { d = swapRegister(registerName = "D", registerValue = d) },
        0x33u to { e = swapRegister(registerName = "E", registerValue = e) },
        0x34u to { h = swapRegister(registerName = "H", registerValue = h) },
        0x35u to { l = swapRegister(registerName = "L", registerValue = l) },
        0x36u to ::swapHl,
        0x37u to { a = swapRegister(registerName = "A", registerValue = a) },
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
        0x80u to { b = resetBitRegister(bit = 0, registerName = "B", registerValue = b) },
        0x81u to { c = resetBitRegister(bit = 0, registerName = "C", registerValue = c) },
        0x82u to { d = resetBitRegister(bit = 0, registerName = "D", registerValue = d) },
        0x83u to { e = resetBitRegister(bit = 0, registerName = "E", registerValue = e) },
        0x84u to { h = resetBitRegister(bit = 0, registerName = "H", registerValue = h) },
        0x85u to { l = resetBitRegister(bit = 0, registerName = "L", registerValue = l) },
        0x87u to { a = resetBitRegister(bit = 0, registerName = "A", registerValue = a) },
    )
    
    private val instructions: Map<UInt, () -> Unit> = mapOf(
        0x00u to ::nop,
        0x01u to { loadPairRegisterN16(pairRegisterName = "BC").destructureAssign(::b, ::c) },
        0x02u to { loadRegisterToAddressPair(pairName = "BC", pairValue = bc) },
        0x03u to { incRegisterPair(pairName = "BC", pairValue = bc).destructureAssign(::b, ::c) },
        0x04u to { b = incRegister(registerName = "B", registerValue = b) },
        0x05u to { b = decRegister(registerName = "B", registerValue = b) },
        0x06u to { b = loadRegisterN8(registerName = "B") },
        0x09u to { addHlPair(pairName = "BC", pairValue = bc) },
        0x0Au to { loadPairRegisterAddressToA(pairName = "BC", pairValue = bc) },
        0x0Bu to { decRegisterPair(pairName = "BC", pairValue = bc).destructureAssign(::b, ::c) },
        0x0Cu to { c = incRegister(registerName = "C", registerValue = c) },
        0x0Du to { c = decRegister(registerName = "C", registerValue = c) },
        0x0Eu to { c = loadRegisterN8(registerName = "C") },
        0x11u to { loadPairRegisterN16(pairRegisterName = "DE").destructureAssign(::d, ::e) },
        0x12u to { loadRegisterToAddressPair(pairName = "DE", pairValue = de) },
        0x13u to { incRegisterPair(pairName = "DE", pairValue = de).destructureAssign(::d, ::e) },
        0x14u to { d = incRegister(registerName = "D", registerValue = d) },
        0x15u to { d = decRegister(registerName = "D", registerValue = d) },
        0x16u to { d = loadRegisterN8(registerName = "D") },
        0x17u to ::rla,
        0x18u to ::jr,
        0x19u to { addHlPair(pairName = "DE", pairValue = de) },
        0x1Au to { loadPairRegisterAddressToA(pairName = "DE", pairValue = de) },
        0x1Cu to { e = incRegister(registerName = "E", registerValue = e) },
        0x1Du to { e = decRegister(registerName = "E", registerValue = e) },
        0x1Eu to { e = loadRegisterN8(registerName = "E") },
        0x20u to ::jrNz,
        0x21u to { loadPairRegisterN16(pairRegisterName = "HL").destructureAssign(::h, ::l) },
        0x22u to ::loadHlIncA,
        0x23u to { incRegisterPair(pairName = "HL", pairValue = hl).destructureAssign(::h, ::l) },
        0x24u to { h = incRegister(registerName = "H", registerValue = h) },
        0x25u to { h = decRegister(registerName = "H", registerValue = h) },
        0x26u to { h = loadRegisterN8(registerName = "H") },
        0x28u to ::jrZ,
        0x29u to { addHlPair(pairName = "HL", pairValue = hl) },
        0x2Au to ::loadAHlInc,
        0x2Cu to { l = incRegister(registerName = "L", registerValue = l) },
        0x2Du to { l = decRegister(registerName = "L", registerValue = l) },
        0x2Eu to { l = loadRegisterN8(registerName = "L") },
        0x2Fu to ::cpl,
        0x31u to ::loadSpN16,
        0x32u to ::loadHlDecA,
        0x33u to ::incSp,
        0x34u to ::incHlAddress,
        0x36u to ::ldHLN8,
        0x39u to { addHlPair(pairName = "SP", pairValue = sp) },
        0x3Cu to { a = incRegister(registerName = "A", registerValue = a) },
        0x3Du to { a = decRegister(registerName = "A", registerValue = a) },
        0x3Eu to { a = loadRegisterN8(registerName = "A") },
        0x40u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "B", reg2Value = b) },
        0x41u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "C", reg2Value = c) },
        0x42u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "D", reg2Value = d) },
        0x43u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "E", reg2Value = e) },
        0x44u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "H", reg2Value = h) },
        0x45u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "L", reg2Value = l) },
        0x46u to { b = loadRegisterFromHlAddress(registerName = "B") },
        0x47u to { b = loadRegisterToRegister(reg1Name = "B", reg2Name = "A", reg2Value = a) },
        0x48u to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "B", reg2Value = b) },
        0x49u to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "C", reg2Value = c) },
        0x4Au to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "D", reg2Value = d) },
        0x4Bu to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "E", reg2Value = e) },
        0x4Cu to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "H", reg2Value = h) },
        0x4Du to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "L", reg2Value = l) },
        0x4Eu to { c = loadRegisterFromHlAddress(registerName = "C") },
        0x4Fu to { c = loadRegisterToRegister(reg1Name = "C", reg2Name = "A", reg2Value = a) },
        0x50u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "B", reg2Value = b) },
        0x51u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "C", reg2Value = c) },
        0x52u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "D", reg2Value = d) },
        0x53u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "E", reg2Value = e) },
        0x54u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "H", reg2Value = h) },
        0x55u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "L", reg2Value = l) },
        0x56u to { d = loadRegisterFromHlAddress(registerName = "D") },
        0x57u to { d = loadRegisterToRegister(reg1Name = "D", reg2Name = "A", reg2Value = a) },
        0x58u to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "B", reg2Value = b) },
        0x59u to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "C", reg2Value = c) },
        0x5Au to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "D", reg2Value = d) },
        0x5Bu to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "E", reg2Value = e) },
        0x5Cu to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "H", reg2Value = h) },
        0x5Du to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "L", reg2Value = l) },
        0x5Eu to { e = loadRegisterFromHlAddress(registerName = "E") },
        0x5Fu to { e = loadRegisterToRegister(reg1Name = "E", reg2Name = "A", reg2Value = a) },
        0x60u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "B", reg2Value = b) },
        0x61u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "C", reg2Value = c) },
        0x62u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "D", reg2Value = d) },
        0x63u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "E", reg2Value = e) },
        0x64u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "H", reg2Value = h) },
        0x65u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "L", reg2Value = l) },
        0x66u to { h = loadRegisterFromHlAddress(registerName = "H") },
        0x67u to { h = loadRegisterToRegister(reg1Name = "H", reg2Name = "A", reg2Value = a) },
        0x68u to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "B", reg2Value = b) },
        0x69u to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "C", reg2Value = c) },
        0x6Au to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "D", reg2Value = d) },
        0x6Bu to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "E", reg2Value = e) },
        0x6Cu to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "H", reg2Value = h) },
        0x6Du to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "L", reg2Value = l) },
        0x6Eu to { l = loadRegisterFromHlAddress(registerName = "L") },
        0x6Fu to { l = loadRegisterToRegister(reg1Name = "L", reg2Name = "A", reg2Value = a) },
        0x70u to { loadRegisterToHlAddress(registerName = "B", registerValue = b) },
        0x71u to { loadRegisterToHlAddress(registerName = "C", registerValue = c) },
        0x72u to { loadRegisterToHlAddress(registerName = "D", registerValue = d) },
        0x73u to { loadRegisterToHlAddress(registerName = "E", registerValue = e) },
        0x74u to { loadRegisterToHlAddress(registerName = "H", registerValue = h) },
        0x75u to { loadRegisterToHlAddress(registerName = "L", registerValue = l) },
        0x77u to { loadRegisterToHlAddress(registerName = "A", registerValue = a) },
        0x78u to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "B", reg2Value = b) },
        0x79u to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "C", reg2Value = c) },
        0x7Au to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "D", reg2Value = d) },
        0x7Bu to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "E", reg2Value = e) },
        0x7Cu to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "H", reg2Value = h) },
        0x7Du to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "L", reg2Value = l) },
        0x7Eu to { a = loadRegisterFromHlAddress(registerName = "A") },
        0x7Fu to { a = loadRegisterToRegister(reg1Name = "A", reg2Name = "A", reg2Value = a) },
        0x80u to { addARegister(registerName = "B", registerValue = b) },
        0x81u to { addARegister(registerName = "C", registerValue = c) },
        0x82u to { addARegister(registerName = "D", registerValue = d) },
        0x83u to { addARegister(registerName = "E", registerValue = e) },
        0x84u to { addARegister(registerName = "H", registerValue = h) },
        0x85u to { addARegister(registerName = "L", registerValue = l) },
        0x86u to ::addAHl,
        0x87u to { addARegister(registerName = "A", registerValue = a) },
        0x90u to { subRegister(registerName = "B", registerValue = b) },
        0x91u to { subRegister(registerName = "C", registerValue = c) },
        0x92u to { subRegister(registerName = "D", registerValue = d) },
        0x93u to { subRegister(registerName = "E", registerValue = e) },
        0x94u to { subRegister(registerName = "H", registerValue = h) },
        0x95u to { subRegister(registerName = "L", registerValue = l) },
        0x97u to { subRegister(registerName = "A", registerValue = a) },
        0xA0u to { andARegister(registerName = "B", registerValue = b) },
        0xA1u to { andARegister(registerName = "C", registerValue = c) },
        0xA2u to { andARegister(registerName = "D", registerValue = d) },
        0xA3u to { andARegister(registerName = "E", registerValue = e) },
        0xA4u to { andARegister(registerName = "H", registerValue = h) },
        0xA5u to { andARegister(registerName = "L", registerValue = l) },
        0xA7u to { andARegister(registerName = "A", registerValue = a) },
        0xA8u to { xorARegister(registerName = "B", registerValue = b) },
        0xA9u to { xorARegister(registerName = "C", registerValue = c) },
        0xAAu to { xorARegister(registerName = "D", registerValue = d) },
        0xABu to { xorARegister(registerName = "E", registerValue = e) },
        0xACu to { xorARegister(registerName = "H", registerValue = h) },
        0xADu to { xorARegister(registerName = "L", registerValue = l) },
        0xAFu to { xorARegister(registerName = "A", registerValue = a) },
        0xB0u to {  orARegister(registerName = "B", registerValue = b) },
        0xB1u to {  orARegister(registerName = "C", registerValue = c) },
        0xB2u to {  orARegister(registerName = "D", registerValue = d) },
        0xB3u to {  orARegister(registerName = "E", registerValue = e) },
        0xB4u to {  orARegister(registerName = "H", registerValue = h) },
        0xB5u to {  orARegister(registerName = "L", registerValue = l) },
        0xB7u to {  orARegister(registerName = "A", registerValue = a) },
        0xB8u to { cp(registerName = "B", registerValue = b) },
        0xB9u to { cp(registerName = "C", registerValue = c) },
        0xBAu to { cp(registerName = "D", registerValue = d) },
        0xBBu to { cp(registerName = "E", registerValue = e) },
        0xBCu to { cp(registerName = "H", registerValue = h) },
        0xBDu to { cp(registerName = "L", registerValue = l) },
        0xBEu to ::cpHl,
        0xBFu to { cp(registerName = "A", registerValue = a) },
        0xC0u to ::retNz,
        0xC1u to { popPairRegister(pairRegisterName = "BC").destructureAssign(::b, ::c) },
        0xC2u to ::jpNz,
        0xC3u to ::jpA16,
        0xCAu to ::jpZ,
        0xD2u to ::jpNc,
        0xDAu to ::jpC,
        0xC5u to { pushPairRegister(pairRegisterName = "BC", high = b, low = c) },
        0xC7u to { rst(0x0000u) },
        0xC8u to ::retZ,
        0xC9u to ::ret,
        0xCDu to ::call,
        0xCFu to { rst(0x0008u) },
        0xD1u to { popPairRegister(pairRegisterName = "DE").destructureAssign(::d, ::e) },
        0xD5u to { pushPairRegister(pairRegisterName = "DE", high = d, low = e) },
        0xD7u to { rst(0x0010u) },
        0xD9u to ::retI,
        0xDFu to { rst(0x0018u) },
        0xE0u to ::loadFF00N8,
        0xE1u to { popPairRegister(pairRegisterName = "HL").destructureAssign(::h, ::l) },
        0xE2u to ::loadFF00C,
        0xE5u to { pushPairRegister(pairRegisterName = "HL", high = h, low = l) },
        0xE6u to ::andAN8,
        0xE7u to { rst(0x0020u) },
        0xE9u to ::jpHl,
        0xEAu to ::loadN16A,
        0xEFu to { rst(0x0028u) },
        0xF0u to ::loadAFF00N8,
        0xF1u to { popPairRegister(pairRegisterName = "AF").destructureAssign(::a, ::f) },
        0xF3u to ::di,
        0xF5u to { pushPairRegister(pairRegisterName = "AF", high = a, low = f) },
        0xF7u to { rst(0x0030u) },
        0xFAu to ::loadAN16,
        0xFBu to ::ei,
        0xFEu to { cp() },
        0xFFu to { rst(0x0038u) },
    )

    fun step() {
        lastT = t
        lastM = m
        if (isDebug) traceStep()
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
        if (imeEnabled) {
            val ifReg = memory.readByte(0xFF0Fu)
            val ieReg = memory.readByte(0xFFFFu)
            val pendingAndEnabled = ifReg and ieReg

            if (pendingAndEnabled == 0u.toUByte()) return

            for (interrupt in InterruptType.getAllTypes()) {
                if (pendingAndEnabled and interrupt.mask != 0u.toUByte()) {
                    imeEnabled = false
                    memory.clearInterruptRequest(interrupt)

                    pushWordToStack(pc)
                    pc = interrupt.address
                    t += 20
                    m += 5

                    break
                }
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
        pc++
        t += 4
        m++
    }

    private fun loadRegisterFromHlAddress(registerName: String): UByte {
        val value = memory.readByte(hl)
        pc++
        t += 8
        m += 2
        return value
    }

    private fun loadRegisterN8(registerName: String): UByte {
        val newValue = memory.readByte((pc + 1u).toUShort())
        pc = (pc + 2u).toUShort()
        t += 8
        m += 2
        return newValue
    }

    private fun loadPairRegisterN16(pairRegisterName: String): Pair<UByte, UByte> {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        pc = (pc + 3u).toUShort()
        t += 12
        m += 3
        return high to low
    }

    private fun loadN16A() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val value = combinateBytes(high, low)
        memory.writeByte(value, a)
        pc = (pc + 3u).toUShort()
        t += 16
        m += 4
    }

    private fun loadAN16() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val address = combinateBytes(high, low)
        a = memory.readByte(address)
        pc = (pc + 3u).toUShort()
        t += 16
        m += 4
    }

    private fun loadSpN16() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        sp = combinateBytes(high, low)
        pc = (pc + 3u).toUShort()
        t += 12
        m += 3
    }

    private fun incSp() {
        sp++
        pc++
        t += 8
        m += 2
    }

    private fun incHlAddress() {
        val value = memory.readByte(hl)
        val newValue = (value + 1u).toUByte()
        memory.writeByte(hl, newValue)

        unsetFlag(FLAG_N)
        if (newValue == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)

        if ((value and 0x0Fu) == 0x0Fu.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
        pc++
        t += 12
        m += 3
    }

    private fun incRegisterPair(pairName: String, pairValue: UShort): Pair<UByte, UByte> {
        val newValue = pairValue + 1u
        val high = (newValue shr 8).toUByte()
        val low = (newValue and 0xFFu).toUByte()
        pc++
        t += 8
        m += 2
        return high to low
    }

    private fun decRegisterPair(pairName: String, pairValue: UShort): Pair<UByte, UByte> {
        val newValue = pairValue - 1u
        val high = (newValue shr 8).toUByte()
        val low = (newValue and 0xFFu).toUByte()
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
        pc++
        t += 4
        m++
        return newValue
    }

    private fun decRegister(registerName: String, registerValue: UByte): UByte {
        val newValue = (registerValue - 1u).toUByte()
        setFlag(FLAG_N)
        if (newValue == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        if (registerValue and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)
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

        pc++
        t += 16
        m += 4
    }

    private fun popPairRegister(pairRegisterName: String): Pair<UByte, UByte> {
        val low = memory.readByte(sp)
        val high = memory.readByte((sp + 1u).toUShort())
        pc++
        sp = (sp + 2u).toUShort()
        t += 12
        m += 3
        return high to low
    }

    private fun cpl() {
        a = a.inv()
        setFlag(FLAG_N)
        setFlag(FLAG_H)
        pc++
        t += 4
        m++
    }

    private fun xorARegister(registerName: String, registerValue: UByte) {
        a = a xor registerValue
        resetFlags()
        if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        pc++
        t += 4
        m++
    }

    private fun orARegister(registerName: String, registerValue: UByte) {
        a = a or registerValue
        resetFlags()
        if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        pc++
        t += 4
        m++
    }

    private fun andARegister(registerName: String, registerValue: UByte) {
        a = a and registerValue
        resetFlags()
        if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        setFlag(FLAG_H)
        pc++
        t += 4
        m++
    }

    private fun andAN8() {
        val value = memory.readByte((pc + 1u).toUShort())
        a = a and value
        if (a == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        unsetFlag(FLAG_N)
        setFlag(FLAG_H)
        unsetFlag(FLAG_C)
        pc = (pc + 2u).toUShort()
        t += 8
        m += 2
    }

    private fun rla() {
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

    private fun call() {
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

    private fun ret() {
        val low = memory.readByte(sp)
        val high = memory.readByte((sp + 1u).toUShort())
        sp = (sp + 2u).toUShort()
        pc = combinateBytes(high, low)
        t += 16
        m += 4
    }

    private fun retZ() {
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

    private fun retNz() {
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

    private fun retI() {
        val low = memory.readByte(sp)
        val high = memory.readByte((sp + 1u).toUShort())
        sp = (sp + 2u).toUShort()
        imeEnabled = true
        pc = combinateBytes(high, low)
        t += 16
        m += 4
    }

    private fun cpHl() {
        val value = memory.readByte(hl)
        val result = a - value

        setFlag(FLAG_N)

        if (result.toUByte() == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)

        if (result.toUByte() and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)

        if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)
        pc++
        t += 8
        m += 2
    }

    private fun cp(registerName: String? = null, registerValue: UByte? = null) {
        val isImmediate = registerName == null && registerValue == null
        var result: UInt
        if (registerName != null && registerValue != null) {
            result = a - registerValue
        } else {
            val value = memory.readByte((pc + 1u).toUShort())
            result = a - value
        }

        setFlag(FLAG_N)

        if (result.toUByte() == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)

        if (result.toUByte() and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)

        if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)

        if (isImmediate) {
            pc = (pc + 2u).toUShort()
            t += 8
            m += 2
        } else {
            pc++
            t += 4
            m++
        }
    }

    private fun addARegister(registerName: String, registerValue: UByte) {
        val result = a.toInt() + registerValue.toInt()

        resetFlags()

        if ((result and 0xFF) == 0) setFlag(FLAG_Z)

        unsetFlag(FLAG_N)


        if (((a.toInt() and 0x0F) + (registerValue.toInt() and 0x0F)) > 0x0F) {
            setFlag(FLAG_H)
        }

        if (result > 0xFF) setFlag(FLAG_C)

        a = result.toUByte()
        pc++
        t += 4
        m += 1
    }

    private fun addAHl() {
        val value = memory.readByte(hl)
        val result = a.toInt() + value.toInt()

        resetFlags()
        if ((result and 0xFF) == 0) setFlag(FLAG_Z)
        if (result > 0xFF) setFlag(FLAG_C)

        if (((a.toInt() and 0x0F) + (value.toInt() and 0x0F)) > 0x0F) setFlag(FLAG_H)
        a = result.toUByte()
        pc++
        t += 8
        m += 2
    }

    private fun addHlPair(pairName: String, pairValue: UShort) {
        val result = hl.toInt() + pairValue.toInt()

        unsetFlag(FLAG_N)
        if (((hl.toInt() and 0x0FFF) + (pairValue.toInt() and 0x0FFF)) > 0x0FFF) setFlag(FLAG_H) else unsetFlag(FLAG_H)
        if (result > 0xFFFF) setFlag(FLAG_C) else unsetFlag(FLAG_C)

        val finalValue = result.toUShort()
        h = (finalValue.toInt() shr 8).toUByte()
        l = (finalValue.toInt() and 0xFF).toUByte()

        pc++
        t += 8
        m += 2
    }

    private fun loadRegisterToRegister(reg1Name: String, reg2Name: String, reg2Value: UByte): UByte {
        pc++
        t += 4
        m++
        return reg2Value
    }

    private fun loadRegisterToAddressPair(pairName: String, pairValue: UShort) {
        memory.writeByte(pairValue, a)
        pc++
        t += 8
        m += 2
    }

    private fun loadRegisterToHlAddress(registerName: String, registerValue: UByte) {
        memory.writeByte(hl, registerValue)
        pc++
        t += 8
        m += 2
    }

    private fun loadPairRegisterAddressToA(pairName: String, pairValue: UShort) {
        a = memory.readByte(pairValue)
        pc++
        t += 8
        m += 2
    }

    private fun loadHlIncA() {
        memory.writeByte(hl, a)
        val inc = hl + 1u
        h = (inc shr 8).toUByte()
        l = (inc and 0xFFu).toUByte()

        pc++
        t += 8
        m += 2
    }

    private fun loadHlDecA() {
        memory.writeByte(hl, a)
        val dec = hl - 1u
        h = (dec shr 8).toUByte()
        l = (dec and 0xFFu).toUByte()
        pc++
        t += 8
        m += 2
    }

    private fun loadAFF00N8() {
        val value = memory.readByte((pc + 1u).toUShort())
        val address = combinateBytes(high = 0xFFu, low = value)
        a = memory.readByte(address)

        pc = (pc + 2u).toUShort()
        t += 12
        m += 3
    }

    private fun loadFF00N8() {
        val value = memory.readByte((pc + 1u).toUShort())
        val address = combinateBytes(high = 0xFFu, low = value)
        memory.writeByte(address, a)
        pc = (pc + 2u).toUShort()
        t += 12
        m += 3
    }

    private fun loadFF00C() {
        val address = combinateBytes(high = 0xFFu, low = c)
        memory.writeByte(address, a)
        pc++
        t += 8
        m += 2
    }

    private fun jpHl() {
        pc = hl
        t += 4
        m += 1
    }

    private fun jr() {
        val offset = memory.readByte((pc + 1u).toUShort()).toByte()
        pc = (pc.toInt() + 2 + offset.toInt()).toUShort()
        t += 12
        m += 3
    }

    private fun jpA16() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val targetAddress = combinateBytes(high, low)
        pc = targetAddress
        t += 16
        m += 4
    }

    private fun jpNz() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val targetAddress = combinateBytes(high, low)
        if (!isFlagSet(FLAG_Z)) {
            pc = targetAddress
            t += 16
            m += 4
        } else {
            pc = (pc + 3u).toUShort()
            t += 12
            m += 3
        }
    }

    private fun jpZ() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val targetAddress = combinateBytes(high, low)
        if (isFlagSet(FLAG_Z)) {
            pc = targetAddress
            t += 16
            m += 4
        } else {
            pc = (pc + 3u).toUShort()
            t += 12
            m += 3
        }
    }

    private fun jpNc() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val targetAddress = combinateBytes(high, low)
        if (!isFlagSet(FLAG_C)) {
            pc = targetAddress
            t += 16
            m += 4
        } else {
            pc = (pc + 3u).toUShort()
            t += 12
            m += 3
        }
    }

    private fun jpC() {
        val low = memory.readByte((pc + 1u).toUShort())
        val high = memory.readByte((pc + 2u).toUShort())
        val targetAddress = combinateBytes(high, low)
        if (isFlagSet(FLAG_C)) {
            pc = targetAddress
            t += 16
            m += 4
        } else {
            pc = (pc + 3u).toUShort()
            t += 12
            m += 3
        }
    }

    private fun di() {
        imeEnabled = false
        pc++
        t += 4
        m++
    }

    private fun ei() {
        imeEnabled = true
        pc++
        t += 4
        m++
    }

    private fun ldHLN8() {
        val value = memory.readByte((pc + 1u).toUShort())
        memory.writeByte(hl, value)
        pc = (pc + 2u).toUShort()
        t += 12
        m += 3
    }

    private fun loadAHlInc() {
        a = memory.readByte(hl)

        val inc = hl + 1u
        h = (inc shr 8).toUByte()
        l = (inc and 0xFFu).toUByte()

        pc++
        t += 8
        m += 2
    }

    private fun jrZ() {
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

    private fun jrNz() {
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

        pc = (pc + 2u).toUShort()
        t += 8
        m += 2
    }

    private fun resetBitRegister(bit: Int, registerName: String, registerValue: UByte): UByte {
        val mask = (1 shl bit).inv() and 0xFF
        val result = (registerValue.toInt() and mask).toUByte()

        pc = (pc + 2u).toUShort()
        t += 8
        m += 2

        return result
    }

    private fun swapRegister(registerName: String, registerValue: UByte): UByte {
        val newValue = ((registerValue.toUInt() shl 4) or (registerValue.toUInt() shr 4)) and 0xFFu
        val result = newValue.toUByte()

        resetFlags()
        if (result == 0u.toUByte()) setFlag(FLAG_Z)

        pc = (pc + 2u).toUShort()
        t += 8
        m += 2
        return result
    }

    private fun swapHl() {
        val value = memory.readByte(hl)
        val result = ((value.toUInt() shl 4) or (value.toUInt() shr 4)) and 0xFFu
        memory.writeByte(hl, result.toUByte())
        resetFlags()
        if (result == 0u) setFlag(FLAG_Z)
        pc = (pc + 2u).toUShort()
        t += 16
        m += 4
    }

    private fun subRegister(registerName: String, registerValue: UByte) {
        val result = a - registerValue

        setFlag(FLAG_N)
        if (result == 0u) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
        if (result and 0x0Fu == 0u) setFlag(FLAG_H) else unsetFlag(FLAG_H)
        if (result.toInt() < 0) setFlag(FLAG_C) else unsetFlag(FLAG_C)

        a = result.toUByte()
        pc++
        t += 4
        m++
    }

    private fun rst(address: UShort) {
        val returnAddress = (pc + 1u).toUShort()
        sp--
        memory.writeByte(sp, (returnAddress.toInt() shr 8).toUByte())
        sp--
        memory.writeByte(sp, (returnAddress.toInt() and 0xFF).toUByte())

        pc = address
        t += 16
        m += 4
    }

    companion object {
        val FLAG_Z = 0b1000_0000u.toUByte() // Zero flag
        val FLAG_N = 0b0100_0000u.toUByte() // Subtract flag
        val FLAG_H = 0b0010_0000u.toUByte() // Half carry flag
        val FLAG_C = 0b0001_0000u.toUByte() // Carry flag
        private const val TRACE_BUFFER_SIZE = 200
    }
}