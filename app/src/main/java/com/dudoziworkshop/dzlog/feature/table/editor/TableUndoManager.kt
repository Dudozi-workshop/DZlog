package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

class TableUndoManager(
    private val maxSize: Int = 100,
) {
    private val undoStack = ArrayDeque<TableTemplateState>()

    fun clear() {
        undoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun pushSnapshotBeforeAction(current: TableTemplateState) {
        val last = undoStack.lastOrNull()
        if (last == current) return
        undoStack.addLast(current)
        while (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
    }

    fun undo(current: TableTemplateState): TableTemplateState {
        val previous = undoStack.removeLastOrNull() ?: return current
        return previous
    }
}
