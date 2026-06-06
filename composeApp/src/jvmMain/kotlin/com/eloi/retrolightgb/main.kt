package com.eloi.retrolightgb

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.JoypadButton
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.SerialTerminal
import java.io.BufferedWriter
import java.io.FileWriter

private const val TRACE_TO_FILE = false

fun main() = application {
    CompositionLocalProvider(LocalDI provides di) {
        val cpu = rememberInstance<Cpu>()

        val traceFileWriter = if (cpu.isDebug && TRACE_TO_FILE) {
            BufferedWriter(FileWriter("trace.log")).also { writer ->
                cpu.traceWriter = { line -> writer.write(line); writer.newLine() }
            }
        } else null

        var openRom by remember { mutableStateOf<(() -> Unit)?>(null) }
        var showTerminal by remember { mutableStateOf(false) }
        val memory = rememberInstance<Memory>()

        Window(
            onCloseRequest = {
                memory.save()
                traceFileWriter?.flush()
                traceFileWriter?.close()
                if (cpu.isDebug) println("Last instructions:\n${cpu.dumpTrace()}")
                exitApplication()
            },
            title = "RetrolightGb",
            state = rememberWindowState(width = (160 * 2).dp, height = (144 * 2).dp),
            onKeyEvent = { event ->
                val button = when (event.key) {
                    Key.DirectionRight -> JoypadButton.Right
                    Key.DirectionLeft  -> JoypadButton.Left
                    Key.DirectionUp    -> JoypadButton.Up
                    Key.DirectionDown  -> JoypadButton.Down
                    Key.Z              -> JoypadButton.A
                    Key.X              -> JoypadButton.B
                    Key.Enter          -> JoypadButton.Start
                    Key.Backspace      -> JoypadButton.Select
                    else               -> null
                } ?: return@Window false
                when (event.type) {
                    KeyEventType.KeyDown -> memory.pressButton(button)
                    KeyEventType.KeyUp   -> memory.releaseButton(button)
                }
                true
            }
        ) {
            MenuBar {
                Menu("File", mnemonic = 'F') {
                    Item("Open", mnemonic = 'O', onClick = { openRom?.invoke() })
                    Item("Exit", onClick = ::exitApplication)
                }
                Menu("View", mnemonic = 'V') {
                    Item("Terminal", mnemonic = 'T', onClick = { showTerminal = true })
                }
            }
            GameBoy(onOpenRomReady = { openRom = it })
        }

        if (showTerminal) {
            Window(
                onCloseRequest = { showTerminal = false },
                title = "Serial Terminal",
                state = rememberWindowState(size = DpSize.Unspecified)
            ) {
                SerialTerminal(output = memory.serialOutput)
            }
        }
    }
}