package com.eloi.retrolightgb.core.memory

import com.eloi.retrolightgb.testMemory
import com.eloi.retrolightgb.core.cpu.InterruptType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MemoryTest {

    @Test
    fun readWriteWorkRam() {
        val mem = testMemory()
        mem.writeByte(0xC000u, 0x42u)
        assertEquals(0x42, mem.readByte(0xC000u).toInt())
    }

    @Test
    fun readWriteVram() {
        val mem = testMemory()
        mem.writeByte(0x8000u, 0xABu)
        assertEquals(0xAB, mem.readByte(0x8000u).toInt())
    }

    @Test
    fun timerDiv_resetsOnWrite() {
        val mem = testMemory()
        // Tick 512 cycles — DIV = (512 shr 8) = 2
        mem.tickTimer(512)
        assertNotEquals(0, mem.readByte(0xFF04u).toInt(), "DIV should be non-zero after ticking")
        // Any write to 0xFF04 resets the counter
        mem.writeByte(0xFF04u, 0x00u)
        assertEquals(0, mem.readByte(0xFF04u).toInt(), "DIV should be 0 after write")
    }

    @Test
    fun timerTima_incrementsWhenEnabled() {
        val mem = testMemory()
        // TAC = 0x05: enabled (bit 2) + clock mode 1 (16 cycles per tick)
        mem.writeByte(0xFF07u, 0x05u)
        assertEquals(0, mem.readByte(0xFF05u).toInt())
        mem.tickTimer(16)
        assertEquals(1, mem.readByte(0xFF05u).toInt())
    }

    @Test
    fun timerTima_doesNotTickWhenDisabled() {
        val mem = testMemory()
        // TAC = 0x01: disabled (bit 2 = 0)
        mem.writeByte(0xFF07u, 0x01u)
        mem.tickTimer(1024)
        assertEquals(0, mem.readByte(0xFF05u).toInt())
    }

    @Test
    fun timerOverflow_requestsTimerInterrupt() {
        val mem = testMemory()
        mem.writeByte(0xFF05u, 0xFFu) // TIMA = 0xFF (one tick from overflow)
        mem.writeByte(0xFF07u, 0x05u) // TAC: enabled, 16 cycles per tick
        mem.tickTimer(16)
        val ifReg = mem.readByte(0xFF0Fu).toInt()
        assertTrue((ifReg and 0x04) != 0, "Timer interrupt bit (bit 2) should be set after overflow")
    }

    @Test
    fun timerOverflow_loadsTmaIntoTima() {
        val mem = testMemory()
        mem.writeByte(0xFF06u, 0x10u) // TMA = 0x10 (modulo)
        mem.writeByte(0xFF05u, 0xFFu)
        mem.writeByte(0xFF07u, 0x05u)
        mem.tickTimer(16)
        assertEquals(0x10, mem.readByte(0xFF05u).toInt(), "TIMA should reload from TMA on overflow")
    }

    @Test
    fun requestInterrupt_setsIfBit() {
        val mem = testMemory()
        // Upper 3 bits of IF are always 1; lower 5 should be 0 initially
        assertEquals(0xE0, mem.readByte(0xFF0Fu).toInt())

        mem.requestInterrupt(InterruptType.VBlank)
        assertTrue((mem.readByte(0xFF0Fu).toInt() and 0x01) != 0)

        mem.requestInterrupt(InterruptType.Timer)
        assertTrue((mem.readByte(0xFF0Fu).toInt() and 0x04) != 0)
    }

    @Test
    fun clearInterrupt_clearsOnlyTargetBit() {
        val mem = testMemory()
        mem.requestInterrupt(InterruptType.VBlank)
        mem.requestInterrupt(InterruptType.Timer)

        mem.clearInterruptRequest(InterruptType.VBlank)

        assertEquals(0, mem.readByte(0xFF0Fu).toInt() and 0x01, "VBlank bit should be cleared")
        assertNotEquals(0, mem.readByte(0xFF0Fu).toInt() and 0x04, "Timer bit should still be set")
    }

    @Test
    fun bootRomDisabled_via0xFF50() {
        val mem = testMemory()
        // Boot ROM is enabled: address 0x0000 returns the boot ROM first byte (0x31 = LD SP,nn)
        val bootFirstByte = mem.readByte(0x0000u).toInt()
        assertNotEquals(0xFF, bootFirstByte, "Boot ROM should be active initially")

        // Writing 0x01 to 0xFF50 disables boot ROM
        mem.writeByte(0xFF50u, 0x01u)

        // Without a cartridge, 0x0000 now returns 0xFF
        assertEquals(0xFF, mem.readByte(0x0000u).toInt(), "Should return 0xFF after boot ROM disabled")
    }
}