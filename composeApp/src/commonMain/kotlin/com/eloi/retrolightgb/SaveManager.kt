package com.eloi.retrolightgb

expect object SaveManager {
    fun load(name: String): ByteArray?
    fun save(name: String, data: ByteArray)
}