package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan

/**
 * iOS capture transaction: stage -> PhotoKit receipt -> persist workspace.
 * A failed media or workspace write leaves the journal pending; another capture
 * is blocked until recovery verifies the full PhotoKit asset set.
 */
class IosCaptureSaveCoordinator(
    private val media: CaptureMediaRepository,
    private val journal: IosCaptureWorkspaceJournal = IosCaptureWorkspaceJournal(),
) {
    fun hasPendingCapture(): Boolean = journal.hasPending()

    suspend fun saveAndCommit(
        originalWorkspace: EditorWorkspace,
        selectedDocument: EditorDocument,
        plan: CaptureMediaSavePlan,
        jpegByRole: Map<CaptureMediaRole, ByteArray>,
        loadWorkspace: () -> EditorWorkspace,
        persistWorkspace: (EditorWorkspace) -> Boolean,
    ): Boolean {
        check(!journal.hasPending()) {
            "Resolve the previous iOS capture before starting another."
        }
        plan.validatePayloads(jpegByRole)
        check(loadWorkspace() == originalWorkspace) {
            "Workspace changed before the planned capture was staged."
        }
        // The journal MUST be durable before issuing the irreversible PhotoKit write.
        journal.stage(originalWorkspace, selectedDocument, plan)
        val receipt = media.saveAllOrRollback(plan, jpegByRole)
        return journal.complete(receipt, loadWorkspace, persistWorkspace)
    }

    suspend fun recoverPending(
        loadWorkspace: () -> EditorWorkspace,
        persistWorkspace: (EditorWorkspace) -> Boolean,
    ): Boolean {
        if (!journal.hasPending()) return true
        return journal.recover(media.listSavedAssets(), loadWorkspace, persistWorkspace)
    }
}
