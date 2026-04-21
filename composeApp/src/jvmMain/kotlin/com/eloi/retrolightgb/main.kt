package com.eloi.retrolightgb

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance
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

        Window(
            onCloseRequest = {
                traceFileWriter?.flush()
                traceFileWriter?.close()
                if (cpu.isDebug) println("Last instructions:\n${cpu.dumpTrace()}")
                exitApplication()
            },
            title = "RetrolightGb",
            state = rememberWindowState(width = 640.dp, height = 576.dp)
        ) { App() }
    }
}