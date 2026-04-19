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

            val bgp = memory.readByte(0xFF47u).toInt()
            val shade = (bgp shr (colorId * 2)) and 0x03

            _frameBuffer[line][x] = shade
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