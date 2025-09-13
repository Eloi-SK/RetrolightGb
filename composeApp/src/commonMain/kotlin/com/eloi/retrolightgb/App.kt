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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.GameBoyScreen
import com.eloi.retrolightgb.ui.compose.rememberFilePickerLauncher
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

        val filePickerLauncher = rememberFilePickerLauncher { bytes ->
            memory.load(rom = bytes)
            scope.launch(Dispatchers.Default) {
                while (true) {
                    cpu.step()
                    ppu.tick(cpu.t - cpu.lastT)
                }
            }
        }

        MaterialTheme {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = filePickerLauncher::launch,
                        content = {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                            )
                        },
                    )
                }
            ) { innerPadding ->
                GameBoyScreen(
                    modifier = Modifier.padding(innerPadding),
                    frameBuffer = ppu.frameBuffer,
                )
            }
        }
    }
}