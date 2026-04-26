package com.eloi.retrolightgb.core.memory

@OptIn(ExperimentalUnsignedTypes::class)
interface Cartridge {
    fun readByte(address: Int): UByte
    fun writeByte(address: Int, value: UByte)
}
