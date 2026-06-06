package com.eloi.retrolightgb.core.memory

// MBC5: up to 8MB ROM (512 banks × 16KB) and 128KB RAM (16 banks × 8KB).
// ROM bank is 9 bits: lower 8 bits at 0x2000–0x2FFF, bit 8 at 0x3000–0x3FFF.
// Unlike MBC1/MBC3, bank 0 is valid in the switchable area and needs no remapping.
// Rumble bit (bit 3 of RAM bank register on rumble carts) is accepted but ignored.
@OptIn(ExperimentalUnsignedTypes::class)
class Mbc5(private val rom: UByteArray) : Cartridge {
    private val ram = UByteArray(0x20000) // 128 KB — maximum MBC5 RAM
    private var romBankLo = 1  // bits 7:0 — register at 0x2000–0x2FFF
    private var romBankHi = 0  // bit 8   — register at 0x3000–0x3FFF
    private var ramBank = 0    // bits 3:0 — register at 0x4000–0x5FFF
    private var ramEnabled = false

    private val romBank: Int get() = (romBankHi shl 8) or romBankLo

    override fun saveRam(): ByteArray = ByteArray(ram.size) { ram[it].toByte() }
    override fun loadRam(data: ByteArray) {
        val len = minOf(data.size, ram.size)
        for (i in 0 until len) ram[i] = data[i].toUByte()
    }

    override fun readByte(address: Int): UByte = when (address) {
        in 0x0000..0x3FFF -> if (address < rom.size) rom[address] else 0xFFu
        in 0x4000..0x7FFF -> {
            val offset = romBank * 0x4000 + (address - 0x4000)
            if (offset < rom.size) rom[offset] else 0xFFu
        }
        in 0xA000..0xBFFF -> {
            if (ramEnabled) ram[ramBank * 0x2000 + (address - 0xA000)] else 0xFFu
        }
        else -> 0xFFu
    }

    override fun writeByte(address: Int, value: UByte) {
        when (address) {
            in 0x0000..0x1FFF -> ramEnabled = value.toInt() and 0x0F == 0x0A
            in 0x2000..0x2FFF -> romBankLo = value.toInt() and 0xFF
            in 0x3000..0x3FFF -> romBankHi = value.toInt() and 0x01
            in 0x4000..0x5FFF -> ramBank = value.toInt() and 0x0F // bit 3 = rumble on some carts, ignored
            in 0xA000..0xBFFF -> {
                if (ramEnabled) ram[ramBank * 0x2000 + (address - 0xA000)] = value
            }
        }
    }
}