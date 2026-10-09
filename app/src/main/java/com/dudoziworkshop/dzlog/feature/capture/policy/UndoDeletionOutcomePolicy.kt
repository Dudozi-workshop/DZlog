package com.dudoziworkshop.dzlog.feature.capture.policy

/** Describe which files actually remain after a multi-file capture deletion. */
internal data class UndoDeletionOutcome<T>(
    val deleted: List<T>,
    val remaining: List<T>,
)

/** A false result or thrown exception means the file must stay available for Undo. */
internal fun <T> classifyUndoDeletion(
    attempts: List<Pair<T, Result<Boolean>>>,
): UndoDeletionOutcome<T> = UndoDeletionOutcome(
    deleted = attempts.filter { (_, result) -> result.getOrNull() == true }.map { it.first },
    remaining = attempts.filter { (_, result) -> result.getOrNull() != true }.map { it.first },
)
