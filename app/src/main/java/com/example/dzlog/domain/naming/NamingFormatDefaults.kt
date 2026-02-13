package com.example.dzlog.domain.naming

/**
 * 파일명/표시 포맷 기본값 단일 소스.
 *
 * 주의:
 * - 화면별 의도된 표기 차이(예: compact time) 때문에 상수를 분리한다.
 * - 하드코딩 문자열을 직접 쓰지 않고 이 상수를 참조해 유지보수 리스크를 줄인다.
 */
object NamingFormatDefaults {
    const val FILE_NAME_DELIMITER = "_"

    const val DATE_FORMAT_DEFAULT = "yyyy.MM.dd"

    // 캡처/테이블 상세에서 사용하는 기본 시간 포맷
    const val TIME_FORMAT_CAPTURE_DEFAULT = "HH.mm.ss"

    // 홈/설정 프리뷰에서 쓰는 compact 포맷
    const val TIME_FORMAT_PREVIEW_COMPACT = "HHmm"

    // 일부 UI 테이블 렌더에서 쓰는 콜론 포맷
    const val TIME_FORMAT_RENDER_COLON = "HH:mm:ss"
}
