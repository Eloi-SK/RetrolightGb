package com.eloi.retrolightgb

import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory

internal fun testMemory() = Memory(apu = null)

internal fun testCpu(mem: Memory = testMemory()): Cpu {
    val cpu = Cpu(mem, isDebug = false)
    cpu.pc = 0xC100u
    cpu.sp = 0xE000u
    return cpu
}

internal fun Memory.write(addr: Int, vararg bytes: Int) {
    bytes.forEachIndexed { i, b ->
        writeByte((addr + i).toUShort(), b.toUByte())
    }
}

internal fun Cpu.flagZ() = isFlagSet(Cpu.FLAG_Z)
internal fun Cpu.flagN() = isFlagSet(Cpu.FLAG_N)
internal fun Cpu.flagH() = isFlagSet(Cpu.FLAG_H)
internal fun Cpu.flagC() = isFlagSet(Cpu.FLAG_C)