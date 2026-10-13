package com.dudoziworkshop.dzlog.data.repository

import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.applyPatch

/**
 * Reconcile committed media with a template after process death.
 *
 * Never applies a patch to a different/edited template: that would silently
 * revert user changes or advance the wrong rotating-phrase state.
 */
internal object CaptureReceiptRecoveryPolicy {
    data class Decision(
        val template: TableTemplateState,
        val acknowledgedIds: List<String>,
    )

    fun resolve(
        current: TableTemplateState,
        receipts: List<CapturePendingStore.PendingCapture>,
    ): Decision? {
        val committed = receipts.sortedWith(
            compareBy<CapturePendingStore.PendingCapture> { it.reservation.createdAtMillis }
                .thenBy { it.reservation.id }
        )
        if (committed.any { it.status != CapturePendingStore.Status.COMMITTED }) return null
        if (committed.isEmpty()) return Decision(current, emptyList())
        fun canonical(t: TableTemplateState) =
            tableTemplateStateFromJson(t.toJsonString()) ?: error("Invalid template during recovery")

        val currentCanonical = canonical(current)
        val finalExpected = canonical(
            committed.last().snapshot.request.tableTemplate.applyPatch(
                committed.last().snapshot.tablePatch
            )
        )
        if (currentCanonical == finalExpected) {
            return Decision(current, committed.map { it.reservation.id })
        }

        var template = currentCanonical
        committed.forEach { record ->
            val before = canonical(record.snapshot.request.tableTemplate)
            val after = canonical(before.applyPatch(record.snapshot.tablePatch))
            template = when (template) {
                before -> after
                after -> template // Already applied before the process was interrupted.
                else -> return null
            }
        }
        return Decision(template, committed.map { it.reservation.id })
    }
}
