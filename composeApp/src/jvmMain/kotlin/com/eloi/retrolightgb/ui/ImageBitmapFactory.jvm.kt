package com.eloi.retrolightgb.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun argbToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    // RGBA_8888 stores bytes in R,G,B,A order regardless of endianness, matching
    // the order we unpack from each 0xAARRGGBB int below.
    val bytes = ByteArray(width * height * 4)
    var j = 0
    for (p in pixels) {
        bytes[j++] = (p shr 16).toByte() // R
        bytes[j++] = (p shr 8).toByte()  // G
        bytes[j++] = p.toByte()          // B
        bytes[j++] = (p shr 24).toByte() // A
    }
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    return Image.makeRaster(info, bytes, width * 4).toComposeImageBitmap()
}