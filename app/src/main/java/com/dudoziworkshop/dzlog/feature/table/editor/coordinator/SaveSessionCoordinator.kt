package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import android.content.Context
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.counter.table.SaveSettingsCounterController
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.policy.TableTemplateCatalogCoordinator
import kotlinx.coroutines.flow.first

/** Production session persistence. The UI coordinator owns only saving/error state. */
internal object SaveSessionCoordinator {
    suspend fun persist(context: Context, catalog: TableTemplateCatalogCoordinator,
        items: List<SavedTableTemplate>, activeId: String?, template: TableTemplateState,
        style: TableStyleState, includePath: Boolean, includeFilename: Boolean,
        saveMode: SaveMode, padding: Int, next: Int?, usesAutoNext: Boolean, rollbackActiveId: String? = activeId): List<SavedTableTemplate> {
        require(template.rows > 0 && template.cols > 0) { "표 크기가 올바르지 않습니다." }
        require(template.cells.map { it.cellId }.distinct().size == template.cells.size) { "셀 ID가 중복됩니다." }
        require(padding in 0..6 && (next == null || next >= 1)) { "자동번호 설정이 올바르지 않습니다." }
        val previousSettings = AppSettingsStore.flow(context).first()
        val previousCounter = if (next != null) SaveSettingsCounterController.readCurrent(
            context, template, saveMode, padding, includePath, includeFilename) else null
        try {
            val updated = catalog.saveActiveSession(items, activeId, template, style)
            AppSettingsStore.setSaveSettings(context, includePath, includeFilename, saveMode, padding)
            if (next != null) SaveSettingsCounterController.applyNext(context, template, saveMode, padding,
                includePath, includeFilename, next, usesAutoNext)
            return updated
        } catch (failure: Exception) {
            // Compensating writes; deliberately not a cross-store DB transaction.
            runCatching { catalog.persist(items, rollbackActiveId) }.exceptionOrNull()?.let(failure::addSuppressed)
            runCatching { AppSettingsStore.setSaveSettings(context, previousSettings.includePathInCounterScope,
                previousSettings.includeFilenameInCounterScope, previousSettings.saveMode,
                previousSettings.counterPadding) }.exceptionOrNull()?.let(failure::addSuppressed)
            if (previousCounter != null) runCatching {
                SaveSettingsCounterController.applyNext(context, template, saveMode, padding, includePath,
                    includeFilename, previousCounter.next, previousCounter.usesAutoNext)
            }.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        }
    }
}
