package com.example.dzlog.domain.preview

/**
 * timeFormat/dateFormat 문자열을 보고 "얼마마다 갱신해야 의미가 있는지"를 계산한다.
 * - 초(s/S)가 포함되면 1초 단위
 * - 분(m)가 포함되면 1분 단위
 * - 시(H/h)가 포함되면 1시간 단위
 * - 그 외는 1일 단위(날짜만 변함)
 */
enum class TickUnit(val millis: Long) {
       SECOND(1_000L),
        MINUTE(60_000L),
        HOUR(3_600_000L),
        DAY(86_400_000L)
    }

fun decideTickUnit(dateFormat: String, timeFormat: String): TickUnit {
        // timeFormat 우선 (시간 표시가 있으면 timeFormat이 실시간성을 결정)
        val tf = timeFormat.trim()
        return when {
                tf.contains('s', ignoreCase = true) -> TickUnit.SECOND
                tf.contains('m') -> TickUnit.MINUTE
                tf.contains('H') || tf.contains('h') -> TickUnit.HOUR
                else -> {
                        // timeFormat이 사실상 비어있거나 의미 없는 경우: dateFormat 기준으로 1일
                        TickUnit.DAY
                    }
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