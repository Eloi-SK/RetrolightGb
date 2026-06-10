package com.eloi.retrolightgb.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eloi.retrolightgb.GameBoy
import com.eloi.retrolightgb.core.memory.JoypadButton
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GameBoyMobileBox() {
    CompositionLocalProvider(LocalDI provides di) {
        val memory = rememberInstance<Memory>()
        var openRom by remember { mutableStateOf<(() -> Unit)?>(null) }
        var saveState by remember { mutableStateOf<((Int) -> Unit)?>(null) }
        var loadState by remember { mutableStateOf<((Int) -> Unit)?>(null) }

        GameBoyMobileBoxContent(
            onOpenRom = { openRom?.invoke() },
            onSaveState = { slot -> saveState?.invoke(slot) },
            onLoadState = { slot -> loadState?.invoke(slot) },
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

@Composable
fun GameBoyMobileBoxContent(
    onOpenRom: () -> Unit,
    onSaveState: (Int) -> Unit,
    onLoadState: (Int) -> Unit,
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
    screen: @Composable () -> Unit,
) {
    MaterialTheme {
        Scaffold(
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SmallFloatingActionButton(onClick = { onSaveState(0) }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save state")
                    }
                    SmallFloatingActionButton(onClick = { onLoadState(0) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Load state")
                    }
                    FloatingActionButton(onClick = onOpenRom) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Open ROM")
                    }
                }
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                screen()
                MobileJoypad(
                    onButtonPressed = onButtonPressed,
                    onButtonReleased = onButtonReleased,
                )
            }
        }
    }
}

@Preview
@Composable
private fun GameBoyMobileBoxPreview() {
    GameBoyMobileBoxContent(
        onOpenRom = {},
        onSaveState = {},
        onLoadState = {},
        onButtonPressed = {},
        onButtonReleased = {},
        screen = {
            Box(
                modifier = Modifier
                    .size(320.dp, 288.dp)
                    .background(Color.Black)
            )
        },
    )
}