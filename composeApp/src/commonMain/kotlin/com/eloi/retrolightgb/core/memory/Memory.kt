package com.eloi.retrolightgb.core.memory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eloi.retrolightgb.core.cpu.InterruptType

@OptIn(ExperimentalUnsignedTypes::class)
class Memory {
    private val ram = UByteArray(RAM_SIZE)
    private val bootRom: UByteArray = BootRom.data.map { byte -> byte.toUByte() }.toUByteArray()
    private var isBootRomEnabled = true
    private var ifRegister: UByte = 0u
    private var ieRegister: UByte = 0u
    private var cartridge: Cartridge? = null

    private val serialBuffer = StringBuilder()
    var serialOutput by mutableStateOf("")
        private set

    fun load(rom: ByteArray) {
        val uRom = UByteArray(rom.size) { rom[it].toUByte() }
        cartridge = CartridgeFactory.create(uRom)
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
            isBootRomEnabled && addr in 0 until bootRom.size -> bootRom[addr]
            addr in 0x0000..0x7FFF -> cartridge?.readByte(addr) ?: 0xFFu
            addr in 0xA000..0xBFFF -> cartridge?.readByte(addr) ?: 0xFFu
            addr == 0xFF0F -> (ifRegister or 0xE0u).toUByte()
            addr == 0xFFFF -> ieRegister
            else -> ram[addr]
        }
    }

    fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt()

        if (addr == BOOT_DISABLED_ADDRESS && value == 0x01u.toUByte())
            isBootRomEnabled = false

        when (addr) {
            in 0x0000..0x7FFF -> cartridge?.writeByte(addr, value)
            in 0xA000..0xBFFF -> cartridge?.writeByte(addr, value)
            0xFF0F -> ifRegister = value
            0xFFFF -> ieRegister = value
            // Serial transfer: bit 7 = start, bit 0 = internal clock
            0xFF02 -> {
                ram[addr] = value
                if (value and 0x81u.toUByte() == 0x81u.toUByte()) {
                    serialBuffer.append(ram[0xFF01].toInt().toChar())
                    serialOutput = serialBuffer.toString()
                    ram[addr] = value and 0x7Fu.toUByte() // clear transfer-start bit
                }
            }
            else -> ram[addr] = value
        }
    }

    companion object {
        private const val RAM_SIZE = 0x10000
        private const val BOOT_DISABLED_ADDRESS = 0xFF50
    }
}
