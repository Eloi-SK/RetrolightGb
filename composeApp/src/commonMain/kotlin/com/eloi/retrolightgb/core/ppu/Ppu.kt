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

    fun reset() {
        scanLineCycles = 0
        currentLine = 0
        mode = 2
        memory.writeByte(0xFF44u, 0u)
    }

    fun tick(cycles: Int) {
        scanLineCycles += cycles

        when (mode) {
            2 -> {
                if (scanLineCycles >= 80) {
                    scanLineCycles -= 80
                    mode = 3
                    setMode(3)
                }
            }
            3 -> {
                if (scanLineCycles >= 172) {
                    scanLineCycles -= 172
                    mode = 0
                    setMode(0)
                    renderScanLine(currentLine)
                }
            }
            0 -> {
                if (scanLineCycles >= 204) {
                    scanLineCycles -= 204
                    currentLine++
                    memory.writeByte(0xFF44u, currentLine.toUByte())

                    if (currentLine == 144) {
                        mode = 1
                        setMode(1)
                        memory.requestInterrupt(InterruptType.VBlank)

                        val statRegister = memory.readByte(0xFF41u)
                        if ((statRegister.toInt() and 0x10) != 0) {
                            memory.requestInterrupt(InterruptType.LcdStat)
                        }

                    } else {
                        mode = 2
                        setMode(2)

                        val statRegister = memory.readByte(0xFF41u)
                        if ((statRegister.toInt() and 0x20) != 0) {
                            memory.requestInterrupt(InterruptType.LcdStat)
                        }
                    }
                }
            }
            1 -> {
                if (scanLineCycles >= 456) {
                    scanLineCycles -= 456
                    currentLine++
                    memory.writeByte(0xFF44u, currentLine.toUByte())

                    if (currentLine > 153) {
                        currentLine = 0
                        memory.writeByte(0xFF44u, 0u)
                        mode = 2
                        setMode(2)
                    }
                }
            }
        }
        frameBuffer = _frameBuffer
    }

    private fun setMode(mode: Int) {
        val stat = memory.readByte(0xFF41u).toInt() and 0xFC
        memory.writeByte(0xFF41u, (stat or mode).toUByte())
    }

    private fun renderScanLine(line: Int) {
        val lcdc = memory.readByte(0xFF40u)
        val bgEnabled = (lcdc and 0x01u) != 0u.toUByte()
        if (!bgEnabled) return

        val scy = memory.readByte(0xFF42u).toInt()
        val scx = memory.readByte(0xFF43u).toInt()

        val usingTileSet8000 = (lcdc and 0x10u) != 0u.toUByte()
        val usingBgMap0C00 = (lcdc and 0x08u) != 0u.toUByte()

        val tileMapBase = if (usingBgMap0C00) 0x9C00 else 0x9800
        val tileDataBase = if (usingTileSet8000) 0x8000 else 0x8800

        val yInBg = (line + scy) and 0xFF
        val tileRow = yInBg / 8

        for (x in 0 until 160) {
            val xInBg = (x + scx) and 0xFF
            val tileCol = xInBg / 8
            val tileIndexAddress = tileMapBase + tileRow * 32 + tileCol

            val tileId = memory.readByte(tileIndexAddress.toUShort())
            val tileNum = if (usingTileSet8000) {
                tileId.toInt() and 0xFF
            } else {
                tileId.toByte().toInt() + 128
            }

            val tileAddress = tileDataBase + tileNum * 16
            val lineInTitle = yInBg % 8
            val data1 = memory.readByte((tileAddress + lineInTitle * 2).toUShort())
            val data2 = memory.readByte((tileAddress + lineInTitle * 2 + 1).toUShort())

            val bit = 7 - (xInBg % 8)
            val colorId = ((data2.toInt() shr bit) and 1 shl 1) or ((data1.toInt() shr bit) and 1)

            _frameBuffer[line][x] = colorId
        }
    }
}