package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.RotatingCounterProgressMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet

internal const val ROTATING_PHRASE_BLANK_KEY = "__blank__"

/**
 * ROTATING_TEXT의 "카운터 스트림 식별" 키를 계산한다.
 * - 활성 문구를 trim 후 사용
 * - 빈 값은 안정적인 blank key로 고정
 */
internal fun resolveRotatingCounterStreamIdentity(
    activePhraseText: String?,
    phraseSet: RotatingPhraseSet? = null,
): String {
    if (phraseSet?.counterProgressMode == RotatingCounterProgressMode.CONTINUOUS) {
        // Stable policy identity also isolates manual overrides across mode changes.
        return "rc_continuous_${phraseSet.id}"
    }
    return activePhraseText?.trim().orEmpty().ifBlank { ROTATING_PHRASE_BLANK_KEY }
}

