package com.eloi.retrolightgb.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

val LocalDI = staticCompositionLocalOf<DI> {
    error("No DI provided")
}

@Composable
inline fun <reified T: Any> rememberInstance(): T {
    val di = LocalDI.current
    return di.direct.instance()
}