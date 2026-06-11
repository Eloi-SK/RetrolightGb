package com.eloi.retrolightgb.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.eloi.retrolightgb.GameBoy
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.mobile.GameBoyMobileBoxContent

data object GameScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val memory = rememberInstance<Memory>()
        var openRom by remember { mutableStateOf<(() -> Unit)?>(null) }
        var saveState by remember { mutableStateOf<((Int) -> Unit)?>(null) }
        var loadState by remember { mutableStateOf<((Int) -> Unit)?>(null) }

        GameBoyMobileBoxContent(
            onOpenRom = { openRom?.invoke() },
            onSaveState = { slot -> saveState?.invoke(slot) },
            onLoadState = { slot -> loadState?.invoke(slot) },
            onOpenLibrary = { navigator.push(LibraryScreen) },
            onButtonPressed = { memory.pressButton(it) },
            onButtonReleased = { memory.releaseButton(it) },
            screen = {
                GameBoy(
                    onOpenRomReady = { openRom = it },
                    onSaveStateReady = { saveState = it },
                    onLoadStateReady = { loadState = it },
                )
            },
        )
    }
}