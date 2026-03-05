package com.dudoziworkshop.dzlog.domain.preview

enum class TickUnit(val millis: Long) {
    SECOND(1_000L),
    MINUTE(60_000L),
    DAY(86_400_000L)
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
