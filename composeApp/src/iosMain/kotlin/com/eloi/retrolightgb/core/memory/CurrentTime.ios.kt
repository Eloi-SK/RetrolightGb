package com.eloi.retrolightgb.core.memory

import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970

actual fun currentUnixSeconds(): Long = NSDate.date().timeIntervalSince1970.toLong()