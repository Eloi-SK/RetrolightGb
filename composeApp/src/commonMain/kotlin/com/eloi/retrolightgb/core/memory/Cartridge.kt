package com.eloi.retrolightgb.core.memory

import okio.BufferedSink
import okio.BufferedSource

@OptIn(ExperimentalUnsignedTypes::class)
interface Cartridge {
    fun readByte(address: Int): UByte
    fun writeByte(address: Int, value: UByte)
    fun tick(cycles: Int) {}           // advance RTC; no-op for cartridges without one
    fun saveRam(): ByteArray? = null   // return RAM contents to persist; null = nothing to save
    fun loadRam(data: ByteArray) {}    // restore RAM from persisted data

    // Save-state hooks: capture/restore the full mutable controller state (bank
    // registers, on-cart RAM, RTC). The ROM itself is immutable and not stored.
    fun saveState(sink: BufferedSink) {}
    fun loadState(source: BufferedSource) {}
}
