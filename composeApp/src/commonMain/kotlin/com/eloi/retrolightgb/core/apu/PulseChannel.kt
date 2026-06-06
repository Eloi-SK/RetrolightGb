package com.eloi.retrolightgb.core.apu

// Channels 1 (with sweep) and 2 (no sweep).
class PulseChannel(private val hasSweep: Boolean) {
    private var dacEnabled = false
    private var channelEnabled = false

    private var lengthCounter = 64
    private var lengthEnable = false

    private var initialVolume = 0
    private var addMode = false
    private var envelopePeriod = 0
    private var envelopeTimer = 0
    private var currentVolume = 0

    private var frequencyLow = 0
    private var frequencyHigh = 0
    private val frequency get() = (frequencyHigh shl 8) or frequencyLow
    private var frequencyTimer = 0
    private var dutyPattern = 2  // 50% default
    private var dutyStep = 0

    // Sweep — CH1 only
    private var sweepPeriod = 0
    private var sweepNegate = false
    private var sweepShift = 0
    private var sweepTimer = 0
    private var sweepEnabled = false
    private var shadowFrequency = 0

    companion object {
        // Each row: 8 steps of duty cycle (false = low, true = high)
        private val DUTY = arrayOf(
            booleanArrayOf(false, false, false, false, false, false, false, true),  // 12.5%
            booleanArrayOf(true,  false, false, false, false, false, false, true),  // 25%
            booleanArrayOf(true,  false, false, false, true,  true,  true,  true),  // 50%
            booleanArrayOf(false, true,  true,  true,  true,  true,  true,  false)  // 75%
        )
    }

    fun isEnabled() = channelEnabled

    fun getSample(): Int {
        if (!dacEnabled || !channelEnabled) return 0
        return if (DUTY[dutyPattern][dutyStep]) currentVolume else 0
    }

    fun tickTimer(cycles: Int) {
        frequencyTimer -= cycles
        while (frequencyTimer <= 0) {
            frequencyTimer += (2048 - frequency) * 4
            dutyStep = (dutyStep + 1) and 7
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

    fun clockSweep() {
        if (!hasSweep) return
        if (sweepTimer > 0) sweepTimer--
        if (sweepTimer == 0) {
            sweepTimer = if (sweepPeriod == 0) 8 else sweepPeriod
            if (sweepEnabled && sweepPeriod != 0) {
                val newFreq = computeSweepFrequency()
                if (newFreq < 2048 && sweepShift != 0) {
                    frequencyLow = newFreq and 0xFF
                    frequencyHigh = (newFreq shr 8) and 7
                    shadowFrequency = newFreq
                    computeSweepFrequency()  // overflow check only
                }
            }
        }
    }

    private fun computeSweepFrequency(): Int {
        val delta = shadowFrequency shr sweepShift
        val newFreq = if (sweepNegate) shadowFrequency - delta else shadowFrequency + delta
        if (newFreq >= 2048) channelEnabled = false
        return newFreq
    }

    private fun trigger() {
        channelEnabled = dacEnabled
        if (lengthCounter == 0) lengthCounter = 64
        frequencyTimer = (2048 - frequency) * 4
        envelopeTimer = envelopePeriod
        currentVolume = initialVolume
        if (hasSweep) {
            shadowFrequency = frequency
            sweepTimer = if (sweepPeriod == 0) 8 else sweepPeriod
            sweepEnabled = sweepPeriod != 0 || sweepShift != 0
            if (sweepShift != 0) computeSweepFrequency()
        }
    }

    fun reset() {
        dacEnabled = false; channelEnabled = false
        lengthCounter = 64; lengthEnable = false
        initialVolume = 0; addMode = false; envelopePeriod = 0; envelopeTimer = 0; currentVolume = 0
        frequencyLow = 0; frequencyHigh = 0; frequencyTimer = 0
        dutyPattern = 2; dutyStep = 0
        sweepPeriod = 0; sweepNegate = false; sweepShift = 0
        sweepTimer = 0; sweepEnabled = false; shadowFrequency = 0
    }

    fun writeLengthOnly(len: Int) { lengthCounter = 64 - (len and 0x3F) }

    // NR10 — sweep (CH1 only); bit 7 unused, reads as 1
    fun readNR10(): UByte = (0x80 or (sweepPeriod shl 4) or (if (sweepNegate) 0x08 else 0) or sweepShift).toUByte()
    fun writeNR10(v: Int) { sweepPeriod = (v shr 4) and 7; sweepNegate = (v and 8) != 0; sweepShift = v and 7 }

    // NR11/NR21 — duty (bits 7:6 readable) + length (bits 5:0 write-only)
    fun readNR11(): UByte = ((dutyPattern shl 6) or 0x3F).toUByte()
    fun writeNR11(v: Int) { dutyPattern = (v shr 6) and 3; lengthCounter = 64 - (v and 0x3F) }

    // NR12/NR22 — volume envelope (fully readable); bits 7:3 == 0 disables DAC
    fun readNR12(): UByte = ((initialVolume shl 4) or (if (addMode) 8 else 0) or envelopePeriod).toUByte()
    fun writeNR12(v: Int) {
        initialVolume = (v shr 4) and 0xF; addMode = (v and 8) != 0; envelopePeriod = v and 7
        dacEnabled = (v and 0xF8) != 0
        if (!dacEnabled) channelEnabled = false
    }

    // NR13/NR23 — frequency low (write-only)
    fun writeNR13(v: Int) { frequencyLow = v and 0xFF }

    // NR14/NR24 — trigger (bit 7), length enable (bit 6 readable), freq high (bits 2:0)
    fun readNR14(): UByte = ((if (lengthEnable) 0x40 else 0) or 0xBF).toUByte()
    fun writeNR14(v: Int) {
        frequencyHigh = v and 7; lengthEnable = (v and 0x40) != 0
        if ((v and 0x80) != 0) trigger()
    }
}