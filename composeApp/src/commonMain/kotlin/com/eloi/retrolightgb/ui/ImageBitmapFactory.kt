package com.eloi.retrolightgb.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Builds an [ImageBitmap] from a row-major array of ARGB pixels.
 *
 * Compose Multiplatform 1.8 has no common API for writing raw pixels into an
 * ImageBitmap, so each target provides its own: Android wraps an `android.graphics.Bitmap`,
 * while JVM and iOS go through Skia. `pixels` must hold `width * height` entries in
 * 0xAARRGGBB order.
 */
expect fun argbToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap