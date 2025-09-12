package com.eloi.retrolightgb.core.cpu

enum class InterruptType(val bit: Int, val address: UShort) {
    VBlank(bit = 0, address = 0x0040u),
    LcdStat(bit = 1, address = 0x0048u),
    Timer(bit = 2, address = 0x0050u),
    Serial(bit = 3, address = 0x0058u),
    Joypad(bit = 4, address = 0x0060u);

    val mask: UByte
        get() = (1 shl bit).toUByte()

    companion object {
        fun fromBit(bit: Int): InterruptType? {
            return entries.find { it.bit == bit }
        }

        fun getAllTypes(): List<InterruptType> = entries.toList()
    }
}