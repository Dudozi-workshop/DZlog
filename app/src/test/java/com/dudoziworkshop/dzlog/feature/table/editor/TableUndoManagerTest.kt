package com.dudoziworkshop.dzlog.feature.table.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableUndoManagerTest {

    @Test
    fun undo_restores_previous_snapshot() {
        val manager = TableUndoManager<String>()
        val before = "before"
        val after = "after"
        manager.pushSnapshotBeforeAction(before)

        val undone = manager.undo(after)

        assertEquals(before, undone)
        assertTrue(!manager.canUndo())
    }
}
