package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import java.util.Locale
import java.util.TimeZone

actual fun tableEditorNow(): CaptureTime = CaptureTime(System.currentTimeMillis(),
    Locale.getDefault().toLanguageTag(), TimeZone.getDefault().id)
