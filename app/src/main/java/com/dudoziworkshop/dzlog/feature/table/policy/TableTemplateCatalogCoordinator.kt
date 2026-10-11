package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

class TableTemplateCatalogCoordinator(
    private val context: Context,
) {
    suspend fun markUsed(id: String, timestamp: Long) = markSavedTableTemplateUsed(context, id, timestamp)

    suspend fun load(): TableTemplateCatalogSnapshot =
        loadOrMigrateTableTemplateCatalog(context)

    suspend fun persist(
        items: List<SavedTableTemplate>,
        activeTemplateId: String?,
    ) {
        persistTableTemplateCatalog(
            context = context,
            items = items,
            activeTemplateId = activeTemplateId,
        )
    }

    suspend fun activate(
        items: List<SavedTableTemplate>,
        activeTemplateId: String,
    ) {
        activateSavedTableTemplate(
            context = context,
            items = items,
            activeTemplateId = activeTemplateId,
        )
    }

    suspend fun updateActiveTemplate(
        items: List<SavedTableTemplate>,
        activeTemplateId: String?,
        templateState: TableTemplateState,
    ): List<SavedTableTemplate> {
        val next = replaceActiveSavedTableTemplate(
            items = items,
            activeTemplateId = activeTemplateId,
            templateState = templateState,
        )
        persist(next, activeTemplateId)
        return next
    }

    suspend fun saveActiveSession(
        items: List<SavedTableTemplate>,
        activeTemplateId: String?,
        templateState: TableTemplateState,
        styleState: TableStyleState,
    ): List<SavedTableTemplate> {
        val next = replaceActiveSavedTableTemplate(
            items = items,
            activeTemplateId = activeTemplateId,
            templateState = templateState,
            styleState = styleState,
        )
        persist(next, activeTemplateId)
        return next
    }
}
