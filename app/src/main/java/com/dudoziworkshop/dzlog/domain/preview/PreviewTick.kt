package com.dudoziworkshop.dzlog.domain.preview

/**
 * timeFormat/dateFormat 문자열을 보고 "얼마마다 갱신해야 의미가 있는지"를 계산한다.
 * - timeFormat에 초(s/S)가 포함되면 1초 단위
 * - timeFormat에 분(m)이 포함되면 1분 단위
 * - timeFormat에 시(H/h)가 포함되면 1시간 단위
 * - 그 외는 dateFormat 기준으로 1일 단위(날짜만 변함)
 */
enum class TickUnit(val millis: Long) {
    SECOND(1_000L),
    MINUTE(60_000L),
    HOUR(3_600_000L),
    DAY(86_400_000L)
}

fun decideTickUnit(dateFormat: String, timeFormat: String): TickUnit {
    val tf = timeFormat.trim()
    val df = dateFormat.trim()
    val dateHasExplicitTimeToken = df.any { it == 'H' || it == 'h' || it == 's' || it == 'S' }
    return when {
        tf.contains('s', ignoreCase = true) -> TickUnit.SECOND
        tf.contains('m') -> TickUnit.MINUTE
        tf.contains('H') || tf.contains('h') -> TickUnit.HOUR
        dateHasExplicitTimeToken -> TickUnit.HOUR
        // dateFormat은 현재 UI 구성상 날짜 영역 중심이므로 1일 주기 갱신
        else -> TickUnit.DAY
    }
}

/**
 * 현재 시각 기준으로 다음 갱신 시점까지 delay(ms)를 계산한다.
 * 예) MINUTE면 다음 "분" 경계까지 남은 시간을 반환.
 */
fun computeNextDelayMillis(unit: TickUnit, nowMillis: Long = System.currentTimeMillis()): Long {
    val step = unit.millis
    val remainder = nowMillis % step
    val delay = step - remainder
    // 0ms 방지: boundary에 딱 맞으면 최소 1ms라도 쉼
    return if (delay <= 0L) 1L else delay
}
