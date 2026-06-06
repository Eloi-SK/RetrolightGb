package com.eloi.retrolightgb.core.memory

// MBC3: up to 2MB ROM (128 banks × 16KB), 32KB RAM (4 banks × 8KB), and Real Time Clock.
//
// RTC registers are selected via 0x4000–0x5FFF (values 0x08–0x0C).
// Latch mechanism: write 0x00 then 0x01 to 0x6000–0x7FFF to snapshot live RTC values.
// Games read the latched snapshot so time doesn't change mid-read.
//
// RTC is ticked from Memory via Cartridge.tick(cycles).
@OptIn(ExperimentalUnsignedTypes::class)
class Mbc3(private val rom: UByteArray) : Cartridge {
    private val ram = UByteArray(0x8000)
    private var romBank = 1
    private var ramBank = 0   // 0x00–0x03 = RAM bank; 0x08–0x0C = RTC register
    private var ramEnabled = false

    // Live RTC registers
    private var rtcS  = 0   // seconds  0–59
    private var rtcM  = 0   // minutes  0–59
    private var rtcH  = 0   // hours    0–23
    private var rtcDL = 0   // days low 0–255
    private var rtcDH = 0   // bit 0 = day bit 8, bit 6 = halt, bit 7 = carry

    // Latched snapshot — what the game actually reads after the latch sequence
    private var latchS  = 0; private var latchM  = 0; private var latchH  = 0
    private var latchDL = 0; private var latchDH = 0

    // Latch sequence state: game must write 0x00 then 0x01 to 0x6000–0x7FFF
    private var latchPrimed = false

    // RTC cycle accumulator — advance one second every CPU_CLOCK T-cycles
    private var rtcCycles = 0
    private val CYCLES_PER_SECOND = 4_194_304

    override fun tick(cycles: Int) {
        if (rtcDH and 0x40 != 0) return  // halted — clock stopped
        rtcCycles += cycles
        while (rtcCycles >= CYCLES_PER_SECOND) {
            rtcCycles -= CYCLES_PER_SECOND
            advanceSecond()
        }
    }

    private fun advanceSecond() {
        if (++rtcS < 60) return
        rtcS = 0
        if (++rtcM < 60) return
        rtcM = 0
        if (++rtcH < 24) return
        rtcH = 0
        // 9-bit day counter: bit 0 of DH is day bit 8
        val days = (((rtcDH and 0x01) shl 8) or rtcDL) + 1
        if (days < 512) {
            rtcDL = days and 0xFF
            rtcDH = (rtcDH and 0xFE) or ((days shr 8) and 0x01)
        } else {
            // Overflow: reset counter, keep halt bit, set carry bit
            rtcDL = 0
            rtcDH = (rtcDH and 0x40) or 0x80
        }
    }

    override fun readByte(address: Int): UByte = when (address) {
        in 0x0000..0x3FFF -> if (address < rom.size) rom[address] else 0xFFu
        in 0x4000..0x7FFF -> {
            val offset = romBank * 0x4000 + (address - 0x4000)
            if (offset < rom.size) rom[offset] else 0xFFu
        }
        in 0xA000..0xBFFF -> {
            if (!ramEnabled) 0xFFu
            else when (ramBank) {
                in 0x00..0x03 -> ram[ramBank * 0x2000 + (address - 0xA000)]
                0x08 -> latchS.toUByte()
                0x09 -> latchM.toUByte()
                0x0A -> latchH.toUByte()
                0x0B -> latchDL.toUByte()
                0x0C -> latchDH.toUByte()
                else -> 0xFFu
            }
        }
        else -> 0xFFu
    }

    override fun writeByte(address: Int, value: UByte) {
        val v = value.toInt()
        when (address) {
            in 0x0000..0x1FFF -> ramEnabled = v and 0x0F == 0x0A
            in 0x2000..0x3FFF -> romBank = (v and 0x7F).let { if (it == 0) 1 else it }
            in 0x4000..0x5FFF -> ramBank = v and 0x0F
            in 0x6000..0x7FFF -> {
                when {
                    v == 0x00 -> latchPrimed = true
                    v == 0x01 && latchPrimed -> {
                        latchS = rtcS; latchM = rtcM; latchH = rtcH
                        latchDL = rtcDL; latchDH = rtcDH
                        latchPrimed = false
                    }
                    else -> latchPrimed = false
                }
            }
            in 0xA000..0xBFFF -> {
                if (ramEnabled) when (ramBank) {
                    in 0x00..0x03 -> ram[ramBank * 0x2000 + (address - 0xA000)] = value
                    0x08 -> rtcS  = v and 0x3F
                    0x09 -> rtcM  = v and 0x3F
                    0x0A -> rtcH  = v and 0x1F
                    0x0B -> rtcDL = v and 0xFF
                    0x0C -> rtcDH = v and 0xC1  // writable: bit 0 (day bit 8), bit 6 (halt), bit 7 (carry)
                }
            }
        }
    }
}