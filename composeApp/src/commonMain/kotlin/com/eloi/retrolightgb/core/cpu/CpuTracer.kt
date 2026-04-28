package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.memory.Memory

class CpuTracer(private val registers: CpuRegisters, private val memory: Memory) {

    private val buffer = arrayOfNulls<String>(BUFFER_SIZE)
    private var head = 0
    private var full = false
    var writer: ((String) -> Unit)? = null

    fun step() {
        val r = registers
        val b0 = memory.readByte(r.pc).toHexString().uppercase()
        val b1 = memory.readByte((r.pc + 1u).toUShort()).toHexString().uppercase()
        val b2 = memory.readByte((r.pc + 2u).toUShort()).toHexString().uppercase()
        val b3 = memory.readByte((r.pc + 3u).toUShort()).toHexString().uppercase()
        record(
            "A:${r.a.toHexString().uppercase()} F:${r.f.toHexString().uppercase()} " +
            "B:${r.b.toHexString().uppercase()} C:${r.c.toHexString().uppercase()} " +
            "D:${r.d.toHexString().uppercase()} E:${r.e.toHexString().uppercase()} " +
            "H:${r.h.toHexString().uppercase()} L:${r.l.toHexString().uppercase()} " +
            "SP:${r.sp.toHexString().uppercase()} PC:${r.pc.toHexString().uppercase()} " +
            "PCMEM:$b0,$b1,$b2,$b3"
        )
    }

    fun dump(): String {
        val count = if (full) BUFFER_SIZE else head
        val start = if (full) head else 0
        return buildString {
            for (i in 0 until count) {
                if (i > 0) appendLine()
                append(buffer[(start + i) % BUFFER_SIZE] ?: "")
            }
        }
    }

    fun clear() {
        buffer.fill(null)
        head = 0
        full = false
    }

    private fun record(msg: String) {
        val w = writer
        if (w != null) {
            w(msg)
        } else {
            buffer[head] = msg
            head++
            if (head >= BUFFER_SIZE) { head = 0; full = true }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 200
    }
}
