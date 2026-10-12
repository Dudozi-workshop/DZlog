package com.dudoziworkshop.dzlog.feature.capture.policy

import java.util.UUID

/** A counter stream is identified by BOTH the normalized path and its filename prefix. */
internal data class CaptureReservationScope(
    val relativePathKey: String,
    val streamPrefix: String,
)

internal data class CaptureReservation(
    val id: String,
    val scope: CaptureReservationScope,
    val counter: Int,
    val createdAtMillis: Long,
)

internal object CaptureReservationPolicy {
    fun nextCounter(
        scope: CaptureReservationScope,
        seed: Int,
        pending: Collection<CaptureReservation>,
    ): Int {
        val largestReserved = pending.asSequence()
            .filter { it.scope == scope }
            .map { it.counter }
            .maxOrNull() ?: 0
        // Never wrap an exhausted integer counter to a used value.
        check(largestReserved < Int.MAX_VALUE && seed > 0) { "Capture counter exhausted" }
        return maxOf(seed, largestReserved + 1)
    }

    fun create(
        scope: CaptureReservationScope,
        counter: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): CaptureReservation {
        require(counter > 0) { "Capture counter must be positive" }
        return CaptureReservation(
            id = UUID.randomUUID().toString(),
            scope = scope,
            counter = counter,
            createdAtMillis = nowMillis,
        )
    }
}
