package com.dudoziworkshop.dzlog.feature.table.editor

class TableUndoManager<T>(
    private val maxSize: Int = 100,
) {
    private val undoStack = ArrayDeque<T>()

    fun clear() {
        undoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun pushSnapshotBeforeAction(current: T) {
        val last = undoStack.lastOrNull()
        if (last == current) return
        undoStack.addLast(current)
        while (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
    }

    fun undo(current: T): T {
        return undoStack.removeLastOrNull() ?: current
    }
}
