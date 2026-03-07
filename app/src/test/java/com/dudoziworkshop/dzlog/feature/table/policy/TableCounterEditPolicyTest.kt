package com.dudoziworkshop.dzlog.feature.table.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TableCounterEditPolicyTest {

    // streamNext 는 저장된 사진 기준 auto-next 값이다.
    // manual override 값은 비교 기준이 되지 않는다.

    @Test
    fun no_conflict_when_new_value_is_below_manual_but_above_auto_next() {
        val conflict = evaluateCounterEditConflict(
            oldValueText = "88",
            newValueText = "50",
            streamNext = 8,
        )

        assertNull(conflict)
    }

    @Test
    fun conflict_when_new_value_is_below_auto_next() {
        val conflict = evaluateCounterEditConflict(
            oldValueText = "88",
            newValueText = "7",
            streamNext = 8,
        )

        assertNotNull(conflict)
        assertEquals(7, conflict?.pendingCounterCommitValue)
        assertEquals(8, conflict?.streamNextValue)
    }
}
