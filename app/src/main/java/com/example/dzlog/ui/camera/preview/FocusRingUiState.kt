package com.example.dzlog.ui.camera.preview

/**
 * 탭 포커스 링 상태 모델
 * - 목적: 탭 포커스 링 표시를 위한 최소 상태 모델 정의
 * - 포함: FocusRingPhase, TapFocusUiState
 * - 제외: 렌더링/카메라 제어 로직
 */
internal enum class FocusRingPhase { FOCUSING, SUCCESS }

internal data class TapFocusUiState(
    val xPx: Float,
    val yPx: Float,
    val phase: FocusRingPhase = FocusRingPhase.FOCUSING
)
