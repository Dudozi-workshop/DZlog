package com.example.dzlog.domain.capturepolicy

import android.content.Context

/**
 * "카운터 · 저장경로 · 파일명"을 한 번에 결정하는 정책 진입점.
 *
 * Step 3.1: 뼈대만 추가함(아직 호출하지 않음).
 */
internal object CaptureNamingPolicy {

    internal data class Result(
        val streamKey: CaptureStreamKey,
        val relativePath: String,
        val displayName: String,
        val usedCounter: Int
    )

    /**
     * 촬영 시 필요한 naming/counter/path를 한 번에 산출한다.
     *
     * NOTE: 다음 스텝에서 CounterManager/NamePathBuilders로 연결한다.
     */
    internal fun buildForCapture(
        appContext: Context,
        captureContext: CaptureContext,
        counterDigits: Int
    ): Result {
        error("CaptureNamingPolicy.buildForCapture is not wired yet (step 3.1 skeleton)")
    }
}
