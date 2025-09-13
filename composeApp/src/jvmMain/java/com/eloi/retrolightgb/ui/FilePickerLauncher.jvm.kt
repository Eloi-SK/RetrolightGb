package com.eloi.retrolightgb.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import okio.FileSystem
import okio.IOException

import okio.Path.Companion.toPath
import okio.buffer
import java.awt.FileDialog
import java.awt.Frame

actual class FilePickerLauncher(private val onResult: (ByteArray) -> Unit) {
    actual fun launch() {
        val fileDialog = FileDialog(null as Frame?, "Selectionar arquivo", FileDialog.LOAD)
        fileDialog.isVisible = true
        val fileName = fileDialog.file
        val filePath = fileDialog.directory

        if (filePath != null && fileName != null) {
            val filePathWithFileName = filePath + fileName
            val bytes = readFileToByteArray(filePathWithFileName)
            if (bytes != null) {
                onResult(bytes)
            }
        }
    }
}

private fun readFileToByteArray(filePath: String): ByteArray? {
    return try {
        FileSystem.SYSTEM.source(filePath.toPath()).buffer().use { bufferedSource ->
            bufferedSource.readByteArray()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}