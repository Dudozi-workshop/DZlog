package com.dudoziworkshop.dzlog.feature.capture.policy

/**
 * A successful shutter press is one entry containing one or more output files.
 * The stack is the same successful-capture source of truth used by Undo.
 * Pending, failed, and empty outputs are not counted.
 */
internal fun sessionCaptureBadgeCount(captures: List<List<*>>): Int? =
    captures.count { it.isNotEmpty() }.takeIf { it > 0 }
