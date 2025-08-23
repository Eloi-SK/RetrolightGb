package com.eloi.retrolightgb.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import okio.buffer
import okio.source

@Composable
actual fun FilePicker(
    show: Boolean,
    onFileSelected: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = readFileToByteArrayFromUri(context, uri)
                if (bytes != null) {
                    onFileSelected(bytes)
                }
            }
        }
    }

    LaunchedEffect(show) {
        if (show) {
            launcher.launch(arrayOf("*/*"))
        }
    }
}

private fun readFileToByteArrayFromUri(context: Context, uri: Uri): ByteArray? {
    return context.contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.source().buffer().use { bufferedSource ->
            bufferedSource.readByteArray()
        }
    }
}