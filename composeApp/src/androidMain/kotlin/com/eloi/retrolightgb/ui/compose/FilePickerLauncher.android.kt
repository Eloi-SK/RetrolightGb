package com.eloi.retrolightgb.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.eloi.retrolightgb.ui.FilePickerLauncher

@Composable
actual fun rememberFilePickerLauncher(onResult: (ByteArray) -> Unit): FilePickerLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        FilePickerLauncher(
            context = context,
            scope = coroutineScope,
            registry = context.activity.activityResultRegistry,
            onResult = onResult
        )
    }
}

private val Context.activity: FragmentActivity
    get() = unwrapped as FragmentActivity

private val Context.unwrapped: Context
    get() {
        var ctx = this
        while (ctx !is Activity && ctx is ContextWrapper) {
            ctx = ctx.baseContext
        }
        return ctx
    }