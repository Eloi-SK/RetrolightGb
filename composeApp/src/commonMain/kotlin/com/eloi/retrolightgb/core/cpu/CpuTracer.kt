package com.eloi.retrolightgb.core.cpu

import com.eloi.retrolightgb.core.memory.Memory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class CpuTracer(private val registers: CpuRegisters, private val memory: Memory) {

    class TraceRecord {
        var a: UByte = 0u; var f: UByte = 0u
        var b: UByte = 0u; var c: UByte = 0u
        var d: UByte = 0u; var e: UByte = 0u
        var h: UByte = 0u; var l: UByte = 0u
        var sp: UShort = 0u; var pc: UShort = 0u
        var m0: UByte = 0u; var m1: UByte = 0u
        var m2: UByte = 0u; var m3: UByte = 0u

        fun snapshot(): TraceRecord = TraceRecord().also {
            it.a = a; it.f = f; it.b = b; it.c = c
            it.d = d; it.e = e; it.h = h; it.l = l
            it.sp = sp; it.pc = pc
            it.m0 = m0; it.m1 = m1; it.m2 = m2; it.m3 = m3
        }
    }

    private val buffer = Array(BUFFER_SIZE) { TraceRecord() }
    private var head = 0
    private var full = false

    private var channel: Channel<TraceRecord>? = null
    private var asyncJob: Job? = null

    fun step() {
        val r = registers
        val ch = channel
        if (ch != null) {
            val snap = TraceRecord().also {
                it.a = r.a; it.f = r.f; it.b = r.b; it.c = r.c
                it.d = r.d; it.e = r.e; it.h = r.h; it.l = r.l
                it.sp = r.sp; it.pc = r.pc
                it.m0 = memory.readByte(r.pc)
                it.m1 = memory.readByte((r.pc + 1u).toUShort())
                it.m2 = memory.readByte((r.pc + 2u).toUShort())
                it.m3 = memory.readByte((r.pc + 3u).toUShort())
            }
            ch.trySend(snap)
        } else {
            val rec = buffer[head]
            rec.a = r.a; rec.f = r.f; rec.b = r.b; rec.c = r.c
            rec.d = r.d; rec.e = r.e; rec.h = r.h; rec.l = r.l
            rec.sp = r.sp; rec.pc = r.pc
            rec.m0 = memory.readByte(r.pc)
            rec.m1 = memory.readByte((r.pc + 1u).toUShort())
            rec.m2 = memory.readByte((r.pc + 2u).toUShort())
            rec.m3 = memory.readByte((r.pc + 3u).toUShort())
            head++
            if (head >= BUFFER_SIZE) { head = 0; full = true }
        }
    }

    fun dump(): String {
        val count = if (full) BUFFER_SIZE else head
        val start = if (full) head else 0
        return buildString {
            for (i in 0 until count) {
                if (i > 0) appendLine()
                append(format(buffer[(start + i) % BUFFER_SIZE]))
            }
        }
    }

    fun clear() {
        head = 0
        full = false
    }

    fun startAsyncWriter(scope: CoroutineScope, writer: (String) -> Unit) {
        stopAsyncWriter()
        val ch = Channel<TraceRecord>(Channel.UNLIMITED)
        channel = ch
        asyncJob = scope.launch {
            for (rec in ch) {
                writer(format(rec))
            }
        }
    }

    fun stopAsyncWriter() {
        channel?.close()
        channel = null
        asyncJob?.cancel()
        asyncJob = null
    }

    private fun format(r: TraceRecord): String =
        "A:${r.a.toHexString().uppercase()} F:${r.f.toHexString().uppercase()} " +
        "B:${r.b.toHexString().uppercase()} C:${r.c.toHexString().uppercase()} " +
        "D:${r.d.toHexString().uppercase()} E:${r.e.toHexString().uppercase()} " +
        "H:${r.h.toHexString().uppercase()} L:${r.l.toHexString().uppercase()} " +
        "SP:${r.sp.toHexString().uppercase()} PC:${r.pc.toHexString().uppercase()} " +
        "PCMEM:${r.m0.toHexString().uppercase()},${r.m1.toHexString().uppercase()}," +
        "${r.m2.toHexString().uppercase()},${r.m3.toHexString().uppercase()}"

    companion object {
        private const val BUFFER_SIZE = 200
    }
}
