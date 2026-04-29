package com.eloi.retrolightgb.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eloi.retrolightgb.ui.FilePickerLauncher
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

@Composable
actual fun rememberFilePickerLauncher(onResult: (ByteArray) -> Unit): FilePickerLauncher {
    return remember {
        FilePickerLauncher(viewController = topViewController(), onResult = onResult)
    }
}

private fun topViewController(): UIViewController {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: error("No root view controller found")
    while (vc.presentedViewController != null) {
        vc = vc.presentedViewController!!
    }
    return vc
}