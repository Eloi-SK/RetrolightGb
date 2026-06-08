package com.eloi.retrolightgb.core.memory

import com.eloi.retrolightgb.core.readBoolean
import com.eloi.retrolightgb.core.writeBoolean
import okio.BufferedSink
import okio.BufferedSource

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

    // Save format: [32KB RAM][S][M][H][DL][DH][8-byte unix timestamp little-endian]
    override fun saveRam(): ByteArray {
        val ramBytes = ByteArray(ram.size) { ram[it].toByte() }
        val ts = currentUnixSeconds()
        return ramBytes + byteArrayOf(
            rtcS.toByte(), rtcM.toByte(), rtcH.toByte(), rtcDL.toByte(), rtcDH.toByte(),
            (ts and 0xFF).toByte(), ((ts shr 8) and 0xFF).toByte(),
            ((ts shr 16) and 0xFF).toByte(), ((ts shr 24) and 0xFF).toByte(),
            ((ts shr 32) and 0xFF).toByte(), ((ts shr 40) and 0xFF).toByte(),
            ((ts shr 48) and 0xFF).toByte(), ((ts shr 56) and 0xFF).toByte()
        )
    }

    override fun loadRam(data: ByteArray) {
        val len = minOf(data.size, ram.size)
        for (i in 0 until len) ram[i] = data[i].toUByte()

        val rtcOffset = ram.size
        if (data.size < rtcOffset + 13) return

        rtcS  = data[rtcOffset].toInt() and 0xFF
        rtcM  = data[rtcOffset + 1].toInt() and 0xFF
        rtcH  = data[rtcOffset + 2].toInt() and 0xFF
        rtcDL = data[rtcOffset + 3].toInt() and 0xFF
        rtcDH = data[rtcOffset + 4].toInt() and 0xFF

        var savedTs = 0L
        for (i in 0 until 8) savedTs = savedTs or ((data[rtcOffset + 5 + i].toLong() and 0xFF) shl (i * 8))

        val elapsed = currentUnixSeconds() - savedTs
        if (elapsed > 0 && rtcDH and 0x40 == 0) advanceBySeconds(elapsed)
    }

    // Convert current RTC state → total seconds → add elapsed → convert back.
    // Handles arbitrarily large elapsed values without looping.
    private fun advanceBySeconds(elapsed: Long) {
        val currentDays = ((rtcDH and 0x01) shl 8) or rtcDL
        val total = elapsed + rtcS + rtcM * 60L + rtcH * 3600L + currentDays * 86400L
        rtcS = (total % 60).toInt()
        val totalMin  = total / 60;  rtcM = (totalMin % 60).toInt()
        val totalHour = totalMin / 60; rtcH = (totalHour % 24).toInt()
        val totalDays = totalHour / 24
        if (totalDays < 512L) {
            rtcDL = (totalDays and 0xFF).toInt()
            rtcDH = (rtcDH and 0xFE) or ((totalDays shr 8).toInt() and 0x01)
        } else {
            val wrapped = totalDays % 512L
            rtcDL = (wrapped and 0xFF).toInt()
            rtcDH = (rtcDH and 0x40) or 0x80 or ((wrapped shr 8).toInt() and 0x01)
        }
    }

    // Save-state captures the full live + latched RTC and the latch/cycle state,
    // so the clock resumes exactly. (saveRam/loadRam above is the battery format
    // that instead re-syncs against wall-clock time.)
    override fun saveState(sink: BufferedSink) {
        sink.write(ram.toByteArray())
        sink.writeInt(romBank); sink.writeInt(ramBank)
        sink.writeBoolean(ramEnabled)
        sink.writeInt(rtcS); sink.writeInt(rtcM); sink.writeInt(rtcH); sink.writeInt(rtcDL); sink.writeInt(rtcDH)
        sink.writeInt(latchS); sink.writeInt(latchM); sink.writeInt(latchH); sink.writeInt(latchDL); sink.writeInt(latchDH)
        sink.writeBoolean(latchPrimed)
        sink.writeInt(rtcCycles)
    }

    override fun loadState(source: BufferedSource) {
        val bytes = source.readByteArray(ram.size.toLong())
        for (i in bytes.indices) ram[i] = bytes[i].toUByte()
        romBank = source.readInt(); ramBank = source.readInt()
        ramEnabled = source.readBoolean()
        rtcS = source.readInt(); rtcM = source.readInt(); rtcH = source.readInt(); rtcDL = source.readInt(); rtcDH = source.readInt()
        latchS = source.readInt(); latchM = source.readInt(); latchH = source.readInt(); latchDL = source.readInt(); latchDH = source.readInt()
        latchPrimed = source.readBoolean()
        rtcCycles = source.readInt()
    }

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