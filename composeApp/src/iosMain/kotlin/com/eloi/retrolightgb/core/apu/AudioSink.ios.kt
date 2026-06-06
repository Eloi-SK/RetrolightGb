package com.eloi.retrolightgb.core.apu

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatFloat32
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback

// AVAudioEngine streams non-interleaved Float32 PCM to the hardware.
// write() receives interleaved Int16 stereo from the APU; we de-interleave and
// convert to Float32 [-1.0, 1.0] before scheduling the buffer on the player node.
@OptIn(ExperimentalForeignApi::class)
actual class AudioSink actual constructor() {
    private val engine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()

    // Non-interleaved Float32 stereo — the native format preferred by AVAudioEngine.
    private val format = AVAudioFormat(
        commonFormat = AVAudioPCMFormatFloat32,
        sampleRate = 44100.0,
        channels = 2u,
        interleaved = false
    )!!

    actual fun start() {
        // Activate audio session so playback works when the screen is locked / silent switch is on.
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)

        engine.attachNode(playerNode)
        engine.connect(playerNode, to = engine.mainMixerNode, format = format)
        engine.startAndReturnError(null)
        playerNode.play()
    }

    actual fun stop() {
        playerNode.stop()
        engine.stop()
    }

    actual fun write(samples: ShortArray) {
        val frameCount = samples.size / 2  // 2 channels interleaved → frames
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frameCount.toUInt()) ?: return
        buffer.frameLength = frameCount.toUInt()

        // floatChannelData[0] = left channel, [1] = right channel (non-interleaved / planar)
        val channelData = buffer.floatChannelData ?: return
        val left  = channelData[0] ?: return
        val right = channelData[1] ?: return

        for (i in 0 until frameCount) {
            left[i]  = samples[i * 2]     / 32768f   // Int16 → Float32 [-1.0, 1.0]
            right[i] = samples[i * 2 + 1] / 32768f
        }

        playerNode.scheduleBuffer(buffer, completionHandler = null)
    }
}