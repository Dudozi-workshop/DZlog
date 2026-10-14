package com.dudoziworkshop.dzlog.feature.counter.camera

import kotlinx.coroutines.CancellationException

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

/**
 * A pending save must not block a different filename/phrase counter stream.
 * The active stream may reuse its projected seed without a MediaStore read.
 */
internal fun shouldReadCameraCounter(
    isTemplateReady: Boolean,
    isCapturing: Boolean,
    pendingSaveCount: Int,
    isCurrentRequestSynced: Boolean,
): Boolean = isTemplateReady &&
    // A new phrase/path stream can be read as soon as its reservation exists,
    // even while CameraX is staging the preceding exposure.
    (!isCurrentRequestSynced || (!isCapturing && pendingSaveCount == 0))

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

internal suspend fun <T> readCameraCounterSafely(read: suspend () -> T): Result<T> = try {
    Result.success(read())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

