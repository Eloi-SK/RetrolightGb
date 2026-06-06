package com.eloi.retrolightgb.core.memory

actual fun currentUnixSeconds(): Long = System.currentTimeMillis() / 1000L