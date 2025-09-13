package com.eloi.retrolightgb.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual class FilePickerLauncher(
    private val context: Context,
    private val scope: CoroutineScope,
    private val registry: ActivityResultRegistry,
    private val onResult: (ByteArray) -> Unit
) {
    @OptIn(ExperimentalUuidApi::class)
    actual fun launch() {
        val key = Uuid.random().toString()
        val contract = ActivityResultContracts.OpenDocument()
        val launcher = registry.register(key, contract) { uri ->
            if (uri != null) {
                scope.launch {
                    val bytes = readFileToByteArrayFromUri(context, uri)
                    if (bytes != null) {
                        onResult(bytes)
                    }
                }
            }
        }

        launcher.launch(arrayOf("*/*"))
    }

    private fun readFileToByteArrayFromUri(context: Context, uri: Uri): ByteArray? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.source().buffer().use { bufferedSource ->
                bufferedSource.readByteArray()
            }
        }
    }
}