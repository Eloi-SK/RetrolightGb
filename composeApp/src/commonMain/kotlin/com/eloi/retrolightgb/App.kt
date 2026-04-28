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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

@Composable
fun App(onOpenRomReady: (openRom: () -> Unit) -> Unit = {}) {
    val memory = rememberInstance<Memory>()
    val cpu = rememberInstance<Cpu>()
    val ppu = rememberInstance<Ppu>()
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberFilePickerLauncher { bytes ->
        memory.load(rom = bytes)
        scope.launch(Dispatchers.Default) {
            val cyclesPerFrame = 70_224           // 456 cycles/line × 154 lines
            val frameDuration = 16_742_706.nanoseconds  // 1s / 59.7275 FPS
            var cycleBudget = 0
            var nextDeadline = TimeSource.Monotonic.markNow() + frameDuration

            try {
                while (true) {
                    cpu.step()
                    val cycles = cpu.t - cpu.lastT
                    ppu.tick(cycles)
                    memory.tickTimer(cycles)
                    cycleBudget += cycles

                    if (cycleBudget >= cyclesPerFrame) {
                        cycleBudget -= cyclesPerFrame
                        // timeLeft is positive when deadline is still in the future
                        val timeLeft = -nextDeadline.elapsedNow()
                        if (timeLeft.isPositive()) {
                            val sleepMs = timeLeft.inWholeMilliseconds - 1
                            if (sleepMs > 0) delay(sleepMs)
                        }
                        // advance by fixed duration — delay overshoot is absorbed next frame
                        nextDeadline += frameDuration
                    }
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