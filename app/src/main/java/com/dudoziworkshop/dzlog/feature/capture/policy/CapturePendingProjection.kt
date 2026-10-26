package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.domain.table.applyPatch

/**
 * Session projection for photos whose media commit has not completed.
 *
 * The source template remains unchanged until a save is committed.
 * Preview computation reads this projected template so rotating text and
 * counter cells advance for the NEXT exposure, not at save completion.
 *
 * The caller must persist a reservation before adding it here, and apply a
 * completed patch to the actual template before acknowledging its token.
 */
internal class CapturePendingProjection {
    internal data class Shot(
        val id: String,
        val scope: CaptureReservationScope,
        val counter: Int,
        val templatePatch: TablePatch,
    )

    private val pending = linkedMapOf<String, Shot>()

    // Catalog state may arrive one Compose frame after durable persistence.
    // Keep committed progression until that catalog update is observed.
    private var latestCommittedTemplate: TableTemplateState? = null

    private var boundTemplateId: String? = null

    /**
     * A session projection must never carry an old template's committed cache
     * into a different quick template. A switch during pending shots is unsafe.
     */
    @Synchronized
    fun bindTemplate(id: String?): Boolean {
        if (boundTemplateId == id) return true
        if (pending.isNotEmpty()) return false
        latestCommittedTemplate = null
        boundTemplateId = id
        return true
    }

    @Synchronized
    fun committedBase(catalogTemplate: TableTemplateState): TableTemplateState {
        val committed = latestCommittedTemplate ?: return catalogTemplate
        if (committed == catalogTemplate) {
            latestCommittedTemplate = null
            return catalogTemplate
        }
        return committed
    }

    @Synchronized
    fun recordCommittedTemplate(template: TableTemplateState) {
        latestCommittedTemplate = template
    }
    /**
     * A saved table edit may replace the same template while this service survives
     * camera navigation. Outstanding capture patches still own their original base.
     * Reject editing that template until those captures have been reconciled.
     */
    @Synchronized
    fun canSaveEditedTemplate(templateId: String): Boolean =
        pending.isEmpty() || (boundTemplateId != null && boundTemplateId != templateId)

    /**
     * Called only after a successful external table-editor save.
     * The saved catalog is now authoritative; the prior capture-progress cache
     * must not shadow newly edited phrases or cell progress.
     */
    @Synchronized
    fun invalidateCommittedAfterExternalEdit(templateId: String): Boolean {
        if (boundTemplateId != templateId || pending.isNotEmpty()) return false
        latestCommittedTemplate = null
        return true
    }


    @Synchronized
    fun add(shot: Shot) {
        require(shot.counter > 0)
        require(!pending.containsKey(shot.id)) { "Duplicate pending capture: ${shot.id}" }
        check(pending.values.none { it.scope == shot.scope && it.counter == shot.counter }) {
            "Duplicate counter reservation in the same capture stream"
        }
        pending[shot.id] = shot
    }

    @Synchronized
    fun previewTemplate(base: TableTemplateState): TableTemplateState =
        pending.values.fold(committedBase(base)) { current, shot ->
            current.applyPatch(shot.templatePatch)
        }

    @Synchronized
    fun nextCounter(scope: CaptureReservationScope, nextFromMediaStore: Int): Int =
        CaptureReservationPolicy.nextCounter(
            scope,
            nextFromMediaStore,
            pending.values.map {
                CaptureReservation(
                    id = it.id,
                    scope = it.scope,
                    counter = it.counter,
                    createdAtMillis = 0L,
                )
            },
        )

    /**
     * Successful saves complete in FIFO order. No speculative removal on
     * failures: the pending image may already be present in MediaStore.
     */
    @Synchronized
    fun completeFirst(id: String): TablePatch {
        val first = pending.values.firstOrNull()
            ?: error("No pending captures to commit")
        check(first.id == id) { "Out-of-order capture commit" }
        pending.remove(id)
        return first.templatePatch
    }

    @Synchronized
    fun pendingCount(): Int = pending.size

    @Synchronized
    fun hasPending(): Boolean = pending.isNotEmpty()

    @Synchronized
    fun snapshot(): List<Shot> = pending.values.toList()
}
