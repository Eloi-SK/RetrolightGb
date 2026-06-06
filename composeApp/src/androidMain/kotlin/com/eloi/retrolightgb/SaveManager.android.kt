package com.eloi.retrolightgb

import android.content.Context
import java.io.File

actual object SaveManager {
    private var savesDir: File? = null

    // Called from MainActivity.onCreate before any ROM is loaded.
    fun init(context: Context) {
        savesDir = File(context.filesDir, "saves").also { it.mkdirs() }
    }

    actual fun load(name: String): ByteArray? {
        val file = File(savesDir ?: return null, "$name.sav")
        return if (file.exists()) file.readBytes() else null
    }

    actual fun save(name: String, data: ByteArray) {
        File(savesDir ?: return, "$name.sav").writeBytes(data)
    }
}