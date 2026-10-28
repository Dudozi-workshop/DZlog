package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesFromJson
import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import org.json.JSONArray

/**
 * This inspection never writes to DataStore. Call it BEFORE legacy catalog migration.
 * Only this package's persisted catalog/setup state is proof of an existing install.
 */
enum class CatalogStartupRoute {
    RESTORE_CATALOG,
    MIGRATE_LEGACY,
    CONTINUE_EXISTING,
    OFFER_ONBOARDING,
    RECOVERY_REQUIRED,
}

/** A malformed or partially decoded catalog must never be silently replaced. */
internal fun decodeCatalogWithoutDataLoss(json: String): List<SavedTableTemplate>? {
    val array = runCatching { JSONArray(json) }.getOrNull() ?: return null
    val decoded = savedTableTemplatesFromJson(json) ?: return null
    if (decoded.size != array.length()) return null
    if (decoded.map { it.id }.toSet().size != decoded.size) return null
    return decoded
}

/**
 * Shared MediaStore photos can belong to a previously installed package.
 * Do not let them suppress setup for a verified fresh app installation.
 * Saved/invalid template catalogs and persisted setup flags always take priority.
 */
fun decideCatalogStartupRoute(
    catalogJson: String?,
    legacyJson: String?,
    hasOtherSavedState: Boolean,
    verifiedFreshInstall: Boolean = false,
): CatalogStartupRoute {
    if (catalogJson != null) {
        return if (decodeCatalogWithoutDataLoss(catalogJson) != null) {
            CatalogStartupRoute.RESTORE_CATALOG
        } else {
            CatalogStartupRoute.RECOVERY_REQUIRED
        }
    }
    if (legacyJson != null) {
        return if (tableTemplateStateFromJson(legacyJson) != null) {
            CatalogStartupRoute.MIGRATE_LEGACY
        } else {
            CatalogStartupRoute.RECOVERY_REQUIRED
        }
    }
    // Saved state or an updated package vetoes fresh onboarding.
    // The contents of shared Pictures/DZlog are intentionally irrelevant.
    if (hasOtherSavedState || !verifiedFreshInstall) {
        return CatalogStartupRoute.CONTINUE_EXISTING
    }
    return CatalogStartupRoute.OFFER_ONBOARDING
}
