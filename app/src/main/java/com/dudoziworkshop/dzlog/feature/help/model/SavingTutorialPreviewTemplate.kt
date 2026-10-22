package com.dudoziworkshop.dzlog.feature.help.model

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * Add local examples only when a passive saving tutorial cannot demonstrate
 * a two-item reorder. Never persist this preview copy to user templates.
 */
internal fun savingTutorialPreviewTemplate(template: TableTemplateState): TableTemplateState {
    val existing = template.fileNameSlotDrafts.filterNotNull()
    if (existing.size >= 2) return template

    val examples = listOf(
        TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "예시"),
        TableEditorSlotDraft(kind = "FORMAT", label = "날짜", formatType = "DATE", formatPattern = "yyyyMMdd"),
    )
    val preview = existing + examples.take(2 - existing.size)
    return template.copy(
        fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) { preview.getOrNull(it) },
    )
}
