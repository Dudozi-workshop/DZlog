package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import platform.Foundation.*

actual fun tableEditorNow(): CaptureTime = CaptureTime((NSDate().timeIntervalSince1970 * 1000).toLong(),
    NSLocale.currentLocale.localeIdentifier, NSTimeZone.localTimeZone.name)
