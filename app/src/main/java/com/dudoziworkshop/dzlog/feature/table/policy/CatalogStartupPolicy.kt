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
 * Before runtime permissions, a verified fresh package with no saved state can
 * display informational introduction pages even if media cannot yet be inspected.
 * After permission setup, requireVerifiedMediaForOnboarding rejects any unknown
 * media result before a template can be created. Prior photos always veto the offer.
 */
fun decideCatalogStartupRoute(
    catalogJson: String?,
    legacyJson: String?,
    hasOtherSavedState: Boolean,
    verifiedNoExistingMedia: Boolean,
    verifiedFreshInstall: Boolean = false,
    knownExistingMedia: Boolean = false,
    requireVerifiedMediaForOnboarding: Boolean = false,
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
    // Existing content and package updates never trigger fresh onboarding.
    if (hasOtherSavedState || knownExistingMedia || !verifiedFreshInstall) {
        return CatalogStartupRoute.CONTINUE_EXISTING
    }
    // A newly installed package alone is insufficient proof that old photos are absent.
    // Fail closed if the post-permission MediaStore check did not complete.
    if (requireVerifiedMediaForOnboarding && !verifiedNoExistingMedia) {
        return CatalogStartupRoute.RECOVERY_REQUIRED
    }
    return CatalogStartupRoute.OFFER_ONBOARDING
}
