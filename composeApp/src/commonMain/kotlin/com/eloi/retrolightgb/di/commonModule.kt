package com.eloi.retrolightgb.di

import com.eloi.retrolightgb.core.apu.Apu
import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import com.eloi.retrolightgb.core.ppu.Ppu
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val commonModule = DI.Module("common") {
    bindSingleton<Apu> { Apu() }
    bindSingleton<Memory> { Memory(apu = instance()) }
    bindSingleton<Cpu> { Cpu(memory = instance(), isDebug = false) }
    bindSingleton<Ppu> { Ppu(memory = instance()) }
}