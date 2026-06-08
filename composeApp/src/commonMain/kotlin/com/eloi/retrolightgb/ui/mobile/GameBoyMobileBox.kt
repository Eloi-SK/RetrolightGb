package com.eloi.retrolightgb.ui.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eloi.retrolightgb.GameBoy
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance

@Composable
fun GameBoyMobileBox() {
    CompositionLocalProvider(LocalDI provides di) {
        val memory = rememberInstance<Memory>()
        var openRom by remember { mutableStateOf<(() -> Unit)?>(null) }
        var saveState by remember { mutableStateOf<((Int) -> Unit)?>(null) }
        var loadState by remember { mutableStateOf<((Int) -> Unit)?>(null) }

        MaterialTheme {
            Scaffold(
                floatingActionButton = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Quick save / load to slot 0. No-op until a ROM is loaded.
                        SmallFloatingActionButton(onClick = { saveState?.invoke(0) }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save state")
                        }
                        SmallFloatingActionButton(onClick = { loadState?.invoke(0) }) {
                            Icon(Icons.Filled.Download, contentDescription = "Load state")
                        }
                        FloatingActionButton(onClick = { openRom?.invoke() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Open ROM")
                        }
                    }
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier.padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameBoy(
                        onOpenRomReady = { openRom = it },
                        onSaveStateReady = { saveState = it },
                        onLoadStateReady = { loadState = it },
                    )
                    MobileJoypad(
                        onButtonPressed = { memory.pressButton(it) },
                        onButtonReleased = { memory.releaseButton(it) }
                    )
                }
            }
        }
    }
}