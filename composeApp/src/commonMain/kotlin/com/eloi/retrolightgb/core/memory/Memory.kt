package com.eloi.retrolightgb.core.memory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalUnsignedTypes::class)
class Memory {
    private val ram = UByteArray(RAM_SIZE)
    private val bootRom: UByteArray = BootRom.data.map { byte -> byte.toUByte() }.toUByteArray()
    private var isBootRomEnabled = true

    var dump by mutableStateOf(toString())
        private set

    fun load(rom: ByteArray) {
        for (i in rom.indices) {
            ram[i] = rom[i].toUByte()
        }
    }

    fun readByte(address: UShort): UByte {
        val addr = address.toInt()
        return when {
            isBootRomEnabled && addr in 0 until bootRom.size -> bootRom[addr]
            else -> ram[addr]
        }
    }

    fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt()

        if (addr == BOOT_DISABLED_ADDRESS && value == 0x01u.toUByte())
            isBootRomEnabled = false

        ram[addr] = value

        dump = toString()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Memory Map:\n")
        sb.append("  Boot ROM enabled: $isBootRomEnabled\n")
//        sb.append("  VRAM content:")
//        for (i in 0x8000 until 0xA000) {
//            if (i % 16 == 0) {
//                sb.append("\n    0x${i.toHexString()}: ")
//            }
//            sb.append("${ram[i].toHexString()} ")
//        }
//        sb.append("\n  OAM content:")
//        for (i in 0xFE00 until 0xFEA0) {
//            if (i % 16 == 0) {
//                sb.append("\n    0x${i.toHexString()}: ")
//            }
//            sb.append("${ram[i].toHexString()} ")
//        }
        sb.append("\n  IO RAM content:")
        for (i in 0xFF00 until 0xFF80) {
            if (i % 16 == 0) {
                sb.append("\n    0x${i.toHexString()}: ")
            }
            sb.append("${ram[i].toHexString()} ")
        }
        sb.append("\n  H RAM content:")
        for (i in 0xFF80 .. 0xFFFF) {
            if (i % 16 == 0) {
                sb.append("\n    0x${i.toHexString()}: ")
            }
            sb.append("${ram[i].toHexString()} ")
        }
        return sb.toString()
    }

    companion object {
        private const val RAM_SIZE = 0x10000 // 64KB
        private const val BOOT_DISABLED_ADDRESS = 0xFF50
    }
}