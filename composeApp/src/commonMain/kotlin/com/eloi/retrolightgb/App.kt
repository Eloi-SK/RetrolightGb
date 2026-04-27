package com.eloi.retrolightgb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.GameBoyScreen
import com.eloi.retrolightgb.ui.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun App(onOpenRomReady: (openRom: () -> Unit) -> Unit = {}) {
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

    SideEffect { onOpenRomReady(filePickerLauncher::launch) }

    GameBoyScreen(frameBuffer = ppu.frameBuffer)
}