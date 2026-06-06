package com.eloi.retrolightgb.core.memory

@OptIn(ExperimentalUnsignedTypes::class)
object CartridgeFactory {
    // Cartridge type is stored at header byte 0x0147.
    fun create(rom: UByteArray): Cartridge {
        val type = if (rom.size > 0x0147) rom[0x0147].toInt() else 0x00
        return when (type) {
            0x00 -> RomOnly(rom)
            0x01, 0x02, 0x03 -> Mbc1(rom)
            0x0F, 0x10, 0x11, 0x12, 0x13 -> Mbc3(rom)
            0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E -> Mbc5(rom)
            else -> {
                println("Unsupported cartridge type 0x${type.toString(16).uppercase()}, falling back to ROM Only")
                RomOnly(rom)
            }
        }
    }
}