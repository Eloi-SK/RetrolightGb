package com.eloi.retrolightgb.core.ppu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eloi.retrolightgb.core.cpu.InterruptType
import com.eloi.retrolightgb.core.memory.Memory
import okio.BufferedSink
import okio.BufferedSource

class Ppu(private val memory: Memory) {
    // Two flat 160×144 buffers of PPU color IDs (0..3), swapped at VBlank.
    // The PPU renders scanlines into `backBuffer`; on VBlank it publishes that
    // buffer as `frameBuffer` (the front buffer the UI reads) and switches the
    // back buffer to the previous front. This avoids allocating a fresh frame
    // every VBlank — the reference still changes each frame, so Compose recomposes.
    private val bufferA = IntArray(160 * 144)
    private val bufferB = IntArray(160 * 144)
    private var backBuffer = bufferA
    private var scanLineCycles: Int = 0
    private var currentLine: Int = 0
    private var mode: Int = 2
    private var windowLineCounter: Int = 0

    var frameBuffer by mutableStateOf(bufferB)
        private set

    fun reset() {
        scanLineCycles = 0
        currentLine = 0
        mode = 2
        windowLineCounter = 0
        bufferA.fill(0)
        bufferB.fill(0)
        backBuffer = bufferA
        frameBuffer = bufferB
    }

    // Save-state: the scanline state machine plus the currently displayed frame,
    // so the screen shows the restored image immediately (next render repaints it).
    fun saveState(sink: BufferedSink) {
        sink.writeInt(scanLineCycles); sink.writeInt(currentLine); sink.writeInt(mode); sink.writeInt(windowLineCounter)
        val front = frameBuffer
        for (pixel in front) sink.writeInt(pixel)
    }

    fun loadState(source: BufferedSource) {
        scanLineCycles = source.readInt(); currentLine = source.readInt(); mode = source.readInt(); windowLineCounter = source.readInt()
        for (i in backBuffer.indices) backBuffer[i] = source.readInt()
        // Publish the restored pixels as the visible front buffer (mirrors VBlank).
        frameBuffer = backBuffer
        backBuffer = if (backBuffer === bufferA) bufferB else bufferA
    }

    fun tick(cycles: Int) {
        var cyclesToProcess = cycles

        if (!isLcdEnabled()) {
            scanLineCycles = 0
            currentLine = 0
            mode = 2
            setMode(mode)
            return
        }

        while (cyclesToProcess > 0) {
            var cyclesStep = 0
            var completeMode = false

            when (mode) {
                2 -> {
                    cyclesStep = 80 - scanLineCycles
                    if (cyclesToProcess >= cyclesStep) {
                        scanLineCycles = 0
                        mode = 3
                        setMode(mode)
                        completeMode = true
                    } else {
                        scanLineCycles += cyclesToProcess
                    }
                }
                3 -> {
                    cyclesStep = 172 - scanLineCycles
                    if (cyclesToProcess >= cyclesStep) {
                        scanLineCycles = 0
                        mode = 0
                        setMode(mode)
                        renderScanLine(currentLine)
                        completeMode = true
                    } else {
                        scanLineCycles += cyclesToProcess
                    }
                }
                0 -> {
                    cyclesStep = 204 - scanLineCycles
                    if (cyclesToProcess >= cyclesStep) {
                        scanLineCycles = 0
                        currentLine++
                        memory.writeByte(0xFF44u, currentLine.toUByte())
                        checkLyLycCoincidence()

                        if (currentLine == 144) {
                            mode = 1
                            setMode(mode)
                            memory.requestInterrupt(InterruptType.VBlank)
                            // Publish the rendered back buffer and swap to the other.
                            frameBuffer = backBuffer
                            backBuffer = if (backBuffer === bufferA) bufferB else bufferA

                            val statRegister = memory.readByte(0xFF41u)
                            if ((statRegister.toInt() and 0x10) != 0) {
                                memory.requestInterrupt(InterruptType.LcdStat)
                            }
                        } else {
                            mode = 2
                            setMode(mode)

                            val statRegister = memory.readByte(0xFF41u)
                            if ((statRegister.toInt() and 0x20) != 0) {
                                memory.requestInterrupt(InterruptType.LcdStat)
                            }
                        }
                        completeMode = true
                    } else {
                        scanLineCycles += cyclesToProcess
                    }
                }
                1 -> {
                    cyclesStep = 456 - scanLineCycles
                    if (cyclesToProcess >= cyclesStep) {
                        scanLineCycles = 0
                        currentLine++
                        memory.writeByte(0xFF44u, currentLine.toUByte())
                        checkLyLycCoincidence()

                        if (currentLine > 153) {
                            currentLine = 0
                            windowLineCounter = 0
                            memory.writeByte(0xFF44u, 0u)
                            checkLyLycCoincidence()
                            mode = 2
                            setMode(mode)
                        }
                        completeMode = true
                    } else {
                        scanLineCycles += cyclesToProcess
                    }
                }
            }

            val cyclesConsumedInStep = if (completeMode) cyclesStep else cyclesToProcess
            cyclesToProcess -= cyclesConsumedInStep

            if (!completeMode && cyclesToProcess > 0)
                cyclesToProcess = 0
        }
    }

    private fun setMode(mode: Int) {
        val stat = memory.readByte(0xFF41u).toInt() and 0xFC
        memory.writeByte(0xFF41u, (stat or mode).toUByte())
    }

    private fun renderScanLine(line: Int) {
        val lcdc = memory.readByte(0xFF40u)
        val rowOffset = line * 160

        // bgColorIds tracks raw BG color index (0-3) per pixel for sprite priority
        val bgColorIds = IntArray(160)

        val bgEnabled = (lcdc and 0x01u) != 0u.toUByte()
        if (bgEnabled) {
            val usingTileSet8000 = (lcdc and 0x10u) != 0u.toUByte()
            val tileDataBase = if (usingTileSet8000) 0x8000 else 0x8800
            val bgp = memory.readByte(0xFF47u).toInt()

            val scy = memory.readByte(0xFF42u).toInt()
            val scx = memory.readByte(0xFF43u).toInt()
            val usingBgMap9C00 = (lcdc and 0x08u) != 0u.toUByte()
            val tileMapBase = if (usingBgMap9C00) 0x9C00 else 0x9800
            val yInBg = (line + scy) and 0xFF
            val tileRow = yInBg / 8

            for (x in 0 until 160) {
                val xInBg = (x + scx) and 0xFF
                val tileId = memory.readByte((tileMapBase + tileRow * 32 + xInBg / 8).toUShort())
                val tileNum = if (usingTileSet8000) tileId.toInt() and 0xFF else tileId.toByte().toInt() + 128
                val tileAddr = tileDataBase + tileNum * 16 + (yInBg % 8) * 2
                val data1 = memory.readByte(tileAddr.toUShort())
                val data2 = memory.readByte((tileAddr + 1).toUShort())
                val bit = 7 - (xInBg % 8)
                val colorId = ((data2.toInt() shr bit) and 1 shl 1) or ((data1.toInt() shr bit) and 1)
                bgColorIds[x] = colorId
                backBuffer[rowOffset + x] = (bgp shr (colorId * 2)) and 0x03
            }

            // Window layer — drawn over BG, below sprites.
            // On DMG, window requires LCDC bit 0 (bgEnabled) to be set as well.
            val windowEnabled = (lcdc and 0x20u) != 0u.toUByte()
            val wy = memory.readByte(0xFF4Au).toInt()
            val wx = memory.readByte(0xFF4Bu).toInt()
            val windowScreenX = wx - 7  // WX=7 → screen x=0; WX=166 → screen x=159

            if (windowEnabled && line >= wy && wx < 167) {
                val usingWindowMap9C00 = (lcdc and 0x40u) != 0u.toUByte()
                val windowTileMapBase = if (usingWindowMap9C00) 0x9C00 else 0x9800
                val windowTileRow = windowLineCounter / 8

                for (x in maxOf(0, windowScreenX) until 160) {
                    val xInWindow = x - windowScreenX
                    val tileId = memory.readByte((windowTileMapBase + windowTileRow * 32 + xInWindow / 8).toUShort())
                    val tileNum = if (usingTileSet8000) tileId.toInt() and 0xFF else tileId.toByte().toInt() + 128
                    val tileAddr = tileDataBase + tileNum * 16 + (windowLineCounter % 8) * 2
                    val data1 = memory.readByte(tileAddr.toUShort())
                    val data2 = memory.readByte((tileAddr + 1).toUShort())
                    val bit = 7 - (xInWindow % 8)
                    val colorId = ((data2.toInt() shr bit) and 1 shl 1) or ((data1.toInt() shr bit) and 1)
                    bgColorIds[x] = colorId
                    backBuffer[rowOffset + x] = (bgp shr (colorId * 2)) and 0x03
                }
                windowLineCounter++
            }
        }

        val spritesEnabled = (lcdc and 0x02u) != 0u.toUByte()
        if (!spritesEnabled) return

        val tallSprites = (lcdc and 0x04u) != 0u.toUByte()
        val spriteHeight = if (tallSprites) 16 else 8

        data class SpriteEntry(val y: Int, val x: Int, val tileNum: Int, val attrs: Int)
        val visibleSprites = mutableListOf<SpriteEntry>()

        for (i in 0 until 40) {
            if (visibleSprites.size >= 10) break
            val oamBase = 0xFE00 + i * 4
            val spriteY = memory.readByte(oamBase.toUShort()).toInt() - 16
            if (line < spriteY || line >= spriteY + spriteHeight) continue
            visibleSprites.add(SpriteEntry(
                y = spriteY,
                x = memory.readByte((oamBase + 1).toUShort()).toInt() - 8,
                tileNum = memory.readByte((oamBase + 2).toUShort()).toInt() and 0xFF,
                attrs = memory.readByte((oamBase + 3).toUShort()).toInt()
            ))
        }

        // Stable sort by X; draw reversed so lower-X / lower-OAM-index wins (drawn on top)
        visibleSprites.sortBy { it.x }

        for (sprite in visibleSprites.reversed()) {
            val xFlip   = (sprite.attrs and 0x20) != 0
            val yFlip   = (sprite.attrs and 0x40) != 0
            val behindBg = (sprite.attrs and 0x80) != 0
            val obp = memory.readByte(if ((sprite.attrs and 0x10) != 0) 0xFF49u else 0xFF48u).toInt()

            var lineInSprite = line - sprite.y
            if (yFlip) lineInSprite = spriteHeight - 1 - lineInSprite

            val (spriteTileNum, spriteTileRow) = if (tallSprites) {
                val baseTile = sprite.tileNum and 0xFE
                if (lineInSprite < 8) Pair(baseTile, lineInSprite) else Pair(baseTile or 0x01, lineInSprite - 8)
            } else {
                Pair(sprite.tileNum, lineInSprite)
            }

            val tileAddr = 0x8000 + spriteTileNum * 16 + spriteTileRow * 2
            val data1 = memory.readByte(tileAddr.toUShort())
            val data2 = memory.readByte((tileAddr + 1).toUShort())

            for (px in 0 until 8) {
                val screenX = sprite.x + px
                if (screenX < 0 || screenX >= 160) continue
                val bit = if (xFlip) px else 7 - px
                val colorId = ((data2.toInt() shr bit) and 1 shl 1) or ((data1.toInt() shr bit) and 1)
                if (colorId == 0) continue                          // color 0 = transparent
                if (behindBg && bgColorIds[screenX] != 0) continue // behind non-zero BG pixels
                backBuffer[rowOffset + screenX] = (obp shr (colorId * 2)) and 0x03
            }
        }
    }

    private fun checkLyLycCoincidence() {
        val lyValue = currentLine.toUByte()
        val lycValue = memory.readByte(0xFF45u)
        var statValue = memory.readByte(0xFF41u)

        if (lyValue == lycValue) {
            statValue = (statValue.toInt() or 0x04).toUByte()

            if ((statValue.toInt() and 0x40) != 0) {
                memory.requestInterrupt(InterruptType.LcdStat)
            }
        } else {
            statValue = (statValue.toInt() and 0xFB).toUByte()
        }
        memory.writeByte(0xFF41u, statValue)
    }

    private fun isLcdEnabled(): Boolean {
        return (memory.readByte(0xFF40u) and 0x80u) != 0u.toUByte()
    }
}