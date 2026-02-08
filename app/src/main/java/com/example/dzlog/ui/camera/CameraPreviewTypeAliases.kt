package com.example.dzlog.ui.camera

/**
 * 패키지 이동(기계적 이동) 후에도 CameraScreen 등 상위 조립자가
 * 기존 타입명을 계속 사용할 수 있도록 typealias 브릿지를 제공함.
 *
 * - 동작/로직 불변
 * - 참조 경로만 연결
 */
internal typealias TapFocusUiState = com.example.dzlog.ui.camera.preview.TapFocusUiState
internal typealias FocusRingPhase = com.example.dzlog.ui.camera.preview.FocusRingPhase
