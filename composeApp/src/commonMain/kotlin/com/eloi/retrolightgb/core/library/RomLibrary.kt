package com.eloi.retrolightgb.core.library

import com.eloi.retrolightgb.SaveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object RomLibrary {
    private const val INDEX_KEY = "_lib_index"
    private const val ROM_PREFIX = "_lib_rom_"

    private val _entries = MutableStateFlow(loadFromDisk())
    val entries: StateFlow<List<RomEntry>> = _entries

    private fun loadFromDisk(): List<RomEntry> =
        SaveManager.load(INDEX_KEY)
            ?.decodeToString()
            ?.lines()
            ?.filter { it.isNotBlank() }
            ?.map { RomEntry(it) }
            ?: emptyList()

    private fun persistIndex(list: List<RomEntry>) {
        SaveManager.save(INDEX_KEY, list.joinToString("\n") { it.title }.encodeToByteArray())
    }

    fun add(title: String, bytes: ByteArray) {
        SaveManager.save("$ROM_PREFIX$title", bytes)
        val current = _entries.value.toMutableList()
        current.removeAll { it.title == title }
        current.add(0, RomEntry(title))
        _entries.value = current
        persistIndex(current)
    }

    fun remove(title: String) {
        val current = _entries.value.filter { it.title != title }
        _entries.value = current
        persistIndex(current)
    }

    fun loadBytes(title: String): ByteArray? = SaveManager.load("$ROM_PREFIX$title")
}

fun ByteArray.extractRomTitle(): String =
    (0x0134..0x0143)
        .map { if (it < size) this[it].toInt() and 0xFF else 0 }
        .takeWhile { it != 0 }
        .joinToString("") { it.toChar().toString() }
        .ifBlank { "Unknown" }