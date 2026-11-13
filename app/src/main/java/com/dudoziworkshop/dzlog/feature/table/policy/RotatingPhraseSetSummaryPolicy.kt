package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet

/** A single-line set summary. Explicit per-item values count even when equal to the default. */
internal fun rotatingPhraseSetSummary(set: RotatingPhraseSet): String {
    val extra = set.itemEveryOverrides.take(set.items.size).count { it != null }
    return "문구 ${set.items.size}개 · 기본 ${set.defaultEvery}장" +
        if (extra > 0) " · 별도 주기 ${extra}개" else ""
}
