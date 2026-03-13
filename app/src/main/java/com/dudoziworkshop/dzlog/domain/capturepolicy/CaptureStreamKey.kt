package com.dudoziworkshop.dzlog.domain.capturepolicy

/**
 * Counter stream identity.
 *
 * - relativePathKey: CounterManager가 사용하는 스트림 키(물리 저장경로 + 가상 분리키 포함 가능)
 * - prefix: 카운터 스트림 분리용 prefix(내부 키)
 * - scanPrefix: MediaStore DISPLAY_NAME 파싱용 실제 파일명 prefix
 */
internal data class CaptureStreamKey(
    val relativePathKey: String,
    val prefix: String,
    val scanPrefix: String,
)
