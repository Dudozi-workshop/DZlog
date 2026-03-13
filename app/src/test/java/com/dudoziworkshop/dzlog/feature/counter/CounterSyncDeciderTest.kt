package com.dudoziworkshop.dzlog.feature.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterSyncDeciderTest {

    @Test
    fun `save mode change with empty new stream adopts one`() {
        val decision = CounterSyncDecider.decide(
            reason = CounterSyncReason.SAVE_MODE_CHANGE,
            currentDisplayedNext = 2,
            resolvedNext = 1,
            isSameStream = false,
        )

        assertEquals(1, decision.appliedNext)
        assertTrue(decision.allowDownwardSync)
        assertTrue(decision.shouldReplace)
    }

    @Test
    fun `same stream requery blocks downward change`() {
        val decision = CounterSyncDecider.decide(
            reason = CounterSyncReason.RESUME,
            currentDisplayedNext = 5,
            resolvedNext = 3,
            isSameStream = true,
        )

        assertEquals(5, decision.appliedNext)
        assertFalse(decision.allowDownwardSync)
        assertFalse(decision.shouldReplace)
    }

    @Test
    fun `undo allows downward sync`() {
        val decision = CounterSyncDecider.decide(
            reason = CounterSyncReason.UNDO,
            currentDisplayedNext = 4,
            resolvedNext = 2,
            isSameStream = true,
        )

        assertEquals(2, decision.appliedNext)
        assertTrue(decision.allowDownwardSync)
        assertTrue(decision.shouldReplace)
    }

    @Test
    fun `stream change can adopt lower value`() {
        val decision = CounterSyncDecider.decide(
            reason = CounterSyncReason.STREAM_CHANGE,
            currentDisplayedNext = 8,
            resolvedNext = 2,
            isSameStream = false,
        )

        assertEquals(2, decision.appliedNext)
        assertTrue(decision.allowDownwardSync)
        assertTrue(decision.shouldReplace)
    }
}
