package com.eloi.retrolightgb.core.ppu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eloi.retrolightgb.core.cpu.InterruptType
import com.eloi.retrolightgb.core.memory.Memory

class Ppu(private val memory: Memory) {
    private val _frameBuffer: Array<IntArray> = Array(144) { IntArray(160) }
    private var scanLineCycles: Int = 0
    private var currentLine: Int = 0
    private var mode: Int = 2

    var frameBuffer by mutableStateOf(_frameBuffer)
        private set

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
                            frameBuffer = Array(144) { _frameBuffer[it].copyOf() }

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

        // bgColorIds tracks raw BG color index (0-3) per pixel for sprite priority
        val bgColorIds = IntArray(160)

        val bgEnabled = (lcdc and 0x01u) != 0u.toUByte()
        if (bgEnabled) {
            val scy = memory.readByte(0xFF42u).toInt()
            val scx = memory.readByte(0xFF43u).toInt()
            val usingTileSet8000 = (lcdc and 0x10u) != 0u.toUByte()
            val usingBgMap9C00 = (lcdc and 0x08u) != 0u.toUByte()
            val tileMapBase = if (usingBgMap9C00) 0x9C00 else 0x9800
            val tileDataBase = if (usingTileSet8000) 0x8000 else 0x8800
            val yInBg = (line + scy) and 0xFF
            val tileRow = yInBg / 8
            val bgp = memory.readByte(0xFF47u).toInt()

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
                _frameBuffer[line][x] = (bgp shr (colorId * 2)) and 0x03
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
                _frameBuffer[line][screenX] = (obp shr (colorId * 2)) and 0x03
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
                println("PPU: LYC=LY Interrupt Requested (LY=${lyValue.toString(16)}, LYC=${lycValue.toString(16)})")
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