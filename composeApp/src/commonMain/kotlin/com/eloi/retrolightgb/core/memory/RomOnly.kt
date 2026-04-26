package com.eloi.retrolightgb.core.memory

@OptIn(ExperimentalUnsignedTypes::class)
class RomOnly(private val rom: UByteArray) : Cartridge {
    override fun readByte(address: Int): UByte = when (address) {
        in 0x0000..0x7FFF -> if (address < rom.size) rom[address] else 0xFFu
        else -> 0xFFu
    }

    override fun writeByte(address: Int, value: UByte) = Unit
}
