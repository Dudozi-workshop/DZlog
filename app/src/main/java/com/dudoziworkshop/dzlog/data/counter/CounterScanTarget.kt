package com.dudoziworkshop.dzlog.data.counter

import com.dudoziworkshop.dzlog.domain.model.SaveMode

enum class CounterScanTarget {
    WATER_ONLY,
    ORIGINAL_ONLY,
    BOTH,
}

fun SaveMode.toCounterScanTarget(): CounterScanTarget = when (this) {
    SaveMode.ORIGINAL_ONLY -> CounterScanTarget.ORIGINAL_ONLY
    SaveMode.WATERMARK_ONLY -> CounterScanTarget.WATER_ONLY
    SaveMode.BOTH -> CounterScanTarget.BOTH
}

