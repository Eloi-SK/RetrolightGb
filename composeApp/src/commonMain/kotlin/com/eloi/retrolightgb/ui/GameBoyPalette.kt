package com.eloi.retrolightgb.ui

import androidx.compose.ui.graphics.Color

// Each palette defines four colors mapped to PPU color IDs 0 (lightest) → 3 (darkest).
enum class GameBoyPalette(val label: String, val c0: Color, val c1: Color, val c2: Color, val c3: Color) {
    ClassicDMG(
        label = "Clássico DMG",
        c0 = Color(0xFF9BBC0F), c1 = Color(0xFF8BAC0F),
        c2 = Color(0xFF306230), c3 = Color(0xFF0F380F)
    ),
    BGB(
        label = "LCD Preciso (BGB)",
        c0 = Color(0xFFE0F8D0), c1 = Color(0xFF88C070),
        c2 = Color(0xFF346856), c3 = Color(0xFF081820)
    ),
    Grayscale(
        label = "Escala de Cinza",
        c0 = Color(0xFFFFFFFF), c1 = Color(0xFFAAAAAA),
        c2 = Color(0xFF555555), c3 = Color(0xFF000000)
    ),
    Pocket(
        label = "Game Boy Pocket",
        c0 = Color(0xFFC4CFA1), c1 = Color(0xFF8B956D),
        c2 = Color(0xFF4D5339), c3 = Color(0xFF1F1F1F)
    ),
    GBLight(
        label = "Game Boy Light",
        c0 = Color(0xFFF0F8C8), c1 = Color(0xFFA8C868),
        c2 = Color(0xFF507050), c3 = Color(0xFF181828)
    );

    fun colorFor(id: Int): Color = when (id) {
        0 -> c0; 1 -> c1; 2 -> c2; else -> c3
    }
}