package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

fun activeSavedTableTemplate(
    items: List<SavedTableTemplate>,
    activeTemplateId: String?,
): SavedTableTemplate? =
    items.firstOrNull { it.id == activeTemplateId }
        ?: items.firstOrNull()

fun activeSavedTableStyle(
    items: List<SavedTableTemplate>,
    activeTemplateId: String?,
): TableStyleState =
    activeSavedTableTemplate(items, activeTemplateId)?.styleState
        ?: TableStyleState()

fun replaceActiveSavedTableTemplate(
    items: List<SavedTableTemplate>,
    activeTemplateId: String?,
    templateState: TableTemplateState,
    styleState: TableStyleState? = null,
    modifiedAt: Long = System.currentTimeMillis(),
): List<SavedTableTemplate> {
    val target = activeSavedTableTemplate(items, activeTemplateId) ?: return items
    return items.map { item ->
        if (item.id == target.id) {
            item.copy(
                templateState = templateState,
                styleState = styleState ?: item.styleState,
                modifiedAt = modifiedAt,
            )
        } else {
            item
        }
    }.sortedByDescending { it.modifiedAt }
}
