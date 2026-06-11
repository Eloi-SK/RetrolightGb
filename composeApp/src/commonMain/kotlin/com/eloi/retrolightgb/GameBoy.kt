package com.eloi.retrolightgb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.eloi.retrolightgb.core.apu.Apu
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.GameBoyPalette
import com.eloi.retrolightgb.ui.GameBoyScreen
import com.eloi.retrolightgb.ui.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

@Composable
fun GameBoy(
    onOpenRomReady: (openRom: () -> Unit) -> Unit = {},
    onSaveStateReady: (saveState: (Int) -> Unit) -> Unit = {},
    onLoadStateReady: (loadState: (Int) -> Unit) -> Unit = {},
    palette: GameBoyPalette = GameBoyPalette.ClassicDMG,
) {
    val memory = rememberInstance<Memory>()
    val cpu = rememberInstance<Cpu>()
    val ppu = rememberInstance<Ppu>()
    val apu = rememberInstance<Apu>()
    val scope = rememberCoroutineScope()
    val emulationJob = remember { mutableListOf<Job>() }

    fun startEmulationLoop() {
        emulationJob += scope.launch(Dispatchers.Default) {
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
                    apu.tick(cycles)
                    cycleBudget += cycles

                    if (cycleBudget >= cyclesPerFrame) {
                        cycleBudget -= cyclesPerFrame

                        ensureActive()
                        // timeLeft is positive when deadline is still in the future
                        val timeLeft = -nextDeadline.elapsedNow()
                        if (timeLeft.isPositive()) {
                            val sleepMs = timeLeft.inWholeMilliseconds - 1
                            if (sleepMs > 0) delay(sleepMs)
                            nextDeadline += frameDuration
                        } else {
                            // Behind schedule (e.g. returning from background) — reset
                            // instead of accumulating debt that would cause catch-up acceleration
                            nextDeadline = TimeSource.Monotonic.markNow() + frameDuration
                        }
                    }
                }
            } catch (e: NotImplementedError) {
                println("CPU CRASH: ${e.message}")
                println(cpu.dumpTrace())
            }
        }
    }

    // Stop the emulation loop and wait for it to fully unwind before mutating shared
    // state. The loop only suspends at frame boundaries, so this lands on a clean,
    // consistent machine state — safe to snapshot, restore, or reset.
    suspend fun stopEmulationLoop() {
        emulationJob.firstOrNull()?.cancelAndJoin()
        emulationJob.clear()
    }

    suspend fun loadRom(bytes: ByteArray) {
        apu.stop()
        stopEmulationLoop()
        cpu.reset()
        ppu.reset()
        memory.load(rom = bytes)
        apu.reset()
        apu.start()
        startEmulationLoop()
    }

    val filePickerLauncher = rememberFilePickerLauncher { bytes ->
        scope.launch(Dispatchers.Default) { loadRom(bytes) }
    }

    val romFlow = rememberInstance<MutableSharedFlow<ByteArray>>()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    LaunchedEffect(romFlow) {
        romFlow.collect { bytes ->
            withContext(Dispatchers.Default) { loadRom(bytes) }
            romFlow.resetReplayCache()
        }
    }

    fun saveState(slot: Int) {
        scope.launch(Dispatchers.Default) {
            val title = memory.currentTitle ?: return@launch  // no ROM loaded
            stopEmulationLoop()
            val blob = SaveState.capture(title, cpu, ppu, memory, apu)
            SaveManager.save(SaveState.slotKey(title, slot), blob)
            startEmulationLoop()
        }
    }

    fun loadState(slot: Int) {
        scope.launch(Dispatchers.Default) {
            val title = memory.currentTitle ?: return@launch  // no ROM loaded
            val blob = SaveManager.load(SaveState.slotKey(title, slot)) ?: return@launch  // no save in slot
            apu.stop()
            stopEmulationLoop()
            SaveState.restore(blob, title, cpu, ppu, memory, apu)
            apu.start()
            startEmulationLoop()
        }
    }

    LifecycleStartEffect(Unit) {
        if (memory.currentTitle != null) {
            apu.start()
            startEmulationLoop()
        }
        onStopOrDispose {
            apu.stop()
            emulationJob.firstOrNull()?.cancel()
            emulationJob.clear()
            memory.save()
        }
    }

    SideEffect {
        onOpenRomReady(filePickerLauncher::launch)
        onSaveStateReady(::saveState)
        onLoadStateReady(::loadState)
    }

    GameBoyScreen(frameBuffer = ppu.frameBuffer, palette = palette)
}