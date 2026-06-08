package com.eloi.retrolightgb

import com.eloi.retrolightgb.core.apu.Apu
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import okio.Buffer

// Full-machine snapshot (CPU + Memory/MBC + PPU + APU), distinct from the
// battery SRAM save. The immutable ROM is not stored — a state can only be
// restored onto the same game (validated by the cartridge's internal title).
//
// Format: ["RLGB"][version:1][titleLen:4][title][cpu][memory][ppu][apu]
object SaveState {
    private const val MAGIC = 0x524C4742  // "RLGB"
    private const val VERSION = 1

    // Build the persistence key for a save-state slot of a given ROM title.
    fun slotKey(title: String, slot: Int): String = "$title.ss$slot"

    fun capture(title: String, cpu: Cpu, ppu: Ppu, memory: Memory, apu: Apu): ByteArray {
        val buffer = Buffer()
        buffer.writeInt(MAGIC)
        buffer.writeByte(VERSION)
        val titleBytes = title.encodeToByteArray()
        buffer.writeInt(titleBytes.size)
        buffer.write(titleBytes)
        cpu.saveState(buffer)
        memory.saveState(buffer)
        ppu.saveState(buffer)
        apu.saveState(buffer)
        return buffer.readByteArray()
    }

    // Returns true if the blob was valid for this ROM and was applied.
    fun restore(blob: ByteArray, title: String, cpu: Cpu, ppu: Ppu, memory: Memory, apu: Apu): Boolean {
        val buffer = Buffer().apply { write(blob) }
        if (buffer.readInt() != MAGIC) return false
        if (buffer.readByte().toInt() != VERSION) return false
        val titleLen = buffer.readInt()
        val savedTitle = buffer.readByteArray(titleLen.toLong()).decodeToString()
        if (savedTitle != title) return false
        cpu.loadState(buffer)
        memory.loadState(buffer)
        ppu.loadState(buffer)
        apu.loadState(buffer)
        return true
    }
}