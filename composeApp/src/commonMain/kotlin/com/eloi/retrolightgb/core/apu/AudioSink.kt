package com.eloi.retrolightgb.core.apu

expect class AudioSink() {
    fun start()
    fun stop()
    fun write(samples: ShortArray)
}