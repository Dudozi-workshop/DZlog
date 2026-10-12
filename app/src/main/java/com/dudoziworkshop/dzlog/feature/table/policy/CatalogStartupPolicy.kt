package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesFromJson
import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import org.json.JSONArray

/**
 * This inspection never writes to DataStore. Call it BEFORE legacy catalog migration.
 * Unknown media state is deliberately not proof of a new installation.
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
 * [verifiedNoExistingMedia] may be true only when the caller has successfully checked the
 * DZlog media folders. false includes 'unknown', query failure, or previously saved photos.
 */
fun decideCatalogStartupRoute(
    catalogJson: String?,
    legacyJson: String?,
    hasOtherSavedState: Boolean,
    verifiedNoExistingMedia: Boolean,
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
    return if (hasOtherSavedState || !verifiedNoExistingMedia) {
        CatalogStartupRoute.CONTINUE_EXISTING
    } else {
        CatalogStartupRoute.OFFER_ONBOARDING
    }
}
