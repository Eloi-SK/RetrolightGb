package com.eloi.retrolightgb.core.apu

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

actual class AudioSink actual constructor() {
    private val sampleRate = 44100
    private val minBuf = AudioTrack.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
        )
        .setBufferSizeInBytes(maxOf(minBuf, 4096) * 4)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    actual fun start() { audioTrack.play() }
    actual fun stop() { audioTrack.pause(); audioTrack.flush() }
    actual fun write(samples: ShortArray) {
        audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_NON_BLOCKING)
    }
}