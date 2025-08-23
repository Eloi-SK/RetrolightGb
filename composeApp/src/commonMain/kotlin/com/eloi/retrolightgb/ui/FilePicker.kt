package com.eloi.retrolightgb.ui

import androidx.compose.runtime.Composable

@Composable
expect fun FilePicker(show: Boolean, onFileSelected: (ByteArray) -> Unit)
