package com.dudoziworkshop.dzlog.domain.capturepolicy

/**
 * Counter stream identity.
 *
 * - relativePathKey: CounterManager가 사용하는 스트림 키(물리 저장경로 + 가상 분리키 포함 가능)
 * - prefix: 카운터 prefix(파일명 prefix와는 구분됨. 날짜 셀 등 포함 가능)
 */
internal data class CaptureStreamKey(
    val relativePathKey: String,
    val prefix: String
)
