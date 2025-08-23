package com.eloi.retrolightgb

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RetrolightGb",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        App()
    }
    var open by remember { mutableStateOf(true) }
     if (open) {
         Window(
             onCloseRequest = { open = false },
             title = "Memory Dump",
             state = rememberWindowState(
                 width = 400.dp,
                 height = 300.dp,
                 position = WindowPosition.Absolute(
                     x = 600.dp,
                     y = 300.dp
                 )
             ),
         ) {
             CompositionLocalProvider(LocalDI provides di) {
                 MaterialTheme {
                     Scaffold { innerPadding ->
                         val memory = rememberInstance<Memory>()
                         Column(
                             modifier = Modifier
                                 .fillMaxSize()
                                 .background(MaterialTheme.colorScheme.primaryContainer)
                                 .padding(innerPadding)
                                 .verticalScroll(rememberScrollState())
                         ) {
                             Text(memory.dump)
                         }
                     }
                 }
             }
         }
     }
}