package com.eloi.retrolightgb.core.apu

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

actual class AudioSink actual constructor() {
    private val format = AudioFormat(44100f, 16, 2, true, false)  // 44100 Hz, 16-bit, stereo, signed, little-endian
    private var line: SourceDataLine? = null

    actual fun start() {
        try {
            val info = DataLine.Info(SourceDataLine::class.java, format)
            line = (AudioSystem.getLine(info) as SourceDataLine).also {
                it.open(format, 8192)
                it.start()
            }
        } catch (e: Exception) {
            println("APU: audio init failed — ${e.message}")
        }
    }

    actual fun stop() {
        line?.drain(); line?.stop(); line?.close(); line = null
    }

    actual fun write(samples: ShortArray) {
        val l = line ?: return
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val s = samples[i].toInt()
            bytes[i * 2]     = (s and 0xFF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        l.write(bytes, 0, bytes.size)
    }
}