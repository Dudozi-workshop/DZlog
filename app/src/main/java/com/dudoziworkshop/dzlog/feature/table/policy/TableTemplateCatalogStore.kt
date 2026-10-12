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
    val activeTemplateId: String?,
)

/** Read-only startup decision for the future onboarding route; no template is created here. */
suspend fun inspectTableTemplateCatalogStartup(
    context: Context,
    verifiedNoExistingMedia: Boolean,
    verifiedFreshInstall: Boolean = false,
    knownExistingMedia: Boolean = false,
): CatalogStartupRoute {
    val prefs = context.dataStore.data.first()
    val catalog = prefs[KEY_TABLE_TEMPLATES_JSON]
    val legacy = prefs[KEY_TABLE_TEMPLATE_JSON]
    return decideCatalogStartupRoute(
        catalogJson = catalog,
        legacyJson = legacy,
        hasOtherSavedState = prefs.asMap().keys.any {
            it != KEY_TABLE_TEMPLATES_JSON && it != KEY_TABLE_TEMPLATE_JSON
        },
        verifiedNoExistingMedia = verifiedNoExistingMedia,
        verifiedFreshInstall = verifiedFreshInstall,
        knownExistingMedia = knownExistingMedia,
    )
}

suspend fun loadOrMigrateTableTemplateCatalog(context: Context): TableTemplateCatalogSnapshot {
    val prefs = context.dataStore.data.first()
    val catalogJson = prefs[KEY_TABLE_TEMPLATES_JSON]
    if (catalogJson != null) {
        val parsedCatalog = decodeCatalogWithoutDataLoss(catalogJson)
            ?: error("Stored table catalog is unreadable. Refusing to overwrite it.")
        if (parsedCatalog.isEmpty()) {
            return TableTemplateCatalogSnapshot(items = emptyList(), activeTemplateId = null)
        }
        val requestedActive = prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID]
        val activeId = requestedActive
            ?.takeIf { id -> parsedCatalog.any { it.id == id } }
            ?: parsedCatalog.maxByOrNull { it.modifiedAt }!!.id
        return TableTemplateCatalogSnapshot(
            items = parsedCatalog.sortedByDescending { it.modifiedAt },
            activeTemplateId = activeId,
        )
    }

    // Never turn an unreadable saved legacy template into a newly created default.
    val legacyJson = prefs[KEY_TABLE_TEMPLATE_JSON]
    val legacyTemplate = if (legacyJson == null) defaultTableTemplateState()
        else tableTemplateStateFromJson(legacyJson)
            ?: error("Stored legacy template is unreadable. Refusing to overwrite it.")
    val style = loadTableStyleState(context)
    val migrated = createSavedTableTemplate(
        name = "기본 템플릿",
        templateState = legacyTemplate,
        styleState = style,
    )

    // Only migrate when the keys still match the originally inspected state.
    // An onboarding confirmation or concurrent restore must not be overwritten.
    var didMigrate = false
    context.dataStore.edit { current ->
        if (current[KEY_TABLE_TEMPLATES_JSON] != null ||
            current[KEY_TABLE_TEMPLATE_JSON] != legacyJson) return@edit
        current[KEY_TABLE_TEMPLATES_JSON] = savedTableTemplatesToJson(listOf(migrated))
        current[KEY_ACTIVE_TABLE_TEMPLATE_ID] = migrated.id
        current[KEY_TABLE_TEMPLATE_JSON] = migrated.templateState.toJsonString()
        didMigrate = true
    }
    if (!didMigrate) return loadOrMigrateTableTemplateCatalog(context)
    persistTableStyleState(context, style)
    return TableTemplateCatalogSnapshot(items = listOf(migrated), activeTemplateId = migrated.id)
}

suspend fun persistTableTemplateCatalog(
    context: Context,
    items: List<SavedTableTemplate>,
    activeTemplateId: String?,
) {
    val normalized = items.sortedByDescending { it.modifiedAt }
    val active = normalized.firstOrNull { it.id == activeTemplateId }
        ?: normalized.firstOrNull()

    context.dataStore.edit { prefs ->
        // Capture completion may race an editor/counter update. Preserve newer usage metadata atomically.
        val previousUse = prefs[KEY_TABLE_TEMPLATES_JSON]
            ?.let { existing ->
                decodeCatalogWithoutDataLoss(existing)
                    ?: error("Stored table catalog is unreadable. Refusing to replace it.")
            }
            .orEmpty().associate { it.id to it.lastUsedAt }
        prefs[KEY_TABLE_TEMPLATES_JSON] = savedTableTemplatesToJson(normalized.map {
            it.copy(lastUsedAt = maxOf(it.lastUsedAt, previousUse[it.id] ?: 0L))
        })
        if (active == null) {
            prefs.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
            prefs.remove(KEY_TABLE_TEMPLATE_JSON)
        } else {
            prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID] = active.id
            // Legacy/current-active key remains synchronized for camera/save compatibility.
            prefs[KEY_TABLE_TEMPLATE_JSON] = active.templateState.toJsonString()
        }
    }
    if (active != null) {
        persistTableStyleState(context, active.styleState)
    }
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

suspend fun markSavedTableTemplateUsed(context: Context, id: String, timestamp: Long) {
    context.dataStore.edit { prefs ->
        val items = prefs[KEY_TABLE_TEMPLATES_JSON]?.let(::savedTableTemplatesFromJson) ?: return@edit
        if (items.none { it.id == id }) return@edit
        prefs[KEY_TABLE_TEMPLATES_JSON] = savedTableTemplatesToJson(items.map {
            if (it.id == id) it.copy(lastUsedAt = maxOf(it.lastUsedAt, timestamp)) else it
        })
    }
}
