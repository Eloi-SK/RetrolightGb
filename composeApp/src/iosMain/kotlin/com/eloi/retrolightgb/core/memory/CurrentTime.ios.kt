package com.eloi.retrolightgb.core.memory

import platform.Foundation.NSDate

actual fun currentUnixSeconds(): Long = NSDate.date().timeIntervalSince1970.toLong()