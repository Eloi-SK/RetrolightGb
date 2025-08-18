package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.memory.Memory

class Cpu(private val memory: Memory, private val isDebug: Boolean = false) {
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

    fun step() {
        val opcode = memory.readByte(pc)

        when (opcode.toUInt()) {
            0x00u -> {
                if (isDebug)
                    println("$${pc.toHexString()} NOP")

                pc++
                t += 4
                m++
            }
            0x06u -> {
                b = memory.readByte((pc + 1u).toUShort())

                if (isDebug)
                    println("$${pc.toHexString()} LD B, $${b.toHexString()}")

                pc = (pc + 2u).toUShort()
                t += 8
                m += 2
            }
            0x0Cu -> {
                c++
                unsetFlag(FLAG_N)
                if (c == 0u.toUByte()) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)
                if (c and 0x0Fu.toUByte() == 0u.toUByte()) setFlag(FLAG_H) else unsetFlag(FLAG_H)

                if (isDebug)
                    println("$${pc.toHexString()} INC C")

                pc++
                t += 4
                m++
            }
            0x0Eu -> {
                c = memory.readByte((pc + 1u).toUShort())

                if (isDebug)
                    println("$${pc.toHexString()} LD C, $${c.toHexString()}")

                pc = (pc + 2u).toUShort()
                t += 8
                m += 2
            }
            0x11u -> {
                val low = memory.readByte((pc + 1u).toUShort())
                val high = memory.readByte((pc + 2u).toUShort())

                val value = combinateBytes(high, low)
                d = high
                e = low

                if (isDebug)
                    println("$${pc.toHexString()} LD DE, $${value.toHexString()}")

                pc = (pc + 3u).toUShort()
                t += 12
                m += 3
            }
            0x1Au -> {
                a = memory.readByte(de)

                if (isDebug)
                    println("$${pc.toHexString()} LD A, (DE)")

                pc++
                t += 8
                m += 2
            }
            0x20u -> {
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
            0x21u -> {
                val low = memory.readByte((pc + 1u).toUShort())
                val high = memory.readByte((pc + 2u).toUShort())

                h = high
                l = low

                if (isDebug)
                    println("$${pc.toHexString()} LD HL, $${hl.toHexString()}")

                pc = (pc + 3u).toUShort()
                t += 12
                m += 3
            }
            0x31u -> {
                val low = memory.readByte((pc + 1u).toUShort())
                val high = memory.readByte((pc + 2u).toUShort())
                sp = combinateBytes(high, low)

                if (isDebug)
                    println("$${pc.toHexString()} LD SP, $${sp.toHexString()}")

                pc = (pc + 3u).toUShort()
                t += 12
                m += 3
            }
            0x32u -> {
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
            0x33u -> {
                sp++

                if (isDebug)
                    println("$${pc.toHexString()} INC SP")

                pc++
                t += 8
                m += 2
            }
            0x3Eu -> {
                a = memory.readByte((pc + 1u).toUShort())

                if (isDebug)
                    println("$${pc.toHexString()} LD A, $${a.toHexString()}")

                pc = (pc + 2u).toUShort()
                t += 8
                m += 2
            }
            0x4Fu -> {
                c = a

                if (isDebug)
                    println("$${pc.toHexString()} LD C, A")

                pc++
                t += 4
                m++
            }
            0x77u -> {
                memory.writeByte(hl, a)

                if (isDebug)
                    println("$${pc.toHexString()} LD (HL), A")

                pc++
                t += 8
                m += 2
            }
            0xAFu -> {
                a = a xor a
                resetFlags()
                setFlag(FLAG_Z)

                if (isDebug)
                    println("$${pc.toHexString()} XOR A")

                pc++
                t += 4
                m++
            }
            0xCBu -> {
                val cbOpcode = memory.readByte((pc + 1u).toUShort())
                cbPrefixStep(cbOpcode)
            }
            0xCDu -> {
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
            0xE0u -> {
                val value = memory.readByte((pc + 1u).toUShort())
                val address = combinateBytes(high = 0xFFu, low = value)
                memory.writeByte(address, a)

                if(isDebug)
                    println("$${pc.toHexString()} LD (\$FF00+$${value.toHexString()}), A")

                pc = (pc + 2u).toUShort()
                t += 12
                m += 3
            }
            0xE2u -> {
                val address = combinateBytes(high = 0xFFu, low = c)
                memory.writeByte(address, a)

                if (isDebug)
                    println("$${pc.toHexString()} LD (\$FF00+C), A")

                pc++
                t += 8
                m += 2
            }
            else -> {
                println(memory.toString())
                throw NotImplementedError("Opcode 0x${opcode.toHexString().uppercase()} not implemented")
            }
        }
    }

    private fun cbPrefixStep(cbOpcode: UByte) = when (cbOpcode.toUInt()) {
        0x7Cu -> {
            val bitValue = (h.toInt() shr 7) and 0x01

            setFlag(FLAG_H)
            unsetFlag(FLAG_N)
            if (bitValue == 0) setFlag(FLAG_Z) else unsetFlag(FLAG_Z)

            if (isDebug)
                println("$${pc.toHexString()} BIT 7, H")

            pc = (pc + 2u).toUShort()
            t += 8
            m += 2
        }
        else -> throw NotImplementedError("CB prefix opcode 0x${cbOpcode.toHexString().uppercase()} not implemented")
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

    private fun isFlagSet(flag: UByte): Boolean = (f and flag) != 0u.toUByte()

    companion object {
        val FLAG_Z = 0b1000_0000u.toUByte() // Zero flag
        val FLAG_N = 0b0100_0000u.toUByte() // Subtract flag
        val FLAG_H = 0b0010_0000u.toUByte() // Half carry flag
        val FLAG_C = 0b0001_0000u.toUByte() // Carry flag
    }
}