package com.eloi.retrolightgb.core.memory

@OptIn(ExperimentalUnsignedTypes::class)
interface Cartridge {
    fun readByte(address: Int): UByte
    fun writeByte(address: Int, value: UByte)
    fun tick(cycles: Int) {}           // advance RTC; no-op for cartridges without one
    fun saveRam(): ByteArray? = null   // return RAM contents to persist; null = nothing to save
    fun loadRam(data: ByteArray) {}    // restore RAM from persisted data
}
