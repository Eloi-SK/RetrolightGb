package com.eloi.retrolightgb.core.memory

// MBC2: up to 256 KB ROM (16 banks × 16 KB) and 512 × 4-bit built-in RAM (no external RAM).
//
// Quirk: writes to 0x0000–0x3FFF use bit 8 of the address to pick the register:
//   bit 8 == 0  →  RAM enable  (0x0A enables, anything else disables)
//   bit 8 == 1  →  ROM bank select (lower 4 bits; bank 0 remaps to 1)
//
// RAM is only 512 bytes but mirrors across the full 0xA000–0xBFFF window.
// Only the lower nibble of each byte is valid; the upper nibble always reads back as 0xF.
@OptIn(ExperimentalUnsignedTypes::class)
class Mbc2(private val rom: UByteArray) : Cartridge {
    private val ram = UByteArray(512)
    private var romBank = 1
    private var ramEnabled = false

    override fun readByte(address: Int): UByte = when (address) {
        in 0x0000..0x3FFF -> if (address < rom.size) rom[address] else 0xFFu
        in 0x4000..0x7FFF -> {
            val offset = romBank * 0x4000 + (address - 0x4000)
            if (offset < rom.size) rom[offset] else 0xFFu
        }
        in 0xA000..0xBFFF -> {
            if (ramEnabled) (ram[address and 0x01FF] or 0xF0u) else 0xFFu
        }
        else -> 0xFFu
    }

    override fun writeByte(address: Int, value: UByte) {
        when (address) {
            in 0x0000..0x3FFF -> {
                if (address and 0x0100 == 0) {
                    ramEnabled = value.toInt() and 0x0F == 0x0A
                } else {
                    romBank = (value.toInt() and 0x0F).let { if (it == 0) 1 else it }
                }
            }
            in 0xA000..0xBFFF -> {
                if (ramEnabled) ram[address and 0x01FF] = (value and 0x0Fu)
            }
        }
    }
}