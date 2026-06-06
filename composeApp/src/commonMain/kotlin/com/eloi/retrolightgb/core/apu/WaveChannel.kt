package com.eloi.retrolightgb.core.apu

// Channel 3 — PCM wave from 32 4-bit samples in Wave RAM (0xFF30-0xFF3F).
@OptIn(ExperimentalUnsignedTypes::class)
class WaveChannel(private val waveRam: UByteArray) {
    private var dacEnabled = false
    private var channelEnabled = false

    private var lengthCounter = 256
    private var lengthEnable = false
    private var outputLevel = 0   // 0=mute, 1=100%, 2=50%, 3=25%

    private var frequencyLow = 0
    private var frequencyHigh = 0
    private val frequency get() = (frequencyHigh shl 8) or frequencyLow
    private var frequencyTimer = 0
    private var wavePosition = 0  // 0-31 nibble index

    fun isEnabled() = channelEnabled

    fun getSample(): Int {
        if (!dacEnabled || !channelEnabled) return 0
        val byte = waveRam[wavePosition / 2].toInt()
        val nibble = if (wavePosition % 2 == 0) (byte shr 4) and 0x0F else byte and 0x0F
        return when (outputLevel) {
            1 -> nibble
            2 -> nibble shr 1
            3 -> nibble shr 2
            else -> 0
        }
    }

    fun tickTimer(cycles: Int) {
        frequencyTimer -= cycles
        while (frequencyTimer <= 0) {
            frequencyTimer += (2048 - frequency) * 2
            wavePosition = (wavePosition + 1) and 31
        }
    }

    fun clockLength() {
        if (lengthEnable && lengthCounter > 0 && --lengthCounter == 0) channelEnabled = false
    }

    private fun trigger() {
        channelEnabled = dacEnabled
        if (lengthCounter == 0) lengthCounter = 256
        frequencyTimer = (2048 - frequency) * 2
        wavePosition = 0
    }

    fun reset() {
        dacEnabled = false; channelEnabled = false
        lengthCounter = 256; lengthEnable = false; outputLevel = 0
        frequencyLow = 0; frequencyHigh = 0; frequencyTimer = 0; wavePosition = 0
    }

    fun writeLengthOnly(len: Int) { lengthCounter = 256 - (len and 0xFF) }

    // NR30 — DAC enable (bit 7 readable)
    fun readNR30(): UByte = (if (dacEnabled) 0xFF else 0x7F).toUByte()
    fun writeNR30(v: Int) { dacEnabled = (v and 0x80) != 0; if (!dacEnabled) channelEnabled = false }

    // NR31 — length (write-only)
    fun writeNR31(v: Int) { lengthCounter = 256 - (v and 0xFF) }

    // NR32 — output level (bits 6:5 readable)
    fun readNR32(): UByte = (0x9F or (outputLevel shl 5)).toUByte()
    fun writeNR32(v: Int) { outputLevel = (v shr 5) and 3 }

    // NR33 — frequency low (write-only)
    fun writeNR33(v: Int) { frequencyLow = v and 0xFF }

    // NR34 — trigger (bit 7), length enable (bit 6 readable), freq high (bits 2:0)
    fun readNR34(): UByte = ((if (lengthEnable) 0x40 else 0) or 0xBF).toUByte()
    fun writeNR34(v: Int) {
        frequencyHigh = v and 7; lengthEnable = (v and 0x40) != 0
        if ((v and 0x80) != 0) trigger()
    }
}