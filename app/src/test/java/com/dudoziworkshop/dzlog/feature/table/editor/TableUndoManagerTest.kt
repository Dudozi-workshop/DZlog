package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableUndoManagerTest {

    @Test
    fun undo_restores_previous_snapshot() {
        val manager = TableUndoManager()
        val before = TableTemplateState.default
        val after = addRow(before)
        manager.pushSnapshotBeforeAction(before)

        val undone = manager.undo(after)

        assertEquals(before.rows, undone.rows)
        assertTrue(!manager.canUndo())
    }
}
