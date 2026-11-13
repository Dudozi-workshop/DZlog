package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import org.junit.Assert.assertEquals
import org.junit.Test

class RotatingPhraseSetSummaryPolicyTest {
    @Test fun explicitEqualToDefaultStillCountsAsSeparatePeriod() {
        val set = RotatingPhraseSet("s", "조사", listOf("A", "B", "C", "D"), 3,
            itemEveryOverrides = listOf(null, 3, null, 5))
        assertEquals("문구 4개 · 기본 3장 · 별도 주기 2개", rotatingPhraseSetSummary(set))
    }
    @Test fun absentSeparatePeriodsDoNotWasteSpace() {
        assertEquals("문구 0개 · 기본 1장", rotatingPhraseSetSummary(
            RotatingPhraseSet("s", "빈 세트", emptyList())))
        assertEquals("문구 2개 · 기본 4장", rotatingPhraseSetSummary(
            RotatingPhraseSet("s2", "세트", listOf("A", "B"), 4)))
    }
}
