package com.dudoziworkshop.dzlog.feature.capture.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CaptureReservationPolicyTest {
    @Test fun reservationsDoNotCollideWithinSameStream() {
        val scope = CaptureReservationScope("Pictures/DZlog/A/", "T1_")
        val first = CaptureReservationPolicy.create(scope, 12, 100L)
        assertEquals(13, CaptureReservationPolicy.nextCounter(scope, 11, listOf(first)))
        assertNotEquals(first.id, CaptureReservationPolicy.create(scope, 13, 101L).id)
    }

    @Test fun reservationsInOtherStreamDoNotAdvanceCounter() {
        val a = CaptureReservationScope("Pictures/DZlog/A/", "T1_")
        val b = CaptureReservationScope("Pictures/DZlog/B/", "T1_")
        val c = CaptureReservationScope("Pictures/DZlog/A/", "T2_")
        val pending = listOf(CaptureReservationPolicy.create(a, 44))
        assertEquals(3, CaptureReservationPolicy.nextCounter(b, 3, pending))
        assertEquals(3, CaptureReservationPolicy.nextCounter(c, 3, pending))
    }

    @Test fun seedAdvancesPastReservations() {
        val scope = CaptureReservationScope("Pictures/DZlog/", "A_")
        val pending = listOf(CaptureReservationPolicy.create(scope, 4))
        assertEquals(100, CaptureReservationPolicy.nextCounter(scope, 100, pending))
    }

    @Test(expected = IllegalStateException::class)
    fun exhaustedCounterFailsInsteadOfWrapping() {
        val scope = CaptureReservationScope("Pictures/DZlog/", "A_")
        CaptureReservationPolicy.nextCounter(scope, 1, listOf(CaptureReservationPolicy.create(scope, Int.MAX_VALUE)))
    }
}
