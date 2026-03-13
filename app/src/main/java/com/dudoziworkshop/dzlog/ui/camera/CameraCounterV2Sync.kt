package com.dudoziworkshop.dzlog.ui.camera

import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.feature.counter.CounterReadResult
import com.dudoziworkshop.dzlog.feature.counter.CounterSyncDecider
import com.dudoziworkshop.dzlog.feature.counter.CounterSyncReason

enum class CameraCounterSyncEvent {
    CAPTURE_COMMITTED,
    UNDO_COMMITTED,
}

internal fun buildCameraV2RequestKey(
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

internal fun detectCameraV2SyncReason(
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
    // 촬영 저장 완료는 stream 전환이 아니라, 같은 stream의 정상 전진 이벤트다.
    if (counterEvent == CameraCounterSyncEvent.CAPTURE_COMMITTED) return CounterSyncReason.CAPTURE_COMMITTED
    if (isResumeEvent) return CounterSyncReason.RESUME
    // 현재 V2 effect는 주기적으로 reason 계산을 수행하므로, 명시 이벤트가 없어도
    // no-op에 가까운 resume-equivalent 처리로 decider 하향 보호 정책을 재사용한다.
    return CounterSyncReason.RESUME
}

internal fun applyCameraV2SyncedNext(
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
