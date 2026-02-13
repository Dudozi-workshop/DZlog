package com.example.dzlog.domain.capturepolicy

import com.example.dzlog.domain.table.ResolvedCell

/**
 * Capture-time context snapshot.
 *
 * 목적
 * - 저장경로(relativePath), 파일명(displayName), 카운터 스트림키(StreamKey) 산출 입력을 1곳으로 모음
 * - UI/Repository가 계산 로직에 직접 관여하지 않도록(SSOT) 경계를 만들기 위한 기반 타입
 *
 * 주의
 * - 카운터 숫자 결정권은 정책(CaptureCounterPolicy)에만 있어야 함
 * - COUNTER 셀 ON/OFF는 "표현"이며, 스트림/증가 로직에 영향을 주면 안 됨
 */
internal data class CaptureContext(
    val resolvedCells: List<ResolvedCell>,
    val fnDelim: String,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String
)
