package com.eloi.retrolightgb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.GameBoyScreen
import com.eloi.retrolightgb.ui.SerialTerminal
import com.eloi.retrolightgb.ui.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val memory = rememberInstance<Memory>()
    val cpu = rememberInstance<Cpu>()
    val ppu = rememberInstance<Ppu>()
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberFilePickerLauncher { bytes ->
        memory.load(rom = bytes)
        scope.launch(Dispatchers.Default) {
            try {
                while (true) {
                    cpu.step()
                    val cycles = cpu.t - cpu.lastT
                    ppu.tick(cycles)
                    memory.tickTimer(cycles)
                }
            } catch (e: NotImplementedError) {
                println("CPU CRASH: ${e.message}")
                println(cpu.dumpTrace())
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
            Column(modifier = Modifier.padding(innerPadding)) {
                GameBoyScreen(frameBuffer = ppu.frameBuffer)
                SerialTerminal(output = memory.serialOutput)
            }
        }
    }
}