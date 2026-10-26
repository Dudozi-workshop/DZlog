package com.dudoziworkshop.dzlog.feature.table.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

class TableTemplateCatalogViewModel : ViewModel() {
    var tableTemplateState by mutableStateOf(newBlankTableTemplateState())
        private set

    var templates by mutableStateOf<List<SavedTableTemplate>>(emptyList())
        private set

    var activeTemplateId by mutableStateOf<String?>(null)
        private set

    /**
     * Every published live table change advances this revision, including
     * completed captures. An in-flight disk write may replace it only if no
     * newer table state was published since the write began.
     */
    var stateRevision: Long = 0L
        private set

    fun restoreCatalog(items: List<SavedTableTemplate>, activeId: String?) {
        val previousUse = templates.associate { it.id to it.lastUsedAt }
        val restored = items.map {
            it.copy(lastUsedAt = maxOf(it.lastUsedAt, previousUse[it.id] ?: 0L))
        }.sortedByDescending { it.modifiedAt }
        val restoredId = activeId?.takeIf { id -> restored.any { it.id == id } }
        val restoredState = restored.firstOrNull { it.id == restoredId }?.templateState
            ?: restored.firstOrNull()?.templateState
            ?: newBlankTableTemplateState()
        if (activeTemplateId != restoredId || tableTemplateState != restoredState) stateRevision++
        templates = restored
        activeTemplateId = restoredId
        tableTemplateState = restoredState
    }

    fun updateStateOnly(state: TableTemplateState) {
        if (tableTemplateState != state) stateRevision++
        tableTemplateState = state
        // Publish the current input to both camera state and template catalog
        // synchronously. Async persistence must not expose a stale list entry.
        val id = activeTemplateId ?: return
        templates = templates.map { item ->
            if (item.id == id) item.copy(templateState = state) else item
        }
    }

    /**
     * Capture commits advance the saved state, so comparing the old live value
     * with savedState is incorrect: the pre-shot value is expected to differ.
     * Only preserve live state when a newer revision appeared during the write.
     */
    fun applyPersistedCatalog(
        items: List<SavedTableTemplate>,
        savedActiveId: String?,
        savedState: TableTemplateState,
        revisionAtWriteStart: Long,
    ) {
        if (activeTemplateId != savedActiveId) return
        val hasNewerLiveState = stateRevision != revisionAtWriteStart
        val current = tableTemplateState
        val chosen = if (hasNewerLiveState && current != savedState) {
            items.map { item ->
                if (item.id == savedActiveId) item.copy(templateState = current) else item
            }
        } else items
        setCatalog(chosen, savedActiveId)
    }

    fun setCatalog(items: List<SavedTableTemplate>, activeId: String?) {
        restoreCatalog(items, activeId)
    }

    fun markUsed(id: String, timestamp: Long) {
        templates = templates.map { if (it.id == id) it.copy(lastUsedAt = maxOf(it.lastUsedAt, timestamp)) else it }
    }

    fun activate(id: String) {
        val target = templates.firstOrNull { it.id == id } ?: return
        if (activeTemplateId != target.id || tableTemplateState != target.templateState) stateRevision++
        activeTemplateId = target.id
        tableTemplateState = target.templateState
    }

    fun updateActive(state: TableTemplateState, style: TableStyleState) {
        val id = activeTemplateId
        if (tableTemplateState != state) stateRevision++
        tableTemplateState = state
        if (id == null) return
        templates = templates.map { item ->
            if (item.id == id) {
                item.copy(
                    templateState = state,
                    styleState = style,
                    modifiedAt = System.currentTimeMillis(),
                )
            } else {
                item
            }
        }.sortedByDescending { it.modifiedAt }
    }
}
