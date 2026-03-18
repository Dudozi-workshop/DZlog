package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

object TableEditorSlotDraftHandlers {
    fun withUpdatedFileNameSlots(
        base: TableTemplateState,
        updatedSlots: List<FileNameSlotUiItem?>,
        normalize: (List<FileNameSlotUiItem?>) -> List<FileNameSlotUiItem?>,
        toDomain: (FileNameSlotUiItem?) -> com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft?,
    ): TableTemplateState {
        val normalized = normalize(updatedSlots)
        val domainDrafts = normalized.map(toDomain)
        return base.copy(fileNameSlotDrafts = domainDrafts)
    }

    fun withUpdatedPathSlots(
        base: TableTemplateState,
        updatedSlots: List<PathSlotUiItem?>,
        normalize: (List<PathSlotUiItem?>) -> List<PathSlotUiItem?>,
        toDomain: (PathSlotUiItem?) -> com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft?,
    ): TableTemplateState {
        val normalized = normalize(updatedSlots)
        val domainDrafts = normalized.map(toDomain)
        return base.copy(pathSlotDrafts = domainDrafts)
    }
}

