package com.dudoziworkshop.dzlog.feature.table.policy

/**
 * In-memory, per-template focus for camera quick editing.
 *
 * The AppRoot composable owns this instance; it is never serialized, stored in
 * preferences, or written to a saved table template.
 */
internal class QuickCellSelectionMemory {
    private val lastSelected = mutableMapOf<String, String>()

    fun resolve(templateId: String?, editableCellIds: List<String>): String? {
        if (editableCellIds.isEmpty()) {
            if (templateId != null) lastSelected.remove(templateId)
            return null
        }
        val previous = templateId?.let(lastSelected::get)
        if (previous != null && previous in editableCellIds) return previous
        if (templateId != null) lastSelected.remove(templateId)
        return editableCellIds.first()
    }

    fun record(templateId: String?, selectedCellId: String, editableCellIds: List<String>) {
        if (templateId != null && selectedCellId in editableCellIds) {
            lastSelected[templateId] = selectedCellId
        }
    }
}
