package com.dudoziworkshop.dzlog.feature.counter.camera

import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.feature.counter.core.CounterReadResult
import com.dudoziworkshop.dzlog.feature.counter.core.CounterSyncDecider
import com.dudoziworkshop.dzlog.feature.counter.core.CounterSyncReason

internal fun buildCameraRequestKey(
    relativePathKey: String,
    prefix: String,
    scanPrefix: String,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): String {
    // stream 식별 정책: 5개 축을 동일 순서로 고정 직렬화한다.
    return listOf(
        relativePathKey,
        prefix,
        scanPrefix,
        includePathInScope.toString(),
        includeFilenameInScope.toString(),
    ).joinToString("|")
}

internal fun detectCameraSyncReason(
    isInitial: Boolean,
    isResumeEvent: Boolean,
    counterEvent: CameraCounterSyncEvent?,
    previousSaveMode: SaveMode?,
    currentSaveMode: SaveMode,
    previousRequestKey: String?,
    currentRequestKey: String,
): CounterSyncReason {
    // 우선순위 정책 유지: initial > undo > mode change > stream change > capture > resume > default(resume)
    return when {
        isInitial -> CounterSyncReason.INITIAL
        counterEvent == CameraCounterSyncEvent.UNDO_COMMITTED -> CounterSyncReason.UNDO
        previousSaveMode != null && previousSaveMode != currentSaveMode -> CounterSyncReason.SAVE_MODE_CHANGE
        previousRequestKey != null && previousRequestKey != currentRequestKey -> CounterSyncReason.STREAM_CHANGE
        counterEvent == CameraCounterSyncEvent.CAPTURE_COMMITTED -> CounterSyncReason.CAPTURE_COMMITTED
        isResumeEvent -> CounterSyncReason.RESUME
        else -> CounterSyncReason.RESUME
    }
}

internal fun applyCameraSyncedNext(
    reason: CounterSyncReason,
    currentDisplayedNext: Int,
    read: CounterReadResult,
    previousRequestKey: String?,
    currentRequestKey: String,
): Int {
    val isSameStream = previousRequestKey == currentRequestKey
    val decision = CounterSyncDecider.decide(
        reason = reason,
        currentDisplayedNext = currentDisplayedNext,
        resolvedNext = read.next,
        isSameStream = isSameStream,
    )
    return decision.appliedNext
}
