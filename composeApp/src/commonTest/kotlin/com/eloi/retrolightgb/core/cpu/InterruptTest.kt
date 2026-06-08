package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.testCpu
import com.eloi.retrolightgb.testMemory
import com.eloi.retrolightgb.write
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterruptTest {

    private fun setup() = testMemory().let { mem -> mem to testCpu(mem) }

    // ── VBlank interrupt ──────────────────────────────────────────────────────

    @Test
    fun vblankInterrupt_jumpsToVector0x0040() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x00) // NOP — will be followed by interrupt handling
        // Set IF bit 0 (VBlank) and IE bit 0
        mem.writeByte(0xFF0Fu, 0x01u)
        mem.writeByte(0xFFFFu, 0x01u)
        cpu.imeEnabled = true

        cpu.step() // executes NOP, then handles interrupt
        assertEquals(0x0040, cpu.pc.toInt(), "PC should jump to VBlank vector")
        assertFalse(cpu.imeEnabled, "IME should be cleared after dispatch")
    }

    // ── Timer interrupt ───────────────────────────────────────────────────────

    @Test
    fun timerInterrupt_jumpsToVector0x0050() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x00) // NOP
        mem.writeByte(0xFF0Fu, 0x04u) // IF: Timer bit (bit 2)
        mem.writeByte(0xFFFFu, 0x04u) // IE: Timer bit
        cpu.imeEnabled = true

        cpu.step()
        assertEquals(0x0050, cpu.pc.toInt(), "PC should jump to Timer vector")
    }

    // ── IME disabled ─────────────────────────────────────────────────────────

    @Test
    fun imeDisabled_interruptPendingButNotDispatched() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x00) // NOP
        mem.writeByte(0xFF0Fu, 0x01u) // IF: VBlank
        mem.writeByte(0xFFFFu, 0x01u) // IE: VBlank
        cpu.imeEnabled = false        // DI state

        cpu.step()
        assertEquals(0xC101, cpu.pc.toInt(), "PC should NOT jump when IME is disabled")
    }

    // ── EI delay ─────────────────────────────────────────────────────────────

    @Test
    fun ei_enablesImeOneInstructionAfterExecution() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0xFB) // EI
        mem.write(0xC101, 0x00) // NOP

        cpu.step() // execute EI
        assertFalse(cpu.imeEnabled, "IME should still be off immediately after EI")
        assertTrue(cpu.pendingIme, "Pending IME should be set after EI")

        cpu.step() // execute NOP — pendingIme resolves at START of this step
        assertTrue(cpu.imeEnabled, "IME should be enabled after the instruction following EI")
        assertFalse(cpu.pendingIme)
    }

    // ── HALT ─────────────────────────────────────────────────────────────────

    @Test
    fun halt_exitsWhenInterruptPending_evenWithoutIme() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x76) // HALT

        cpu.step() // execute HALT → cpu.halted = true
        assertTrue(cpu.halted)

        // Set an interrupt (with IE bit) but keep IME disabled
        mem.writeByte(0xFF0Fu, 0x01u)
        mem.writeByte(0xFFFFu, 0x01u)
        cpu.imeEnabled = false

        cpu.step() // halted step — should exit halt due to pending interrupt
        assertFalse(cpu.halted, "HALT should exit when IF & IE is non-zero, even without IME")
        assertEquals(0xC101, cpu.pc.toInt(), "PC should not jump because IME is off")
    }

    // ── Interrupt priority ───────────────────────────────────────────────────

    @Test
    fun multipleInterruptsPending_vblankHandledFirst() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x00) // NOP
        // Set both VBlank (bit 0) and Timer (bit 2) pending
        mem.writeByte(0xFF0Fu, 0x05u)
        mem.writeByte(0xFFFFu, 0x05u)
        cpu.imeEnabled = true

        cpu.step()
        // VBlank has lower bit index (0) → higher priority per getAllTypes() order
        assertEquals(0x0040, cpu.pc.toInt(), "VBlank should be handled first")
    }
}