package com.eloi.retrolightgb.core.memory

// MBC3: up to 2MB ROM (128 banks × 16KB), 32KB RAM (4 banks × 8KB), and optional RTC.
// RTC registers (0x08–0x0C) are accepted but not emulated.
@OptIn(ExperimentalUnsignedTypes::class)
class Mbc3(private val rom: UByteArray) : Cartridge {
    private val ram = UByteArray(0x8000)
    private var romBank = 1
    private var ramBank = 0
    private var ramEnabled = false

    override fun readByte(address: Int): UByte = when (address) {
        in 0x0000..0x3FFF -> if (address < rom.size) rom[address] else 0xFFu
        in 0x4000..0x7FFF -> {
            val offset = romBank * 0x4000 + (address - 0x4000)
            if (offset < rom.size) rom[offset] else 0xFFu
        }
        in 0xA000..0xBFFF -> {
            if (ramEnabled && ramBank < 4) ram[ramBank * 0x2000 + (address - 0xA000)] else 0xFFu
        }
        else -> 0xFFu
    }

    override fun writeByte(address: Int, value: UByte) {
        when (address) {
            in 0x0000..0x1FFF -> ramEnabled = value.toInt() and 0x0F == 0x0A
            in 0x2000..0x3FFF -> romBank = (value.toInt() and 0x7F).let { if (it == 0) 1 else it }
            in 0x4000..0x5FFF -> ramBank = value.toInt() and 0x0F
            in 0x6000..0x7FFF -> Unit // RTC latch — not emulated
            in 0xA000..0xBFFF -> {
                if (ramEnabled && ramBank < 4) ram[ramBank * 0x2000 + (address - 0xA000)] = value
            }
        }
    }
}
