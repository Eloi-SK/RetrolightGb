@file:OptIn(ExperimentalUnsignedTypes::class)

package com.eloi.retrolightgb.core.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MbcTest {

    // ── MBC1 ─────────────────────────────────────────────────────────────────

    @Test
    fun mbc1_bankSwitch_readsFromCorrectBank() {
        // 3 banks of 16KB = 48KB. Bank 2 starts at 0x8000.
        val rom = UByteArray(3 * 0x4000)
        rom[2 * 0x4000] = 0xABu // Marker in bank 2

        val mbc1 = Mbc1(rom)
        mbc1.writeByte(0x2000, 0x02u) // Select ROM bank 2
        assertEquals(0xAB, mbc1.readByte(0x4000).toInt())
    }

    @Test
    fun mbc1_ramDisabledByDefault_returnsFF() {
        val mbc1 = Mbc1(UByteArray(0x8000))
        assertEquals(0xFF, mbc1.readByte(0xA000).toInt())
    }

    @Test
    fun mbc1_ramEnabled_allowsReadWrite() {
        val mbc1 = Mbc1(UByteArray(0x8000))
        mbc1.writeByte(0x0000, 0x0Au) // Enable RAM
        mbc1.writeByte(0xA000, 0x55u)
        assertEquals(0x55, mbc1.readByte(0xA000).toInt())
    }

    @Test
    fun mbc1_ramDisabled_afterNonEnableValue() {
        val mbc1 = Mbc1(UByteArray(0x8000))
        mbc1.writeByte(0x0000, 0x0Au) // Enable
        mbc1.writeByte(0xA000, 0x55u)
        mbc1.writeByte(0x0000, 0x00u) // Disable
        assertEquals(0xFF, mbc1.readByte(0xA000).toInt())
    }

    @Test
    fun mbc1_saveRam_roundTripPreservesData() {
        val mbc1 = Mbc1(UByteArray(0x8000))
        mbc1.writeByte(0x0000, 0x0Au) // Enable RAM
        mbc1.writeByte(0xA000, 0x12u)
        mbc1.writeByte(0xA001, 0x34u)

        val saved = mbc1.saveRam()

        val mbc1b = Mbc1(UByteArray(0x8000))
        mbc1b.loadRam(saved)
        mbc1b.writeByte(0x0000, 0x0Au) // Enable RAM on the new instance
        assertEquals(0x12, mbc1b.readByte(0xA000).toInt())
        assertEquals(0x34, mbc1b.readByte(0xA001).toInt())
    }

    // ── MBC2 ─────────────────────────────────────────────────────────────────

    @Test
    fun mbc2_builtInRam_onlyLowerNibbleWritable() {
        val mbc2 = Mbc2(UByteArray(0x4000))
        // Enable RAM: address bit 8 = 0 (e.g. 0x0000), value = 0x0A
        mbc2.writeByte(0x0000, 0x0Au)
        mbc2.writeByte(0xA000, 0xABu) // only lower nibble 0xB should survive
        // MBC2 stores only the lower nibble, upper nibble reads back as 0xF
        val val1 = mbc2.readByte(0xA000).toInt()
        assertEquals(0xFB, val1, "MBC2 upper nibble should always read as 0xF")
    }

    @Test
    fun mbc2_ram_mirrors512Bytes() {
        val mbc2 = Mbc2(UByteArray(0x4000))
        mbc2.writeByte(0x0000, 0x0Au) // Enable
        mbc2.writeByte(0xA000, 0x05u)
        // 0xA200 mirrors 0xA000 (0xA200 and 0x01FF = 0x000)
        assertEquals(mbc2.readByte(0xA000).toInt(), mbc2.readByte(0xA200).toInt())
    }

    // ── MBC3 RTC ─────────────────────────────────────────────────────────────

    @Test
    fun mbc3_rtcTick_advancesSecondsAfterOneFullSecond() {
        val mbc3 = Mbc3(UByteArray(0x8000))
        mbc3.writeByte(0x0000, 0x0Au) // Enable RAM/RTC
        mbc3.writeByte(0x4000, 0x08u) // Select RTC seconds register

        // Initial latch: seconds = 0
        mbc3.writeByte(0x6000, 0x00u)
        mbc3.writeByte(0x6001, 0x01u)
        assertEquals(0, mbc3.readByte(0xA000).toInt())

        // Tick exactly one Game Boy second (4,194,304 T-cycles)
        mbc3.tick(4_194_304)

        // Latch and read again
        mbc3.writeByte(0x6000, 0x00u)
        mbc3.writeByte(0x6001, 0x01u)
        assertEquals(1, mbc3.readByte(0xA000).toInt())
    }

    @Test
    fun mbc3_rtcHalt_preventsClockAdvance() {
        val mbc3 = Mbc3(UByteArray(0x8000))
        mbc3.writeByte(0x0000, 0x0Au) // Enable
        // Set DH register (0x0C) with halt bit (bit 6 = 0x40)
        mbc3.writeByte(0x4000, 0x0Cu) // Select RTC DH register
        mbc3.writeByte(0xA000, 0x40u) // rtcDH = 0x40 → halt bit set

        mbc3.tick(4_194_304) // would advance 1 second if not halted

        // Select and latch seconds register
        mbc3.writeByte(0x4000, 0x08u)
        mbc3.writeByte(0x6000, 0x00u)
        mbc3.writeByte(0x6001, 0x01u)
        assertEquals(0, mbc3.readByte(0xA000).toInt(), "RTC should not advance when halted")
    }

    @Test
    fun mbc3_saveRam_roundTripPreservesData() {
        val mbc3 = Mbc3(UByteArray(0x8000))
        mbc3.writeByte(0x0000, 0x0Au)
        mbc3.writeByte(0x4000, 0x00u) // Select RAM bank 0
        mbc3.writeByte(0xA000, 0x77u)
        mbc3.writeByte(0xA001, 0x88u)

        val saved = mbc3.saveRam()

        val mbc3b = Mbc3(UByteArray(0x8000))
        mbc3b.loadRam(saved)
        mbc3b.writeByte(0x0000, 0x0Au)
        mbc3b.writeByte(0x4000, 0x00u)
        assertEquals(0x77, mbc3b.readByte(0xA000).toInt())
        assertEquals(0x88, mbc3b.readByte(0xA001).toInt())
    }

    // ── MBC5 ─────────────────────────────────────────────────────────────────

    @Test
    fun mbc5_bankSelection_readsCorrectBank() {
        val rom = UByteArray(3 * 0x4000)
        rom[0 * 0x4000 + 0x10] = 0x11u // Fixed bank 0
        rom[1 * 0x4000 + 0x00] = 0x22u // Bank 1
        rom[2 * 0x4000 + 0x00] = 0x33u // Bank 2

        val mbc5 = Mbc5(rom)
        // Bank 1 (default)
        assertEquals(0x22, mbc5.readByte(0x4000).toInt())

        // Switch to bank 2
        mbc5.writeByte(0x2000, 0x02u) // romBankLo = 2
        mbc5.writeByte(0x3000, 0x00u) // romBankHi = 0
        assertEquals(0x33, mbc5.readByte(0x4000).toInt())

        // Fixed bank 0 is always accessible
        assertEquals(0x11, mbc5.readByte(0x0010).toInt())
    }

    @Test
    fun mbc5_ramEnable_allowsReadWrite() {
        val mbc5 = Mbc5(UByteArray(0x8000))
        assertEquals(0xFF, mbc5.readByte(0xA000).toInt(), "RAM should return 0xFF when disabled")

        mbc5.writeByte(0x0000, 0x0Au) // Enable RAM
        mbc5.writeByte(0xA000, 0xCAu)
        assertEquals(0xCA, mbc5.readByte(0xA000).toInt())
    }

    @Test
    fun mbc5_saveRam_roundTripPreservesData() {
        val mbc5 = Mbc5(UByteArray(0x8000))
        mbc5.writeByte(0x0000, 0x0Au)
        mbc5.writeByte(0xA000, 0xAAu)
        mbc5.writeByte(0xA100, 0xBBu)

        val saved = mbc5.saveRam()

        val mbc5b = Mbc5(UByteArray(0x8000))
        mbc5b.loadRam(saved)
        mbc5b.writeByte(0x0000, 0x0Au)
        assertEquals(0xAA, mbc5b.readByte(0xA000).toInt())
        assertEquals(0xBB, mbc5b.readByte(0xA100).toInt())
    }
}