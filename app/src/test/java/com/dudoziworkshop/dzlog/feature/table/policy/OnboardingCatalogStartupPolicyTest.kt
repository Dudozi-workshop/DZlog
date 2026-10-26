package com.dudoziworkshop.dzlog.feature.table.policy

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import android.content.Context
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingCatalogStartupPolicyTest {
    private val validState = requireNotNull(OnboardingStarterCatalog.buildState("basic"))
    private val validCatalog = savedTableTemplatesToJson(
        listOf(createSavedTableTemplate("existing", validState, TableStyleState(), id = "existing-1"))
    )

    @Test
    fun `only a verified fresh package with no saved catalog offers starter setup`() {
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING,
            decideCatalogStartupRoute(null, null, false))
        assertEquals(CatalogStartupRoute.OFFER_ONBOARDING,
            decideCatalogStartupRoute(null, null, false, verifiedFreshInstall = true))
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING,
            decideCatalogStartupRoute(null, null, true, verifiedFreshInstall = true))
        assertEquals(CatalogStartupRoute.MIGRATE_LEGACY,
            decideCatalogStartupRoute(null, validState.toJsonString(), false, verifiedFreshInstall = true))
        assertEquals(CatalogStartupRoute.RESTORE_CATALOG,
            decideCatalogStartupRoute(validCatalog, null, false, verifiedFreshInstall = true))
    }

    @Test
    fun `shared gallery photos are not installation state and cannot suppress a new starter`() {
        // This policy deliberately takes NO media argument: photos can remain
        // in shared Pictures/DZlog when an independently installed QA app starts.
        assertEquals(CatalogStartupRoute.OFFER_ONBOARDING,
            decideCatalogStartupRoute(
                catalogJson = null,
                legacyJson = null,
                hasOtherSavedState = false,
                verifiedFreshInstall = true,
            ))
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING,
            decideCatalogStartupRoute(null, null, false, verifiedFreshInstall = false))
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING,
            decideCatalogStartupRoute(null, null, true, verifiedFreshInstall = true))
    }

    @Test
    fun `corrupt catalog and legacy data are never overwritten by onboarding`() {
        assertEquals(CatalogStartupRoute.RECOVERY_REQUIRED,
            decideCatalogStartupRoute("{broken", null, false, verifiedFreshInstall = true))
        assertEquals(CatalogStartupRoute.RECOVERY_REQUIRED,
            decideCatalogStartupRoute(null, "{broken", false, verifiedFreshInstall = true))
    }

    @Test
    fun `partially decoded and duplicate catalogs require recovery instead of overwrite`() {
        val partlyCorrupt = validCatalog.dropLast(1) + ",{}]"
        assertEquals(CatalogStartupRoute.RECOVERY_REQUIRED,
            decideCatalogStartupRoute(partlyCorrupt, null, false, verifiedFreshInstall = true))
        val duplicated = validCatalog.dropLast(1) + "," + validCatalog.drop(1)
        assertEquals(CatalogStartupRoute.RECOVERY_REQUIRED,
            decideCatalogStartupRoute(duplicated, null, false, verifiedFreshInstall = true))
    }

    @Test
    fun `initial commit creates exactly one template and repeated confirmation is idempotent`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.dataStore.edit {
            it.remove(KEY_TABLE_TEMPLATES_JSON)
            it.remove(KEY_TABLE_TEMPLATE_JSON)
            it.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
            it.remove(KEY_ONBOARDING_SETUP_COMPLETED)
        }
        try {
            val created = commitFirstOnboardingStarter(ctx, "basic", CatalogStartupRoute.OFFER_ONBOARDING)
            assertTrue(created is OnboardingStarterCommitResult.Created)
            val id = (created as OnboardingStarterCommitResult.Created).snapshot.activeTemplateId
            val again = commitFirstOnboardingStarter(ctx, "work", CatalogStartupRoute.OFFER_ONBOARDING)
            assertTrue(again is OnboardingStarterCommitResult.AlreadyCompleted)
            val prefs = ctx.dataStore.data.first()
            assertEquals(true, prefs[KEY_ONBOARDING_SETUP_COMPLETED])
            assertEquals(id, prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID])
            val decoded = requireNotNull(decodeCatalogWithoutDataLoss(prefs[KEY_TABLE_TEMPLATES_JSON]!!))
            assertEquals(1, decoded.size)
            assertEquals("기본 촬영", decoded.single().name)
            assertEquals(id, decoded.single().id)
        } finally {
            ctx.dataStore.edit {
                it.remove(KEY_TABLE_TEMPLATES_JSON)
                it.remove(KEY_TABLE_TEMPLATE_JSON)
                it.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
                it.remove(KEY_ONBOARDING_SETUP_COMPLETED)
            }
        }
    }

    @Test
    fun `corrupted stored catalog is never overwritten by a normal persist`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.dataStore.edit {
            it[KEY_TABLE_TEMPLATES_JSON] = "{corrupted"
            it.remove(KEY_TABLE_TEMPLATE_JSON)
            it.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
            it.remove(KEY_ONBOARDING_SETUP_COMPLETED)
        }
        try {
            var blocked = false
            try {
                persistTableTemplateCatalog(ctx, emptyList(), null)
            } catch (_: IllegalStateException) {
                blocked = true
            }
            assertTrue(blocked)
            assertEquals("{corrupted", ctx.dataStore.data.first()[KEY_TABLE_TEMPLATES_JSON])
        } finally {
            ctx.dataStore.edit {
                it.remove(KEY_TABLE_TEMPLATES_JSON)
                it.remove(KEY_TABLE_TEMPLATE_JSON)
                it.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
                it.remove(KEY_ONBOARDING_SETUP_COMPLETED)
            }
        }
    }

    @Test
    fun `existing legacy catalog cannot be replaced even if caller offers onboarding`() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.dataStore.edit {
            it.remove(KEY_TABLE_TEMPLATES_JSON)
            it[KEY_TABLE_TEMPLATE_JSON] = validState.toJsonString()
            it.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
            it.remove(KEY_ONBOARDING_SETUP_COMPLETED)
        }
        try {
            val result = commitFirstOnboardingStarter(ctx, "work", CatalogStartupRoute.OFFER_ONBOARDING)
            assertEquals(OnboardingStarterCommitResult.ExistingData, result)
            val prefs = ctx.dataStore.data.first()
            assertFalse(prefs[KEY_ONBOARDING_SETUP_COMPLETED] == true)
            assertEquals(validState.toJsonString().length, prefs[KEY_TABLE_TEMPLATE_JSON]!!.length)
        } finally {
            ctx.dataStore.edit {
                it.remove(KEY_TABLE_TEMPLATES_JSON)
                it.remove(KEY_TABLE_TEMPLATE_JSON)
                it.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
                it.remove(KEY_ONBOARDING_SETUP_COMPLETED)
            }
        }
    }
}
