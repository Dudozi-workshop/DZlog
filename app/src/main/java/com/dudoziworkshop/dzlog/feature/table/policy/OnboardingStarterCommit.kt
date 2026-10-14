package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_ACTIVE_TABLE_TEMPLATE_ID
import com.dudoziworkshop.dzlog.data.preferences.KEY_ONBOARDING_SETUP_COMPLETED
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATES_JSON
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesToJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

sealed interface OnboardingStarterCommitResult {
    data class Created(val snapshot: TableTemplateCatalogSnapshot) : OnboardingStarterCommitResult
    data class AlreadyCompleted(val snapshot: TableTemplateCatalogSnapshot) : OnboardingStarterCommitResult
    data object ExistingData : OnboardingStarterCommitResult
    data object InvalidStarter : OnboardingStarterCommitResult
    data object NotEligible : OnboardingStarterCommitResult
}

/**
 * Called ONLY after explicit ON-03 confirmation or the user's Skip action.
 * The startup route must be established before legacy migration, with an actual media check.
 * Selection and the setup-complete flag are persisted in one DataStore transaction.
 */
suspend fun commitFirstOnboardingStarter(
    context: Context,
    starterId: String,
    startupRoute: CatalogStartupRoute,
): OnboardingStarterCommitResult {
    if (startupRoute != CatalogStartupRoute.OFFER_ONBOARDING) {
        return OnboardingStarterCommitResult.NotEligible
    }
    val definition = OnboardingStarterCatalog.find(starterId)
        ?: return OnboardingStarterCommitResult.InvalidStarter
    val state = OnboardingStarterCatalog.buildState(starterId)
        ?: return OnboardingStarterCommitResult.InvalidStarter
    val item = createSavedTableTemplate(
        name = definition.title,
        templateState = state,
        styleState = TableStyleState(),
    )
    var outcome: OnboardingStarterCommitResult = OnboardingStarterCommitResult.ExistingData
    context.dataStore.edit { prefs ->
        val catalogJson = prefs[KEY_TABLE_TEMPLATES_JSON]
        val legacyJson = prefs[KEY_TABLE_TEMPLATE_JSON]
        val activeId = prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID]
        if (catalogJson != null || legacyJson != null || activeId != null) {
            // A repeated tap must not generate a second ID or overwrite an existing template.
            val existing = catalogJson?.let(::decodeCatalogWithoutDataLoss)
            if (prefs[KEY_ONBOARDING_SETUP_COMPLETED] == true &&
                existing?.size == 1 && existing.single().id == activeId) {
                outcome = OnboardingStarterCommitResult.AlreadyCompleted(
                    TableTemplateCatalogSnapshot(existing, activeId)
                )
            }
            return@edit
        }
        if (prefs[KEY_ONBOARDING_SETUP_COMPLETED] == true) return@edit

        prefs[KEY_TABLE_TEMPLATES_JSON] = savedTableTemplatesToJson(listOf(item))
        prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID] = item.id
        // Keep old camera and save consumers in sync with the new active template.
        prefs[KEY_TABLE_TEMPLATE_JSON] = state.toJsonString()
        prefs[KEY_ONBOARDING_SETUP_COMPLETED] = true
        outcome = OnboardingStarterCommitResult.Created(
            TableTemplateCatalogSnapshot(listOf(item), item.id)
        )
    }
    return outcome
}
