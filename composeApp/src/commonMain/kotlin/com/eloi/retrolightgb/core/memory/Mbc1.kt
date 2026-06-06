package com.eloi.retrolightgb.core.memory

// MBC1: up to 2MB ROM (128 banks × 16KB) and 32KB RAM (4 banks × 8KB).
// Writes to 0x0000–0x7FFF configure bank registers instead of modifying ROM.
@OptIn(ExperimentalUnsignedTypes::class)
class Mbc1(private val rom: UByteArray) : Cartridge {
    private val ram = UByteArray(0x8000)
    private var romBankLow = 1   // bits 4:0 — register at 0x2000–0x3FFF
    private var bankHighOrRamBank = 0  // bits 1:0 — register at 0x4000–0x5FFF
    private var bankingMode = 0  // 0 = ROM banking, 1 = RAM banking
    private var ramEnabled = false

    // Banks 0x00, 0x20, 0x40, 0x60 are remapped to the next bank by hardware.
    private val effectiveRomBank: Int
        get() {
            val low = if (romBankLow == 0) 1 else romBankLow
            return if (bankingMode == 0) (bankHighOrRamBank shl 5) or low else low
        }

    private val effectiveRamBank: Int
        get() = if (bankingMode == 1) bankHighOrRamBank else 0

    override fun readByte(address: Int): UByte = when (address) {
        in 0x0000..0x3FFF -> if (address < rom.size) rom[address] else 0xFFu
        in 0x4000..0x7FFF -> {
            val offset = effectiveRomBank * 0x4000 + (address - 0x4000)
            if (offset < rom.size) rom[offset] else 0xFFu
        }
        in 0xA000..0xBFFF -> {
            if (ramEnabled) ram[effectiveRamBank * 0x2000 + (address - 0xA000)] else 0xFFu
        }
        else -> 0xFFu
    }

    override fun saveRam(): ByteArray = ByteArray(ram.size) { ram[it].toByte() }
    override fun loadRam(data: ByteArray) {
        val len = minOf(data.size, ram.size)
        for (i in 0 until len) ram[i] = data[i].toUByte()
    }

    override fun writeByte(address: Int, value: UByte) {
        when (address) {
            in 0x0000..0x1FFF -> ramEnabled = value.toInt() and 0x0F == 0x0A
            in 0x2000..0x3FFF -> romBankLow = value.toInt() and 0x1F
            in 0x4000..0x5FFF -> bankHighOrRamBank = value.toInt() and 0x03
            in 0x6000..0x7FFF -> bankingMode = value.toInt() and 0x01
            in 0xA000..0xBFFF -> {
                if (ramEnabled) ram[effectiveRamBank * 0x2000 + (address - 0xA000)] = value
            }
        }
    }
}
