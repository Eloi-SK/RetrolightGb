package com.eloi.retrolightgb

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform