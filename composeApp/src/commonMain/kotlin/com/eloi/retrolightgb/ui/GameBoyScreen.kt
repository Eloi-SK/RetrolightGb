package com.eloi.retrolightgb.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private const val WIDTH = 160
private const val HEIGHT = 144

@Composable
fun GameBoyScreen(
    frameBuffer: IntArray,
    modifier: Modifier = Modifier,
    palette: GameBoyPalette = GameBoyPalette.ClassicDMG,
) {
    val scale = 2

    // Reused ARGB scratch buffer — only the ImageBitmap is rebuilt per frame.
    val argb = remember { IntArray(WIDTH * HEIGHT) }
    val image = remember(frameBuffer, palette) {
        val lut = palette.argb
        for (i in frameBuffer.indices) argb[i] = lut[frameBuffer[i]]
        argbToImageBitmap(argb, WIDTH, HEIGHT)
    }

    Canvas(modifier = modifier.size((WIDTH * scale).dp, (HEIGHT * scale).dp)) {
        drawImage(
            image = image,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.None,
        )
    }
}