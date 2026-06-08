package com.eloi.retrolightgb.core

import okio.BufferedSink
import okio.BufferedSource

// Shared helpers for the binary save-state format. Booleans are stored as a
// single byte; all multi-byte integers use Okio's big-endian writeInt/writeShort.
internal fun BufferedSink.writeBoolean(value: Boolean) { writeByte(if (value) 1 else 0) }
internal fun BufferedSource.readBoolean(): Boolean = readByte().toInt() != 0