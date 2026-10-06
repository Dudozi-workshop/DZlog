package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_ACTIVE_TABLE_TEMPLATE_ID
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATES_JSON
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesFromJson
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesToJson
import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.feature.table.state.loadTableStyleState
import com.dudoziworkshop.dzlog.feature.table.state.persistTableStyleState
import kotlinx.coroutines.flow.first

data class TableTemplateCatalogSnapshot(
    val items: List<SavedTableTemplate>,
    val activeTemplateId: String,
)

suspend fun loadOrMigrateTableTemplateCatalog(context: Context): TableTemplateCatalogSnapshot {
    val prefs = context.dataStore.data.first()
    val catalogJson = prefs[KEY_TABLE_TEMPLATES_JSON]
    val parsedCatalog = catalogJson
        ?.takeIf { it.isNotBlank() }
        ?.let(::savedTableTemplatesFromJson)
        .orEmpty()

    if (parsedCatalog.isNotEmpty()) {
        val requestedActive = prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID]
        val activeId = requestedActive
            ?.takeIf { id -> parsedCatalog.any { it.id == id } }
            ?: parsedCatalog.maxByOrNull { it.modifiedAt }!!.id
        return TableTemplateCatalogSnapshot(
            items = parsedCatalog.sortedByDescending { it.modifiedAt },
            activeTemplateId = activeId,
        )
    }

    val legacyTemplate = prefs[KEY_TABLE_TEMPLATE_JSON]
        ?.takeIf { it.isNotBlank() }
        ?.let(::tableTemplateStateFromJson)
        ?: defaultTableTemplateState()
    val style = loadTableStyleState(context)
    val migrated = createSavedTableTemplate(
        name = "기본 템플릿",
        templateState = legacyTemplate,
        styleState = style,
    )
    persistTableTemplateCatalog(
        context = context,
        items = listOf(migrated),
        activeTemplateId = migrated.id,
    )
    return TableTemplateCatalogSnapshot(
        items = listOf(migrated),
        activeTemplateId = migrated.id,
    )
}

suspend fun persistTableTemplateCatalog(
    context: Context,
    items: List<SavedTableTemplate>,
    activeTemplateId: String,
) {
    val normalized = items.sortedByDescending { it.modifiedAt }
    val active = normalized.firstOrNull { it.id == activeTemplateId }
        ?: normalized.firstOrNull()
        ?: return

    context.dataStore.edit { prefs ->
        prefs[KEY_TABLE_TEMPLATES_JSON] = savedTableTemplatesToJson(normalized)
        prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID] = active.id
        // Legacy/current-active key remains synchronized for camera/save compatibility.
        prefs[KEY_TABLE_TEMPLATE_JSON] = active.templateState.toJsonString()
    }
    persistTableStyleState(context, active.styleState)
}

suspend fun activateSavedTableTemplate(
    context: Context,
    items: List<SavedTableTemplate>,
    activeTemplateId: String,
) {
    persistTableTemplateCatalog(
        context = context,
        items = items,
        activeTemplateId = activeTemplateId,
    )
}
