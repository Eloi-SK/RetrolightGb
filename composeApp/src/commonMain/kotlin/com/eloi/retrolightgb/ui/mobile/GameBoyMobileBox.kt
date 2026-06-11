package com.eloi.retrolightgb.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eloi.retrolightgb.GameBoy
import com.eloi.retrolightgb.core.memory.JoypadButton
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.di.LocalDI
import com.eloi.retrolightgb.di.di
import com.eloi.retrolightgb.di.rememberInstance
import org.jetbrains.compose.ui.tooling.preview.Preview

private val BodyColor = Color(0xFFBEBEBE)
private val BezelColor = Color(0xFF1A1A1A)

@Composable
fun GameBoyMobileBox() {
    CompositionLocalProvider(LocalDI provides di) {
        val memory = rememberInstance<Memory>()
        var openRom by remember { mutableStateOf<(() -> Unit)?>(null) }
        var saveState by remember { mutableStateOf<((Int) -> Unit)?>(null) }
        var loadState by remember { mutableStateOf<((Int) -> Unit)?>(null) }

        GameBoyMobileBoxContent(
            onOpenRom = { openRom?.invoke() },
            onSaveState = { slot -> saveState?.invoke(slot) },
            onLoadState = { slot -> loadState?.invoke(slot) },
            onButtonPressed = { memory.pressButton(it) },
            onButtonReleased = { memory.releaseButton(it) },
            screen = {
                GameBoy(
                    onOpenRomReady = { openRom = it },
                    onSaveStateReady = { saveState = it },
                    onLoadStateReady = { loadState = it },
                )
            },
        )
    }
}

@Composable
fun GameBoyMobileBoxContent(
    onOpenRom: () -> Unit,
    onSaveState: (Int) -> Unit,
    onLoadState: (Int) -> Unit,
    onButtonPressed: (JoypadButton) -> Unit,
    onButtonReleased: (JoypadButton) -> Unit,
    screen: @Composable () -> Unit,
) {
    MaterialTheme {
        var fabExpanded by remember { mutableStateOf(false) }
        val fabRotation by animateFloatAsState(targetValue = if (fabExpanded) 180f else 0f)

        Scaffold(
            containerColor = BodyColor,
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SmallFloatingActionButton(onClick = {
                                fabExpanded = false
                                onSaveState(0)
                            }) {
                                Icon(Icons.Filled.Save, contentDescription = "Save state")
                            }
                            SmallFloatingActionButton(onClick = {
                                fabExpanded = false
                                onLoadState(0)
                            }) {
                                Icon(Icons.Filled.Download, contentDescription = "Load state")
                            }
                            SmallFloatingActionButton(onClick = {
                                fabExpanded = false
                                onOpenRom()
                            }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Open ROM")
                            }
                        }
                    }
                    FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                        Icon(
                            if (fabExpanded) Icons.Filled.Close else Icons.Filled.Menu,
                            contentDescription = if (fabExpanded) "Close menu" else "Open menu",
                            modifier = Modifier.rotate(fabRotation),
                        )
                    }
                }
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BezelColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(
                            top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp,
                        ),
                    ) {
                        Text(
                            text = "RETRO LIGHT GB",
                            color = Color(0xFF666688),
                            fontSize = 7.sp,
                            letterSpacing = 2.sp,
                        )
                        Box(
                            modifier = Modifier
                                .border(4.dp, Color(0xFF080808))
                                .background(Color.Black),
                        ) {
                            screen()
                        }
                    }
                }

                MobileJoypad(
                    onButtonPressed = onButtonPressed,
                    onButtonReleased = onButtonReleased,
                )
            }
        }
    }
}

@Preview
@Composable
private fun GameBoyMobileBoxPreview() {
    GameBoyMobileBoxContent(
        onOpenRom = {},
        onSaveState = {},
        onLoadState = {},
        onButtonPressed = {},
        onButtonReleased = {},
        screen = {
            Box(
                modifier = Modifier
                    .size(320.dp, 288.dp)
                    .background(Color.Black),
            )
        },
    )
}