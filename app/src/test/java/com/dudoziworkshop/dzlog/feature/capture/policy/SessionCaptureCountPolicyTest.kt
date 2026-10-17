package com.dudoziworkshop.dzlog.feature.capture.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCaptureCountPolicyTest {
    @Test fun oneShotWithOriginalAndResultCountsOnce() {
        val stack = listOf(listOf("original-1", "result-1"), listOf("original-2"))
        assertEquals(2, sessionCaptureBadgeCount(stack))
    }

    @Test fun noCompletedShotHidesBadge() {
        assertNull(sessionCaptureBadgeCount(emptyList<List<String>>()))
        assertNull(sessionCaptureBadgeCount(listOf(emptyList<String>())))
    }

    @Test fun undoRemovalImmediatelyDecreasesCount() {
        val stack = mutableListOf(listOf("a", "a-result"), listOf("b"))
        assertEquals(2, sessionCaptureBadgeCount(stack))
        stack.removeAt(stack.lastIndex)
        assertEquals(1, sessionCaptureBadgeCount(stack))
        stack.clear()
        assertNull(sessionCaptureBadgeCount(stack))
    }

    @Test fun failedOrPendingCaptureIsNotIncludedInCompletedStack() {
        val stack = listOf(listOf("saved-1"), emptyList<String>())
        assertEquals(1, sessionCaptureBadgeCount(stack))
    }
}
