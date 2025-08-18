package com.eloi.retrolightgb

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import okio.FileSystem
import okio.SYSTEM

import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val memory = remember { Memory() }
    val cpu = remember { Cpu(memory) }
    MaterialTheme {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        while(true) {
                            cpu.step()
                        }
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    },
                )
            }
        ) { innerPadding ->

        }
    }
}