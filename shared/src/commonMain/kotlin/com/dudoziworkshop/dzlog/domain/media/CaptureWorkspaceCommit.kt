package com.dudoziworkshop.dzlog.domain.media

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.table.applyPatch

/**
 * Calculates a workspace update ONLY after the entire capture is durably saved.
 *
 * The caller must persist the returned workspace, and must not report its counter
 * as committed before that write succeeds. This function performs no file or
 * preference writes and never changes its input objects.
 *
 * In particular, an unsynchronized automatic counter is not assumed to be 1:
 * the camera integration must first resolve the scoped next number.
 */
fun commitConfirmedCaptureToWorkspace(
    current: EditorWorkspace,
    capturedDocument: EditorDocument,
    capturePlan: CaptureMediaSavePlan,
    saved: ConfirmedCaptureMediaSave,
): EditorWorkspace {
    check(saved.plan == capturePlan) { "Save receipt is not for the planned capture." }
    check(current.activeTemplateId == capturedDocument.template.id) {
        "Active template changed before capture commit."
    }
    check(current.active == capturedDocument) {
        "Template, counter or settings changed during capture."
    }
    check(capturedDocument.nextCounter == capturePlan.usedCounter) {
        "Capture counter is not synchronized with the saved document."
    }
    check(capturePlan.usedCounter < Int.MAX_VALUE) { "Capture counter is exhausted." }

    val expectedRoles = when (capturedDocument.saveMode) {
        SaveMode.ORIGINAL_ONLY -> listOf(CaptureMediaRole.ORIGINAL)
        SaveMode.WATERMARK_ONLY -> listOf(CaptureMediaRole.RESULT)
        SaveMode.BOTH -> listOf(CaptureMediaRole.RESULT, CaptureMediaRole.ORIGINAL)
    }
    check(capturePlan.targets.map { it.role } == expectedRoles) {
        "Capture roles differ from the selected save mode."
    }

    val updated = capturedDocument.copy(
        template = capturedDocument.template.copy(
            templateState = capturedDocument.template.templateState.applyPatch(saved.patchAfterSuccess),
        ),
        nextCounter = capturePlan.usedCounter + 1,
    )
    return current.copy(
        documents = current.documents.map {
            if (it.template.id == updated.template.id) updated else it
        },
    )
}
