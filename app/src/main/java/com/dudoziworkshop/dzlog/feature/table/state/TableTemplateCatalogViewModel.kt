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

    fun restoreCatalog(items: List<SavedTableTemplate>, activeId: String?) {
        templates = items.sortedByDescending { it.modifiedAt }
        activeTemplateId = activeId?.takeIf { id -> templates.any { it.id == id } }
        tableTemplateState = templates.firstOrNull { it.id == activeTemplateId }?.templateState
            ?: templates.firstOrNull()?.templateState
            ?: newBlankTableTemplateState()
    }

    fun updateStateOnly(state: TableTemplateState) {
        tableTemplateState = state
    }

    fun setCatalog(items: List<SavedTableTemplate>, activeId: String?) {
        templates = items.sortedByDescending { it.modifiedAt }
        activeTemplateId = activeId?.takeIf { id -> templates.any { it.id == id } }
        tableTemplateState = templates.firstOrNull { it.id == activeTemplateId }?.templateState
            ?: templates.firstOrNull()?.templateState
            ?: newBlankTableTemplateState()
    }

    fun activate(id: String) {
        val target = templates.firstOrNull { it.id == id } ?: return
        activeTemplateId = target.id
        tableTemplateState = target.templateState
    }

    fun updateActive(state: TableTemplateState, style: TableStyleState) {
        val id = activeTemplateId
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
