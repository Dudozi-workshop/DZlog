package com.dudoziworkshop.dzlog.domain.counter

internal const val ROTATING_COUNTER_BLANK_KEY: String = "__blank__"

/**
 * ROTATING_TEXT의 "카운터 스트림 식별" 키를 계산한다.
 * - 활성 문구를 trim 후 사용
 * - 빈 값은 안정적인 blank key로 고정
 */
internal fun resolveRotatingCounterStreamIdentity(
    activePhraseText: String?,
): String {
    val normalized = activePhraseText?.trim().orEmpty()
    return if (normalized.isBlank()) ROTATING_COUNTER_BLANK_KEY else normalized
}
