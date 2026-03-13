package com.dudoziworkshop.dzlog.feature.counter

import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.SaveMode

internal data class CounterRequest(
    // 화면에서 들어온 원본 모드(로그/분석/추적 용도)
    val saveMode: SaveMode,
    // resolver가 엔진 호출 축으로 정규화한 유효 모드.
    // 계약: BOTH == WATERMARK_ONLY, ORIGINAL_ONLY는 별도 축.
    val effectiveSaveMode: SaveMode,
    // resolver가 saveMode 정책까지 반영해 만든 최종 카운터 stream 입력.
    val relativePathKey: String,
    val prefix: String,
    val scanPrefix: String,
    val includePathInScope: Boolean,
    val includeFilenameInScope: Boolean,
    val tableTemplateId: String? = null,
)

internal data class CounterReadResult(
    val next: Int,
    val hasManualOverride: Boolean,
    // 식별은 facade 전용 규칙이 아니라 기존 domain scoped stream을 그대로 사용한다.
    val scopedStream: CaptureScopedCounterStream,
)

internal enum class CounterSyncReason {
    INITIAL,
    RESUME,
    // 촬영 1건 저장 완료 이후 같은 stream에서 다음 값을 전진 반영할 때 사용한다.
    CAPTURE_COMMITTED,
    SAVE_MODE_CHANGE,
    UNDO,
    STREAM_CHANGE,
    MANUAL_SET,
}

internal data class CounterSyncDecision(
    val appliedNext: Int,
    val shouldReplace: Boolean,
    val allowDownwardSync: Boolean,
)
