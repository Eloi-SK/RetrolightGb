package com.eloi.retrolightgb

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.FilePicker
import com.eloi.retrolightgb.ui.GameBoyScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    CompositionLocalProvider(LocalDI provides di) {
        val memory = rememberInstance<Memory>()
        val cpu = rememberInstance<Cpu>()
        val ppu = rememberInstance<Ppu>()
        val scope = rememberCoroutineScope()

        var showFilePicker by remember { mutableStateOf(false) }

        MaterialTheme {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showFilePicker = true },
                        content = {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                            )
                        },
                    )
                }
            ) { innerPadding ->

                if (showFilePicker) {
                    FilePicker(show = showFilePicker) { bytes ->
                        memory.load(rom = bytes)
                        scope.launch(Dispatchers.Default) {
                            while (true) {
                                cpu.step()
                                ppu.tick(cpu.t - cpu.lastT)
                            }
                        }
                        showFilePicker = false
                    }
                }

                GameBoyScreen(
                    modifier = Modifier.padding(innerPadding),
                    frameBuffer = ppu.frameBuffer,
                )
            }
        }
    }
}