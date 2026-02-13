package com.example.dzlog.feature.table.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableCounterConflictDialogPolicyTest {

    @Test
    fun open_sets_visible_and_payload() {
        val conflict = CounterEditConflict(
            pendingCounterCommitValue = 5,
            streamNextValue = 9
        )

        val state = openCounterConflictDialog(
            editingCellId = "cell-1",
            conflict = conflict
        )

        assertTrue(state.isVisible)
        assertEquals("cell-1", state.editingCellId)
        assertEquals(5, state.pendingCounterCommitValue)
        assertEquals(9, state.pendingCounterStreamNextValue)
    }

    @Test
    fun confirm_returns_apply_effect_and_closed_state() {
        val state = TableCounterConflictDialogState(
            isVisible = true,
            editingCellId = "cell-1",
            pendingCounterCommitValue = 0,
            pendingCounterStreamNextValue = 7
        )

        val (nextState, effect) = confirmCounterConflictDialog(state)

        assertFalse(nextState.isVisible)
        assertEquals(null, nextState.editingCellId)
        assertEquals(TableCounterConflictDialogEffect.ApplyManualSeed("cell-1", 1), effect)
    }

    @Test
    fun dismiss_returns_restore_effect_and_closed_state() {
        val state = TableCounterConflictDialogState(
            isVisible = true,
            editingCellId = "cell-2",
            pendingCounterCommitValue = 4,
            pendingCounterStreamNextValue = 8
        )

        val (nextState, effect) = dismissCounterConflictDialog(state)

        assertFalse(nextState.isVisible)
        assertEquals(null, nextState.editingCellId)
        assertEquals(TableCounterConflictDialogEffect.RestoreAutoNext("cell-2"), effect)
    }
}
