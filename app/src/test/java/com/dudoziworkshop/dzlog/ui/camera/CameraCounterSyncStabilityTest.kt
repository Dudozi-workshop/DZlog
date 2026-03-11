package com.dudoziworkshop.dzlog.ui.camera

import com.dudoziworkshop.dzlog.feature.capture.policy.resolveSyncedScopeNext
import com.dudoziworkshop.dzlog.feature.capture.policy.stabilizeStreamNextCounter
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraCounterSyncStabilityTest {

    @Test
    fun same_stream_does_not_regress_below_current_counter() {
        val stable = stabilizeStreamNextCounter(
            streamNextFromPolicy = 10,
            currentScopeNext = 11,
            isNewStream = false
        )

        assertEquals(11, stable)
    }

    @Test
    fun same_stream_uses_policy_when_it_is_ahead() {
        val stable = stabilizeStreamNextCounter(
            streamNextFromPolicy = 12,
            currentScopeNext = 11,
            isNewStream = false
        )

        assertEquals(12, stable)
    }

    @Test
    fun new_stream_allows_reset_to_policy_value() {
        val stable = stabilizeStreamNextCounter(
            streamNextFromPolicy = 3,
            currentScopeNext = 11,
            isNewStream = true
        )

        assertEquals(3, stable)
    }
    @Test
    fun undo_resync_can_apply_lower_policy_value() {
        val synced = resolveSyncedScopeNext(
            streamNextFromPolicy = 2,
            currentScopeNext = 3,
            isNewScope = false,
            allowDownwardSync = true,
        )

        assertEquals(2, synced)
    }

    @Test
    fun resume_resync_does_not_regress_when_downward_not_allowed() {
        val synced = resolveSyncedScopeNext(
            streamNextFromPolicy = 2,
            currentScopeNext = 3,
            isNewScope = false,
            allowDownwardSync = false,
        )

        assertEquals(3, synced)
    }

}
