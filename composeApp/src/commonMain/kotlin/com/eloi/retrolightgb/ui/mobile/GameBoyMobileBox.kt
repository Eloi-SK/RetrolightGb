package com.eloi.retrolightgb.ui.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        MaterialTheme {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { openRom?.invoke() },
                        content = {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                            )
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier.padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameBoy(onOpenRomReady = { openRom = it })
                    MobileJoypad(
                        onButtonPressed = { memory.pressButton(it) },
                        onButtonReleased = { memory.releaseButton(it) }
                    )
                }
            }
        }
    }
}