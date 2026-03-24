package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorSaveResultApplierTest {

    @Test
    fun success_advances_revision_and_requests_notify_exit() {
        val template = defaultTableTemplateState()
        val style = TableStyleState(gridEnabled = false)
        val placement = TablePlacementState(wmWidthRatio = 42)

        val applied = TableEditorSaveResultApplier.apply(
            TableEditorSaveApplyInput(
                result = TableEditorSaveResult.Success(savedPlacement = placement),
                savePayload = template,
                stylePayload = style,
                exitAfterSave = true,
                currentUndoRevision = 3,
            )
        )

        assertEquals(template, applied.nextInitialTemplateSnapshot)
        assertEquals(style, applied.nextInitialStyleSnapshot)
        assertEquals(placement, applied.nextInitialPlacementSnapshot)
        assertTrue(applied.shouldClearUndo)
        assertEquals(4, applied.nextUndoRevision)
        assertTrue(applied.shouldNotifyTemplateChange)
        assertTrue(applied.shouldExitAfterSave)
        assertEquals("저장됨", applied.toastMessage)
    }

    @Test
    fun failure_preserves_revision_and_does_not_notify() {
        val applied = TableEditorSaveResultApplier.apply(
            TableEditorSaveApplyInput(
                result = TableEditorSaveResult.Failure(
                    message = "save failed",
                    isLongToast = true,
                ),
                savePayload = defaultTableTemplateState(),
                stylePayload = TableStyleState(),
                exitAfterSave = true,
                currentUndoRevision = 7,
            )
        )

        assertFalse(applied.shouldClearUndo)
        assertEquals(7, applied.nextUndoRevision)
        assertFalse(applied.shouldNotifyTemplateChange)
        assertFalse(applied.shouldExitAfterSave)
        assertTrue(applied.isLongToast)
        assertEquals("save failed", applied.toastMessage)
        assertNull(applied.nextInitialTemplateSnapshot)
    }
}
