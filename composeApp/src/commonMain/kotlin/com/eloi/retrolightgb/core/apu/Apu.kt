package com.eloi.retrolightgb.core.apu

import com.eloi.retrolightgb.core.readBoolean
import com.eloi.retrolightgb.core.writeBoolean
import okio.BufferedSink
import okio.BufferedSource

// Game Boy APU — all 4 channels + frame sequencer + stereo mixing.
@OptIn(ExperimentalUnsignedTypes::class)
class Apu {
    private val audioSink = AudioSink()

    // Bresenham sample timer: generate one sample every CPU_CLOCK/SAMPLE_RATE cycles.
    private val SAMPLE_RATE = 44100
    private val CPU_CLOCK = 4_194_304
    private val BUFFER_SAMPLES = 512
    private var sampleAccum = 0
    private val sampleBuffer = ShortArray(BUFFER_SAMPLES * 2)  // stereo interleaved
    private var bufferPos = 0

    // Frame sequencer: 512 Hz = every 8192 T-cycles.
    private val FS_PERIOD = 8192
    private var fsCycles = 0
    private var fsStep = 0

    // Master control registers
    private var masterEnabled = false
    private var nr50 = 0x77  // default: volume 7 on both sides
    private var nr51 = 0xF3  // default panning

    // Wave RAM (0xFF30-0xFF3F)
    val waveRam = UByteArray(16)

    val ch1 = PulseChannel(hasSweep = true)
    val ch2 = PulseChannel(hasSweep = false)
    val ch3 = WaveChannel(waveRam)
    val ch4 = NoiseChannel()

    fun start() = audioSink.start()
    fun stop() = audioSink.stop()

    fun reset() {
        fsCycles = 0; fsStep = 0; sampleAccum = 0; bufferPos = 0
        masterEnabled = false; nr50 = 0x77; nr51 = 0xF3
        waveRam.fill(0u)
        ch1.reset(); ch2.reset(); ch3.reset(); ch4.reset()
    }

    // Save-state. The partially filled output buffer is transient audio, so it is
    // dropped (bufferPos resets to 0) instead of being persisted.
    fun saveState(sink: BufferedSink) {
        sink.writeInt(sampleAccum); sink.writeInt(fsCycles); sink.writeInt(fsStep)
        sink.writeBoolean(masterEnabled); sink.writeInt(nr50); sink.writeInt(nr51)
        sink.write(waveRam.toByteArray())
        ch1.saveState(sink); ch2.saveState(sink); ch3.saveState(sink); ch4.saveState(sink)
    }

    fun loadState(source: BufferedSource) {
        sampleAccum = source.readInt(); fsCycles = source.readInt(); fsStep = source.readInt()
        masterEnabled = source.readBoolean(); nr50 = source.readInt(); nr51 = source.readInt()
        val bytes = source.readByteArray(waveRam.size.toLong())
        for (i in bytes.indices) waveRam[i] = bytes[i].toUByte()
        ch1.loadState(source); ch2.loadState(source); ch3.loadState(source); ch4.loadState(source)
        bufferPos = 0
    }

    fun tick(cycles: Int) {
        if (masterEnabled) {
            ch1.tickTimer(cycles); ch2.tickTimer(cycles)
            ch3.tickTimer(cycles); ch4.tickTimer(cycles)
        }

        fsCycles += cycles
        while (fsCycles >= FS_PERIOD) {
            fsCycles -= FS_PERIOD
            tickFrameSequencer()
        }

        sampleAccum += cycles * SAMPLE_RATE
        while (sampleAccum >= CPU_CLOCK) {
            sampleAccum -= CPU_CLOCK
            pushSample()
        }
    }

    private fun tickFrameSequencer() {
        when (fsStep) {
            0, 2, 4, 6 -> { ch1.clockLength(); ch2.clockLength(); ch3.clockLength(); ch4.clockLength() }
        }
        if (fsStep == 2 || fsStep == 6) ch1.clockSweep()
        if (fsStep == 7) { ch1.clockEnvelope(); ch2.clockEnvelope(); ch4.clockEnvelope() }
        fsStep = (fsStep + 1) and 7
    }

    private fun pushSample() {
        val leftVol  = ((nr50 shr 4) and 7) + 1  // 1..8
        val rightVol = (nr50 and 7) + 1           // 1..8

        val s1 = if (masterEnabled) ch1.getSample() else 0
        val s2 = if (masterEnabled) ch2.getSample() else 0
        val s3 = if (masterEnabled) ch3.getSample() else 0
        val s4 = if (masterEnabled) ch4.getSample() else 0

        var left = 0; var right = 0
        if (nr51 and 0x10 != 0) left  += s1;  if (nr51 and 0x01 != 0) right += s1
        if (nr51 and 0x20 != 0) left  += s2;  if (nr51 and 0x02 != 0) right += s2
        if (nr51 and 0x40 != 0) left  += s3;  if (nr51 and 0x04 != 0) right += s3
        if (nr51 and 0x80 != 0) left  += s4;  if (nr51 and 0x08 != 0) right += s4

        // Scale: 4 channels × max amplitude 15 × max NR50 vol 8 = 480 peak.
        val leftSample  = ((left  * leftVol  * 32767) / (15 * 4 * 8)).toShort()
        val rightSample = ((right * rightVol * 32767) / (15 * 4 * 8)).toShort()

        sampleBuffer[bufferPos++] = leftSample
        sampleBuffer[bufferPos++] = rightSample
        if (bufferPos >= sampleBuffer.size) {
            audioSink.write(sampleBuffer)
            bufferPos = 0
        }
    }

    fun readByte(address: Int): UByte = when (address) {
        0xFF10 -> ch1.readNR10()
        0xFF11 -> ch1.readNR11()
        0xFF12 -> ch1.readNR12()
        0xFF13 -> 0xFFu
        0xFF14 -> ch1.readNR14()
        0xFF15 -> 0xFFu
        0xFF16 -> ch2.readNR11()
        0xFF17 -> ch2.readNR12()
        0xFF18 -> 0xFFu
        0xFF19 -> ch2.readNR14()
        0xFF1A -> ch3.readNR30()
        0xFF1B -> 0xFFu
        0xFF1C -> ch3.readNR32()
        0xFF1D -> 0xFFu
        0xFF1E -> ch3.readNR34()
        0xFF1F -> 0xFFu
        0xFF20 -> 0xFFu
        0xFF21 -> ch4.readNR42()
        0xFF22 -> ch4.readNR43()
        0xFF23 -> ch4.readNR44()
        0xFF24 -> nr50.toUByte()
        0xFF25 -> nr51.toUByte()
        0xFF26 -> {
            var v = if (masterEnabled) 0x80 else 0x00
            if (ch1.isEnabled()) v = v or 0x01
            if (ch2.isEnabled()) v = v or 0x02
            if (ch3.isEnabled()) v = v or 0x04
            if (ch4.isEnabled()) v = v or 0x08
            (v or 0x70).toUByte()  // bits 6:4 always read as 1
        }
        in 0xFF27..0xFF2F -> 0xFFu
        in 0xFF30..0xFF3F -> waveRam[address - 0xFF30]
        else -> 0xFFu
    }

    fun writeByte(address: Int, value: UByte) {
        val v = value.toInt()

        // Most registers are locked when APU is powered off (except NR52 and Wave RAM).
        // On DMG, length counters can still be written while powered off.
        if (!masterEnabled && address in 0xFF10..0xFF25) {
            when (address) {
                0xFF11 -> ch1.writeLengthOnly(v and 0x3F)
                0xFF16 -> ch2.writeLengthOnly(v and 0x3F)
                0xFF1B -> ch3.writeLengthOnly(v)
                0xFF20 -> ch4.writeLengthOnly(v and 0x3F)
            }
            return
        }

        when (address) {
            0xFF10 -> ch1.writeNR10(v)
            0xFF11 -> ch1.writeNR11(v)
            0xFF12 -> ch1.writeNR12(v)
            0xFF13 -> ch1.writeNR13(v)
            0xFF14 -> ch1.writeNR14(v)
            0xFF16 -> ch2.writeNR11(v)
            0xFF17 -> ch2.writeNR12(v)
            0xFF18 -> ch2.writeNR13(v)
            0xFF19 -> ch2.writeNR14(v)
            0xFF1A -> ch3.writeNR30(v)
            0xFF1B -> ch3.writeNR31(v)
            0xFF1C -> ch3.writeNR32(v)
            0xFF1D -> ch3.writeNR33(v)
            0xFF1E -> ch3.writeNR34(v)
            0xFF20 -> ch4.writeNR41(v)
            0xFF21 -> ch4.writeNR42(v)
            0xFF22 -> ch4.writeNR43(v)
            0xFF23 -> ch4.writeNR44(v)
            0xFF24 -> nr50 = v
            0xFF25 -> nr51 = v
            0xFF26 -> {
                val wasEnabled = masterEnabled
                masterEnabled = (v and 0x80) != 0
                if (wasEnabled && !masterEnabled) {
                    ch1.reset(); ch2.reset(); ch3.reset(); ch4.reset()
                    nr50 = 0; nr51 = 0
                }
            }
            in 0xFF30..0xFF3F -> waveRam[address - 0xFF30] = value
        }
    }
}