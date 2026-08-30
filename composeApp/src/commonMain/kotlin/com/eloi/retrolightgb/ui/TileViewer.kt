package com.eloi.retrolightgb.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eloi.retrolightgb.core.memory.Memory
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val TILE_COUNT = 384
private const val TILES_PER_ROW = 16
private const val TILE_SIZE = 8
private const val IMG_WIDTH = TILES_PER_ROW * TILE_SIZE                    // 128
private const val IMG_HEIGHT = (TILE_COUNT / TILES_PER_ROW) * TILE_SIZE   // 192
private const val DISPLAY_SCALE = 2

@Composable
fun TileViewer(memory: Memory, palette: GameBoyPalette) {
    var tileImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val argb = remember { IntArray(IMG_WIDTH * IMG_HEIGHT) }

    LaunchedEffect(palette) {
        while (true) {
            val lut = palette.argb
            for (t in 0 until TILE_COUNT) {
                val tileCol = t % TILES_PER_ROW
                val tileRow = t / TILES_PER_ROW
                val tileAddr = 0x8000 + t * 16
                for (y in 0 until TILE_SIZE) {
                    val data1 = memory.readByte((tileAddr + y * 2).toUShort()).toInt()
                    val data2 = memory.readByte((tileAddr + y * 2 + 1).toUShort()).toInt()
                    val pixelY = tileRow * TILE_SIZE + y
                    for (x in 0 until TILE_SIZE) {
                        val bit = 7 - x
                        val colorId = ((data2 shr bit) and 1 shl 1) or ((data1 shr bit) and 1)
                        argb[pixelY * IMG_WIDTH + tileCol * TILE_SIZE + x] = lut[colorId]
                    }
                }
            }
            tileImage = argbToImageBitmap(argb, IMG_WIDTH, IMG_HEIGHT)
            delay(50.milliseconds)
        }
    }

    Box(
        modifier = Modifier
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier.size(
                (IMG_WIDTH * DISPLAY_SCALE).dp,
                (IMG_HEIGHT * DISPLAY_SCALE).dp,
            )
        ) {
            val image = tileImage ?: return@Canvas
            drawImage(
                image = image,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None,
            )
        }
    }
}
