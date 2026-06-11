package com.eloi.retrolightgb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.eloi.retrolightgb.GameBoy
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.GameBoyPalette
import com.eloi.retrolightgb.ui.mobile.GameBoyMobileBoxContent

data object GameScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val memory = rememberInstance<Memory>()
        var openRom by remember { mutableStateOf<(() -> Unit)?>(null) }
        var saveState by remember { mutableStateOf<((Int) -> Unit)?>(null) }
        var loadState by remember { mutableStateOf<((Int) -> Unit)?>(null) }
        var palette by remember { mutableStateOf(GameBoyPalette.ClassicDMG) }
        var showPaletteSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState()

        GameBoyMobileBoxContent(
            onOpenRom = { openRom?.invoke() },
            onSaveState = { slot -> saveState?.invoke(slot) },
            onLoadState = { slot -> loadState?.invoke(slot) },
            onOpenLibrary = { navigator.push(LibraryScreen) },
            onOpenPalette = { showPaletteSheet = true },
            onButtonPressed = { memory.pressButton(it) },
            onButtonReleased = { memory.releaseButton(it) },
            screen = {
                GameBoy(
                    onOpenRomReady = { openRom = it },
                    onSaveStateReady = { saveState = it },
                    onLoadStateReady = { loadState = it },
                    palette = palette,
                )
            },
        )

        if (showPaletteSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPaletteSheet = false },
                sheetState = sheetState,
            ) {
                PaletteSheetContent(
                    current = palette,
                    onSelect = { palette = it; showPaletteSheet = false },
                )
            }
        }
    }
}

@Composable
private fun PaletteSheetContent(
    current: GameBoyPalette,
    onSelect: (GameBoyPalette) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = "Palette",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        GameBoyPalette.entries.forEach { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(p) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row {
                    listOf(p.c0, p.c1, p.c2, p.c3).forEach { color ->
                        Box(modifier = Modifier.size(width = 18.dp, height = 28.dp).background(color))
                    }
                }
                Text(p.label, modifier = Modifier.weight(1f))
                if (p == current) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            }
        }
    }
}