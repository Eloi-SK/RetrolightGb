package com.eloi.retrolightgb.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eloi.retrolightgb.ui.FilePickerLauncher

@Composable
actual fun rememberFilePickerLauncher(onResult: (ByteArray) -> Unit): FilePickerLauncher =
    remember { FilePickerLauncher(onResult) }