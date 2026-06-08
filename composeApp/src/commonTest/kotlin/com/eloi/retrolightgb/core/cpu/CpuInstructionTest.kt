package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.flagC
import com.eloi.retrolightgb.flagH
import com.eloi.retrolightgb.flagN
import com.eloi.retrolightgb.flagZ
import com.eloi.retrolightgb.testCpu
import com.eloi.retrolightgb.testMemory
import com.eloi.retrolightgb.write
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CpuInstructionTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun setup() = testMemory().let { mem -> mem to testCpu(mem) }

    private fun cycles(cpu: Cpu) = cpu.t - cpu.lastT

    // ── NOP ──────────────────────────────────────────────────────────────────

    @Test
    fun nop_advancesPcBy1AndTakes4Cycles() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x00)
        cpu.step()
        assertEquals(0xC101, cpu.pc.toInt())
        assertEquals(4, cycles(cpu))
    }

    // ── Load instructions ─────────────────────────────────────────────────────

    @Test
    fun ldA_n8_loadsImmediateValue() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x3E, 0x42) // LD A, 0x42
        cpu.step()
        assertEquals(0x42, cpu.a.toInt())
        assertEquals(0xC102, cpu.pc.toInt())
        assertEquals(8, cycles(cpu))
    }

    @Test
    fun ldBC_n16_loadsImmediatePair() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x01, 0x34, 0x12) // LD BC, 0x1234 (little-endian)
        cpu.step()
        assertEquals(0x12, cpu.b.toInt())
        assertEquals(0x34, cpu.c.toInt())
        assertEquals(0xC103, cpu.pc.toInt())
    }

    @Test
    fun ldMem_A_writesToAbsoluteAddress() {
        val (mem, cpu) = setup()
        cpu.a = 0xBBu
        mem.write(0xC100, 0xEA, 0x00, 0xC2) // LD (0xC200), A
        cpu.step()
        assertEquals(0xBB, mem.readByte(0xC200u).toInt())
        assertEquals(0xC103, cpu.pc.toInt())
    }

    @Test
    fun ldA_mem_readsFromAbsoluteAddress() {
        val (mem, cpu) = setup()
        mem.writeByte(0xC200u, 0x77u)
        mem.write(0xC100, 0xFA, 0x00, 0xC2) // LD A, (0xC200)
        cpu.step()
        assertEquals(0x77, cpu.a.toInt())
    }

    @Test
    fun pushBC_popBC_roundTrip() {
        val (mem, cpu) = setup()
        cpu.b = 0x12u; cpu.c = 0x34u
        mem.write(0xC100, 0xC5) // PUSH BC
        cpu.step()
        assertEquals(0xDFFE, cpu.sp.toInt())

        // Corrupt B/C, then POP
        cpu.b = 0u; cpu.c = 0u
        mem.write(0xC101, 0xC1) // POP BC
        cpu.step()
        assertEquals(0x12, cpu.b.toInt())
        assertEquals(0x34, cpu.c.toInt())
        assertEquals(0xE000, cpu.sp.toInt())
    }

    // ── Arithmetic & flags ───────────────────────────────────────────────────

    @Test
    fun addA_n8_simpleResult() {
        val (mem, cpu) = setup()
        cpu.a = 0x10u
        mem.write(0xC100, 0xC6, 0x20) // ADD A, 0x20
        cpu.step()
        assertEquals(0x30, cpu.a.toInt())
        assertFalse(cpu.flagZ())
        assertFalse(cpu.flagN())
        assertFalse(cpu.flagH())
        assertFalse(cpu.flagC())
    }

    @Test
    fun addA_n8_setsHalfCarryAndCarryOnOverflow() {
        val (mem, cpu) = setup()
        cpu.a = 0xFFu
        mem.write(0xC100, 0xC6, 0x01) // ADD A, 0x01 → wraps to 0x00
        cpu.step()
        assertEquals(0x00, cpu.a.toInt())
        assertTrue(cpu.flagZ())
        assertFalse(cpu.flagN())
        assertTrue(cpu.flagH())
        assertTrue(cpu.flagC())
    }

    @Test
    fun subA_n8_simpleResult() {
        val (mem, cpu) = setup()
        cpu.a = 0x15u
        mem.write(0xC100, 0xD6, 0x05) // SUB 0x05
        cpu.step()
        assertEquals(0x10, cpu.a.toInt())
        assertFalse(cpu.flagZ())
        assertTrue(cpu.flagN())
        assertFalse(cpu.flagH())
        assertFalse(cpu.flagC())
    }

    @Test
    fun subA_n8_zeroResultSetsZFlag() {
        val (mem, cpu) = setup()
        cpu.a = 0x05u
        mem.write(0xC100, 0xD6, 0x05) // SUB 0x05 → 0
        cpu.step()
        assertEquals(0x00, cpu.a.toInt())
        assertTrue(cpu.flagZ())
        assertTrue(cpu.flagN())
        assertFalse(cpu.flagH())
        assertFalse(cpu.flagC())
    }

    @Test
    fun subA_n8_borrowSetsCarryAndHalf() {
        val (mem, cpu) = setup()
        cpu.a = 0x00u
        mem.write(0xC100, 0xD6, 0x01) // SUB 0x01 → 0xFF
        cpu.step()
        assertEquals(0xFF, cpu.a.toInt())
        assertFalse(cpu.flagZ())
        assertTrue(cpu.flagN())
        assertTrue(cpu.flagH())
        assertTrue(cpu.flagC())
    }

    @Test
    fun incA_setsHalfCarryOnNibbleRollover() {
        val (mem, cpu) = setup()
        cpu.a = 0x0Fu
        mem.write(0xC100, 0x3C) // INC A → 0x10
        cpu.step()
        assertEquals(0x10, cpu.a.toInt())
        assertFalse(cpu.flagZ())
        assertFalse(cpu.flagN())
        assertTrue(cpu.flagH())
    }

    @Test
    fun incA_setsZeroFlagOnWrap() {
        val (mem, cpu) = setup()
        cpu.a = 0xFFu
        mem.write(0xC100, 0x3C) // INC A → 0x00
        cpu.step()
        assertEquals(0x00, cpu.a.toInt())
        assertTrue(cpu.flagZ())
        assertTrue(cpu.flagH())
        assertFalse(cpu.flagN())
    }

    @Test
    fun decA_setsNFlagAndHalfCarry() {
        val (mem, cpu) = setup()
        cpu.a = 0x10u
        mem.write(0xC100, 0x3D) // DEC A → 0x0F
        cpu.step()
        assertEquals(0x0F, cpu.a.toInt())
        assertFalse(cpu.flagZ())
        assertTrue(cpu.flagN())
        assertTrue(cpu.flagH()) // lower nibble of original value was 0
    }

    @Test
    fun decA_setsZeroFlag() {
        val (mem, cpu) = setup()
        cpu.a = 0x01u
        mem.write(0xC100, 0x3D) // DEC A → 0x00
        cpu.step()
        assertEquals(0x00, cpu.a.toInt())
        assertTrue(cpu.flagZ())
        assertTrue(cpu.flagN())
        assertFalse(cpu.flagH())
    }

    @Test
    fun andA_n8_setsHFlagAlways() {
        val (mem, cpu) = setup()
        cpu.a = 0xFFu
        mem.write(0xC100, 0xE6, 0x0F) // AND 0x0F → 0x0F
        cpu.step()
        assertEquals(0x0F, cpu.a.toInt())
        assertFalse(cpu.flagZ())
        assertFalse(cpu.flagN())
        assertTrue(cpu.flagH())
        assertFalse(cpu.flagC())
    }

    @Test
    fun orA_n8_zeroInputSetsZFlag() {
        val (mem, cpu) = setup()
        cpu.a = 0x00u
        mem.write(0xC100, 0xF6, 0x00) // OR 0x00 → 0x00
        cpu.step()
        assertEquals(0x00, cpu.a.toInt())
        assertTrue(cpu.flagZ())
        assertFalse(cpu.flagN())
        assertFalse(cpu.flagH())
        assertFalse(cpu.flagC())
    }

    @Test
    fun xorA_n8_setsZOnSameByte() {
        val (mem, cpu) = setup()
        cpu.a = 0x55u
        mem.write(0xC100, 0xEE, 0x55) // XOR 0x55 → 0x00
        cpu.step()
        assertEquals(0x00, cpu.a.toInt())
        assertTrue(cpu.flagZ())
        assertFalse(cpu.flagN())
        assertFalse(cpu.flagH())
        assertFalse(cpu.flagC())
    }

    @Test
    fun cpA_n8_equalSetsZAndN() {
        val (mem, cpu) = setup()
        cpu.a = 0x05u
        mem.write(0xC100, 0xFE, 0x05) // CP 0x05
        cpu.step()
        assertEquals(0x05, cpu.a.toInt(), "CP must not modify A")
        assertTrue(cpu.flagZ())
        assertTrue(cpu.flagN())
        assertFalse(cpu.flagC())
    }

    @Test
    fun cpA_n8_greaterValueSetsCarry() {
        val (mem, cpu) = setup()
        cpu.a = 0x01u
        mem.write(0xC100, 0xFE, 0x05) // CP 0x05 (A < operand → borrow)
        cpu.step()
        assertFalse(cpu.flagZ())
        assertTrue(cpu.flagN())
        assertTrue(cpu.flagC())
    }

    // ── Control flow ─────────────────────────────────────────────────────────

    @Test
    fun jp_n16_jumpsToDirect16BitAddress() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0xC3, 0x00, 0xD0) // JP 0xD000
        cpu.step()
        assertEquals(0xD000, cpu.pc.toInt())
        assertEquals(16, cycles(cpu))
    }

    @Test
    fun jr_taken_addsSignedOffsetToPc() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x18, 0x05) // JR +5 → PC = 0xC100 + 2 + 5 = 0xC107
        cpu.step()
        assertEquals(0xC107, cpu.pc.toInt())
        assertEquals(12, cycles(cpu))
    }

    @Test
    fun jrNz_notTaken_advancesByTwo_whenZFlagSet() {
        val (mem, cpu) = setup()
        cpu.setFlag(Cpu.FLAG_Z)
        mem.write(0xC100, 0x20, 0x10) // JR NZ, +0x10 (not taken because Z=1)
        cpu.step()
        assertEquals(0xC102, cpu.pc.toInt())
        assertEquals(8, cycles(cpu))
    }

    @Test
    fun callAndRet_returnsToCorrectAddress() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0xCD, 0x00, 0xD0) // CALL 0xD000
        mem.write(0xD000, 0xC9)              // RET

        cpu.step() // CALL
        assertEquals(0xD000, cpu.pc.toInt())
        assertEquals(0xDFFE, cpu.sp.toInt())

        cpu.step() // RET
        assertEquals(0xC103, cpu.pc.toInt(), "Should return to instruction after CALL")
        assertEquals(0xE000, cpu.sp.toInt())
    }

    @Test
    fun halt_setsHaltedFlag() {
        val (mem, cpu) = setup()
        mem.write(0xC100, 0x76) // HALT
        cpu.step()
        assertTrue(cpu.halted)
        assertEquals(0xC101, cpu.pc.toInt())
    }

    // ── CB-prefixed bit operations ────────────────────────────────────────────

    @Test
    fun cb_rlc_a_rotatesLeftAndSetsCarry() {
        val (mem, cpu) = setup()
        cpu.a = 0x85u // 1000_0101
        mem.write(0xC100, 0xCB, 0x07) // RLC A
        cpu.step()
        assertEquals(0x0B, cpu.a.toInt()) // 0000_1011 with bit7 rotated to bit0
        assertTrue(cpu.flagC())
        assertFalse(cpu.flagZ())
        assertEquals(0xC102, cpu.pc.toInt())
    }

    @Test
    fun cb_rrc_a_rotatesRightAndSetsCarry() {
        val (mem, cpu) = setup()
        cpu.a = 0x01u
        mem.write(0xC100, 0xCB, 0x0F) // RRC A
        cpu.step()
        assertEquals(0x80, cpu.a.toInt()) // bit0 rotated to bit7
        assertTrue(cpu.flagC())
        assertFalse(cpu.flagZ())
    }

    @Test
    fun cb_srl_a_shiftsRightAndLeavesZero() {
        val (mem, cpu) = setup()
        cpu.a = 0x01u
        mem.write(0xC100, 0xCB, 0x3F) // SRL A → 0x00
        cpu.step()
        assertEquals(0x00, cpu.a.toInt())
        assertTrue(cpu.flagZ())
        assertTrue(cpu.flagC()) // original bit0 = 1
    }

    @Test
    fun cb_bit_0_a_zeroWhenBitSet() {
        val (mem, cpu) = setup()
        cpu.a = 0x01u // bit 0 = 1
        mem.write(0xC100, 0xCB, 0x47) // BIT 0, A
        cpu.step()
        assertFalse(cpu.flagZ(), "Z should be clear when tested bit is set")
        assertTrue(cpu.flagH())
        assertFalse(cpu.flagN())
        assertEquals(0x01, cpu.a.toInt(), "BIT must not modify the register")
    }

    @Test
    fun cb_bit_0_a_setWhenBitClear() {
        val (mem, cpu) = setup()
        cpu.a = 0x02u // bit 0 = 0
        mem.write(0xC100, 0xCB, 0x47) // BIT 0, A
        cpu.step()
        assertTrue(cpu.flagZ(), "Z should be set when tested bit is 0")
        assertTrue(cpu.flagH())
    }

    @Test
    fun cb_set_0_a_setsBit0() {
        val (mem, cpu) = setup()
        cpu.a = 0x00u
        mem.write(0xC100, 0xCB, 0xC7) // SET 0, A
        cpu.step()
        assertEquals(0x01, cpu.a.toInt())
    }

    @Test
    fun cb_res_0_a_clearsBit0() {
        val (mem, cpu) = setup()
        cpu.a = 0xFFu
        mem.write(0xC100, 0xCB, 0x87) // RES 0, A → 0xFE
        cpu.step()
        assertEquals(0xFE, cpu.a.toInt())
    }
}