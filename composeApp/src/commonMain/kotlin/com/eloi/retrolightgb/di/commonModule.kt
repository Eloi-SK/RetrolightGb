package com.eloi.retrolightgb.di

import com.eloi.retrolightgb.core.cpu.Cpu
import com.eloi.retrolightgb.core.memory.Memory
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val commonModule = DI.Module("common") {
    bindSingleton<Memory> { Memory() }
    bindSingleton<Cpu> { Cpu(memory = instance(), isDebug = true) }
}