package com.dudoziworkshop.dzlog.feature.table.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TableCounterEditPolicyTest {

    // streamNext 는 저장된 사진 기준 auto-next 값이다.
    // manual override 값/이전 후보값은 경고 기준이 아니다.

    @Test
    fun no_conflict_when_new_value_is_below_manual_but_above_auto_next() {
        val conflict = evaluateCounterEditConflict(
            newValueText = "50",
            streamNext = 8,
            lowCounterWarningLatchedInSession = false,
        )

        assertNull(conflict)
    }

    @Test
    fun conflict_when_new_value_is_below_auto_next() {
        val conflict = evaluateCounterEditConflict(
            newValueText = "7",
            streamNext = 8,
            lowCounterWarningLatchedInSession = false,
        )

        assertNotNull(conflict)
        assertEquals(7, conflict?.pendingCounterCommitValue)
        assertEquals(8, conflict?.streamNextValue)
    }

    @Test
    fun low_warning_is_not_reopened_while_latched_in_same_session() {
        val conflict = evaluateCounterEditConflict(
            newValueText = "6",
            streamNext = 8,
            lowCounterWarningLatchedInSession = true,
        )

        assertNull(conflict)
    }

    @Test
    fun low_warning_latch_resets_when_recovered_to_auto_or_above() {
        val stillLow = nextLowCounterWarningLatch(
            pendingCounterCommitValue = 4,
            streamNext = 8,
        )
        val recovered = nextLowCounterWarningLatch(
            pendingCounterCommitValue = 8,
            streamNext = 8,
        )

        assertEquals(true, stillLow)
        assertEquals(false, recovered)
    }
}
