package com.eloi.retrolightgb.ui.compose

import androidx.compose.runtime.Composable
import com.eloi.retrolightgb.ui.FilePickerLauncher

@Composable
expect fun rememberFilePickerLauncher(onResult: (ByteArray) -> Unit): FilePickerLauncher