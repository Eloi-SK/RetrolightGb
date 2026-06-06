package com.eloi.retrolightgb.core.apu

// Channel 4 — LFSR-based pseudo-random noise.
class NoiseChannel {
    private var dacEnabled = false
    private var channelEnabled = false

    private var lengthCounter = 64
    private var lengthEnable = false

    private var initialVolume = 0
    private var addMode = false
    private var envelopePeriod = 0
    private var envelopeTimer = 0
    private var currentVolume = 0

    private var clockShift = 0    // NR43 bits 7:4
    private var lfsrWidth = false  // NR43 bit 3: true = 7-bit LFSR
    private var divisorCode = 0   // NR43 bits 2:0
    private var lfsr = 0x7FFF
    private var frequencyTimer = 0

    companion object {
        private val DIVISORS = intArrayOf(8, 16, 32, 48, 64, 80, 96, 112)
    }

    fun isEnabled() = channelEnabled

    // Output is the inverted low bit of the LFSR: 0 → quiet, 1 → amplitude.
    fun getSample(): Int {
        if (!dacEnabled || !channelEnabled) return 0
        return if ((lfsr and 1) == 0) currentVolume else 0
    }

    fun tickTimer(cycles: Int) {
        frequencyTimer -= cycles
        while (frequencyTimer <= 0) {
            frequencyTimer += DIVISORS[divisorCode] shl clockShift
            val xorBit = (lfsr and 1) xor ((lfsr shr 1) and 1)
            lfsr = ((lfsr shr 1) and 0x3FFF) or (xorBit shl 14)
            if (lfsrWidth) lfsr = (lfsr and 0x7FBF) or (xorBit shl 6)  // 7-bit: also write bit 6
        }
    }

    fun clockLength() {
        if (lengthEnable && lengthCounter > 0 && --lengthCounter == 0) channelEnabled = false
    }

    fun clockEnvelope() {
        if (envelopePeriod == 0) return
        if (--envelopeTimer <= 0) {
            envelopeTimer = envelopePeriod
            if (addMode && currentVolume < 15) currentVolume++
            else if (!addMode && currentVolume > 0) currentVolume--
        }
    }

    private fun trigger() {
        channelEnabled = dacEnabled
        if (lengthCounter == 0) lengthCounter = 64
        envelopeTimer = envelopePeriod
        currentVolume = initialVolume
        lfsr = 0x7FFF
        frequencyTimer = DIVISORS[divisorCode] shl clockShift
    }

    fun reset() {
        dacEnabled = false; channelEnabled = false
        lengthCounter = 64; lengthEnable = false
        initialVolume = 0; addMode = false; envelopePeriod = 0; envelopeTimer = 0; currentVolume = 0
        clockShift = 0; lfsrWidth = false; divisorCode = 0; lfsr = 0x7FFF; frequencyTimer = 0
    }

    fun writeLengthOnly(len: Int) { lengthCounter = 64 - (len and 0x3F) }

    // NR41 — length (write-only)
    fun writeNR41(v: Int) { lengthCounter = 64 - (v and 0x3F) }

    // NR42 — volume envelope (fully readable); bits 7:3 == 0 disables DAC
    fun readNR42(): UByte = ((initialVolume shl 4) or (if (addMode) 8 else 0) or envelopePeriod).toUByte()
    fun writeNR42(v: Int) {
        initialVolume = (v shr 4) and 0xF; addMode = (v and 8) != 0; envelopePeriod = v and 7
        dacEnabled = (v and 0xF8) != 0
        if (!dacEnabled) channelEnabled = false
    }

    // NR43 — clock/LFSR config (fully readable)
    fun readNR43(): UByte = ((clockShift shl 4) or (if (lfsrWidth) 8 else 0) or divisorCode).toUByte()
    fun writeNR43(v: Int) { clockShift = (v shr 4) and 0xF; lfsrWidth = (v and 8) != 0; divisorCode = v and 7 }

    // NR44 — trigger (bit 7), length enable (bit 6 readable)
    fun readNR44(): UByte = ((if (lengthEnable) 0x40 else 0) or 0xBF).toUByte()
    fun writeNR44(v: Int) {
        lengthEnable = (v and 0x40) != 0
        if ((v and 0x80) != 0) trigger()
    }
}