package com.eloi.retrolightgb

import java.io.File

actual object SaveManager {
    private val savesDir = File(System.getProperty("user.home"), ".retrolightgb/saves").also { it.mkdirs() }

    actual fun load(name: String): ByteArray? {
        val file = File(savesDir, "$name.sav")
        return if (file.exists()) file.readBytes() else null
    }

    actual fun save(name: String, data: ByteArray) {
        File(savesDir, "$name.sav").writeBytes(data)
    }
}