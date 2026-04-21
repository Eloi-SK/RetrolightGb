package com.eloi.retrolightgb.core.memory

import com.eloi.retrolightgb.core.cpu.InterruptType

@OptIn(ExperimentalUnsignedTypes::class)
class Memory {
    private val ram = UByteArray(RAM_SIZE)
    private val bootRom: UByteArray = BootRom.data.map { byte -> byte.toUByte() }.toUByteArray()
    private var isBootRomEnabled = true
    private var ifRegister: UByte = 0u
    private var ieRegister: UByte = 0u

    fun load(rom: ByteArray) {
        for (i in rom.indices) {
            ram[i] = rom[i].toUByte()
        }
    }

    fun requestInterrupt(type: InterruptType) {
        ifRegister = ifRegister or type.mask
    }

    fun clearInterruptRequest(type: InterruptType) {
        ifRegister = ifRegister and type.mask.inv()
    }

    fun readByte(address: UShort): UByte {
        val addr = address.toInt()
        return when {
            addr == 0xFF0F -> ifRegister
            addr == 0xFFFF -> ieRegister
            isBootRomEnabled && addr in 0 until bootRom.size -> bootRom[addr]
            else -> ram[addr]
        }
    }

    fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt()

        if (addr == BOOT_DISABLED_ADDRESS && value == 0x01u.toUByte())
            isBootRomEnabled = false

        when (addr) {
            0xFF0F -> ifRegister = value
            0xFFFF -> ieRegister = value
            else -> ram[addr] = value
        }
    }

    companion object {
        private const val RAM_SIZE = 0x10000 // 64KB
        private const val BOOT_DISABLED_ADDRESS = 0xFF50
    }
}