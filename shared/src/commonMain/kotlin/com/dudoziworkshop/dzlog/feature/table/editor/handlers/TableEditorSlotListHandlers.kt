package com.dudoziworkshop.dzlog.feature.table.editor.handlers

object TableEditorSlotListHandlers {
    fun <T> removeSlotAt(
        slots: List<T?>,
        index: Int,
        normalize: (List<T?>) -> List<T?>,
    ): List<T?> {
        val compacted = slots.filterIndexed { i, item -> i != index && item != null }
        return normalize(compacted)
    }

    fun <T> moveSlot(
        slots: List<T?>,
        from: Int,
        to: Int,
        normalize: (List<T?>) -> List<T?>,
    ): List<T?> {
        val mutable = slots.toMutableList()
        val temp = mutable[from]
        mutable[from] = mutable[to]
        mutable[to] = temp
        return normalize(mutable)
    }

    fun <T> removeCellRefs(
        slots: List<T?>,
        deletedCellIds: Set<String>,
        slotCellId: (T) -> String?,
        normalize: (List<T?>) -> List<T?>,
    ): List<T?> {
        val filtered = slots.filter { slot ->
            slot != null && (slotCellId(slot) == null || slotCellId(slot) !in deletedCellIds)
        }
        return normalize(filtered)
    }
}

