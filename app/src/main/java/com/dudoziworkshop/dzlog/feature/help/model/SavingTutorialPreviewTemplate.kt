package com.dudoziworkshop.dzlog.feature.help.model

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_UI_MAX_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/** Isolated deterministic tutorial fixture. Never persist to user templates. */
internal fun savingTutorialPreviewTemplate(template: TableTemplateState): TableTemplateState {
    val filename = listOf(
        TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "현장기록"),
        TableEditorSlotDraft(kind = "FORMAT", label = "날짜", formatType = "DATE", formatPattern = "yyyyMMdd"),
    )
    val folders = listOf(
        TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "현장기록"),
        TableEditorSlotDraft(kind = "FORMAT", label = "날짜", formatType = "DATE", formatPattern = "yyyyMMdd"),
    )
    return template.copy(
        fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) { filename.getOrNull(it) },
        pathSlotDrafts = List(PATH_SLOT_UI_MAX_COUNT) { folders.getOrNull(it) },
    )
}
