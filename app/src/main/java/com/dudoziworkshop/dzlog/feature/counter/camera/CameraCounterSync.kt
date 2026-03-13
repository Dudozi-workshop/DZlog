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
    return listOf(
        relativePathKey,
        prefix,
        scanPrefix,
        includePathInScope,
        includeFilenameInScope,
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
    if (isInitial) return CounterSyncReason.INITIAL
    if (counterEvent == CameraCounterSyncEvent.UNDO_COMMITTED) return CounterSyncReason.UNDO
    if (previousSaveMode != null && previousSaveMode != currentSaveMode) return CounterSyncReason.SAVE_MODE_CHANGE
    if (previousRequestKey != null && previousRequestKey != currentRequestKey) return CounterSyncReason.STREAM_CHANGE
    // CAPTURE_COMMITTED는 stream 전환이 아니라 같은 stream 정상 전진 이벤트다.
    if (counterEvent == CameraCounterSyncEvent.CAPTURE_COMMITTED) return CounterSyncReason.CAPTURE_COMMITTED
    if (isResumeEvent) return CounterSyncReason.RESUME
    // 명시 이벤트가 없는 재실행 cycle은 resume-equivalent 처리로 하향 보호 정책을 재사용한다.
    return CounterSyncReason.RESUME
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
