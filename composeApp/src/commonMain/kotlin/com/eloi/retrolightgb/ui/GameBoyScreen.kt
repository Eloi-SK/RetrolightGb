package com.eloi.retrolightgb.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp

@Composable
fun GameBoyScreen(
    frameBuffer: Array<IntArray>,
    modifier: Modifier = Modifier,
) {
    val scale = 2

    Canvas(modifier = modifier.size((160 * scale).dp, (144 * scale).dp)) {
        val pixelW = size.width / 160f
        val pixelH = size.height / 144f
        frameBuffer.forEachIndexed { y, row ->
            row.forEachIndexed { x, colorId ->
                val color = when (colorId) {
                    0 -> Color(color = 0xFFFFFFFF)
                    1 -> Color(color = 0xFFAAAAAA)
                    2 -> Color(color = 0xFF555555)
                    3 -> Color(color = 0xFF000000)
                    else -> Color.Green
                }

                drawRect(
                    color = color,
                    topLeft = Offset(x = x * pixelW, y = y * pixelH),
                    size = Size(width = pixelW, height = pixelH)
                )
            }
        }
    }
}