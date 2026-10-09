package com.dudoziworkshop.dzlog.feature.table.editor

class TableUndoManager<T>(
    private val maxSize: Int = 100,
) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun pushSnapshotBeforeAction(current: T) {
        val last = undoStack.lastOrNull()
        if (last == current) return
        undoStack.addLast(current)
        trimToMaxSize(undoStack)
        redoStack.clear()
    }

    fun undo(current: T): T {
        val restored = undoStack.removeLastOrNull() ?: return current
        redoStack.addLast(current)
        trimToMaxSize(redoStack)
        return restored
    }

    fun redo(current: T): T {
        val restored = redoStack.removeLastOrNull() ?: return current
        undoStack.addLast(current)
        trimToMaxSize(undoStack)
        return restored
    }

    private fun trimToMaxSize(stack: ArrayDeque<T>) {
        while (stack.size > maxSize) {
            stack.removeFirst()
        }
    }
}
