package com.eloi.retrolightgb.core.memory

@OptIn(ExperimentalUnsignedTypes::class)
class Memory {
    private val ram = UByteArray(RAM_SIZE)
    private val bootRom: UByteArray = BootRom.data.map { byte -> byte.toUByte() }.toUByteArray()
    private var isBootRomEnabled = true

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
    }

    companion object {
        private const val RAM_SIZE = 0x10000 // 64KB
        private const val BOOT_DISABLED_ADDRESS = 0xFF50
    }
}