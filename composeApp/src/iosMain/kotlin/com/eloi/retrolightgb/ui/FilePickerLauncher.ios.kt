package com.eloi.retrolightgb.ui

actual class FilePickerLauncher(private val onResult: (ByteArray) -> Unit) {
    actual fun launch() {}
}