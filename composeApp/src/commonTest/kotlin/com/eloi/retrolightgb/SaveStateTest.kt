@file:OptIn(ExperimentalUnsignedTypes::class)

package com.eloi.retrolightgb

import com.eloi.retrolightgb.core.apu.NoiseChannel
import com.eloi.retrolightgb.core.apu.PulseChannel
import com.eloi.retrolightgb.core.apu.WaveChannel
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveStateTest {

    @Test
    fun cpuRegisters_roundTrip() {
        val mem = testMemory()
        val cpu = testCpu(mem)
        cpu.a = 0x12u; cpu.b = 0x34u; cpu.pc = 0xABCDu; cpu.sp = 0x1234u
        cpu.t = 999; cpu.imeEnabled = true; cpu.halted = true

        val buffer = Buffer()
        cpu.saveState(buffer)

        val restored = testCpu(testMemory())
        restored.loadState(buffer)

        assertEquals(0x12, restored.a.toInt())
        assertEquals(0x34, restored.b.toInt())
        assertEquals(0xABCD, restored.pc.toInt())
        assertEquals(0x1234, restored.sp.toInt())
        assertEquals(999, restored.t)
        assertTrue(restored.imeEnabled)
        assertTrue(restored.halted)
    }

    @Test
    fun memory_roundTrip_includesWorkRamCartRamAndTimer() {
        val mem = testMemory()
        mem.load(rom = mbc1Rom())
        // Work RAM
        mem.writeByte(0xC000u, 0xAAu)
        mem.writeByte(0xDFFFu, 0xBBu)
        // Enable + write cartridge RAM
        mem.writeByte(0x0000u, 0x0Au)  // enable RAM
        mem.writeByte(0xA000u, 0x77u)
        // Advance the timer so DIV is non-zero
        mem.writeByte(0xFF07u, 0x05u)  // TAC: enable, freq 16
        mem.tickTimer(5000)
        val div = mem.readByte(0xFF04u).toInt()

        val buffer = Buffer()
        mem.saveState(buffer)

        val restored = testMemory()
        restored.load(rom = mbc1Rom())
        restored.loadState(buffer)

        assertEquals(0xAA, restored.readByte(0xC000u).toInt())
        assertEquals(0xBB, restored.readByte(0xDFFFu).toInt())
        assertEquals(0x77, restored.readByte(0xA000u).toInt(), "cartridge RAM should round-trip")
        assertEquals(div, restored.readByte(0xFF04u).toInt(), "DIV should round-trip")
    }

    @Test
    fun ppu_roundTrip_preservesFrameBuffer() {
        val mem = testMemory()
        val ppu = Ppu(mem)
        // Enable LCD and put a tile + map entry in VRAM so something renders.
        mem.writeByte(0xFF40u, 0x91u)  // LCDC: LCD on, BG on, tiledata 0x8000
        mem.writeByte(0xFF47u, 0xE4u)  // BGP palette
        mem.writeByte(0x8000u, 0xFFu)  // tile 0 row 0
        mem.writeByte(0x8001u, 0xFFu)
        // Tick through a couple of frames so scanlines render.
        repeat(80_000) { ppu.tick(4) }
        val before = ppu.frameBuffer.copyOf()

        val buffer = Buffer()
        ppu.saveState(buffer)

        val restored = Ppu(mem)
        restored.loadState(buffer)

        assertTrue(before.contentEquals(restored.frameBuffer), "frame buffer should round-trip")
    }

    @Test
    fun pulseChannel_roundTrip() {
        val ch = PulseChannel(hasSweep = true)
        ch.writeNR10(0x35); ch.writeNR11(0x80); ch.writeNR12(0xF3); ch.writeNR13(0x55); ch.writeNR14(0x87)
        ch.tickTimer(100); ch.clockEnvelope(); ch.clockSweep()
        val sampleBefore = ch.getSample()
        val enabledBefore = ch.isEnabled()

        val buffer = Buffer()
        ch.saveState(buffer)

        val restored = PulseChannel(hasSweep = true)
        restored.loadState(buffer)

        assertEquals(enabledBefore, restored.isEnabled())
        assertEquals(sampleBefore, restored.getSample())
        // Both must continue identically after the same input.
        ch.tickTimer(64); restored.tickTimer(64)
        assertEquals(ch.getSample(), restored.getSample())
    }

    @Test
    fun waveChannel_roundTrip() {
        val waveRam = UByteArray(16) { (it * 0x11).toUByte() }
        val ch = WaveChannel(waveRam)
        ch.writeNR30(0x80); ch.writeNR32(0x40); ch.writeNR33(0x33); ch.writeNR34(0x87)
        ch.tickTimer(200)
        val sampleBefore = ch.getSample()

        val buffer = Buffer()
        ch.saveState(buffer)

        val restored = WaveChannel(waveRam)
        restored.loadState(buffer)

        assertEquals(ch.isEnabled(), restored.isEnabled())
        assertEquals(sampleBefore, restored.getSample())
        ch.tickTimer(64); restored.tickTimer(64)
        assertEquals(ch.getSample(), restored.getSample())
    }

    @Test
    fun noiseChannel_roundTrip() {
        val ch = NoiseChannel()
        ch.writeNR42(0xF3); ch.writeNR43(0x55); ch.writeNR44(0x80)
        ch.tickTimer(300); ch.clockEnvelope()
        val sampleBefore = ch.getSample()

        val buffer = Buffer()
        ch.saveState(buffer)

        val restored = NoiseChannel()
        restored.loadState(buffer)

        assertEquals(ch.isEnabled(), restored.isEnabled())
        assertEquals(sampleBefore, restored.getSample())
        ch.tickTimer(128); restored.tickTimer(128)
        assertEquals(ch.getSample(), restored.getSample())
    }

    // MBC1 ROM with a recognizable title and 3 ROM banks.
    private fun mbc1Rom(): ByteArray {
        val rom = ByteArray(3 * 0x4000)
        "TESTGAME".forEachIndexed { i, ch -> rom[0x134 + i] = ch.code.toByte() }
        rom[0x147] = 0x01  // MBC1
        return rom
    }
}