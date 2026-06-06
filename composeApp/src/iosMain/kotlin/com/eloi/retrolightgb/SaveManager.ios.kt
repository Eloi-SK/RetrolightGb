package com.eloi.retrolightgb

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual object SaveManager {
    private val savesDir: String? = run {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        (paths.firstOrNull() as? String)?.let { "$it/saves" }?.also { dir ->
            NSFileManager.defaultManager.createDirectoryAtPath(
                dir, withIntermediateDirectories = true, attributes = null, error = null
            )
        }
    }

    actual fun load(name: String): ByteArray? {
        val path = "${savesDir ?: return null}/$name.sav"
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return ByteArray(data.length.toInt()).also { bytes ->
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
    }

    actual fun save(name: String, data: ByteArray) {
        val path = "${savesDir ?: return}/$name.sav"
        data.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
                .writeToFile(path, atomically = true)
        }
    }
}