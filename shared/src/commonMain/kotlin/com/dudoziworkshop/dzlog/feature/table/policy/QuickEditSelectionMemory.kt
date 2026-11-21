package com.dudoziworkshop.dzlog.feature.table.policy

/**
 * Remembers a quick-edit focus only while AppRoot is alive.
 * No DataStore, photo metadata, or template persistence is involved.
 */
class QuickEditSelectionMemory {
    private val lastSelectedByTemplate = mutableMapOf<String, String>()

    fun initialSelection(templateId: String?, editableCellIds: List<String>): String? {
        val remembered = templateId?.let(lastSelectedByTemplate::get)
        return remembered?.takeIf { it in editableCellIds } ?: editableCellIds.firstOrNull()
    }

    fun recordSelection(
        templateId: String?,
        cellId: String,
        editableCellIds: List<String>,
    ) {
        if (templateId != null && cellId in editableCellIds) {
            lastSelectedByTemplate[templateId] = cellId
        }
    }
}
