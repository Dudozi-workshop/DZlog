package com.dudoziworkshop.dzlog.feature.table.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingPermissionRoutingTest {
    private fun route(
        saved: Boolean = false,
        fresh: Boolean = true,
        mediaEmpty: Boolean = false,
        mediaExists: Boolean = false,
        strict: Boolean = false,
    ) = decideCatalogStartupRoute(
        catalogJson = null,
        legacyJson = null,
        hasOtherSavedState = saved,
        verifiedNoExistingMedia = mediaEmpty,
        verifiedFreshInstall = fresh,
        knownExistingMedia = mediaExists,
        requireVerifiedMediaForOnboarding = strict,
    )

    @Test fun newInstallMayReadIntroBeforeAccessButMustRecheck() {
        assertEquals(CatalogStartupRoute.OFFER_ONBOARDING, route())
        assertEquals(CatalogStartupRoute.RECOVERY_REQUIRED, route(strict = true))
        assertEquals(CatalogStartupRoute.OFFER_ONBOARDING, route(mediaEmpty = true, strict = true))
    }

    @Test fun oldMediaOrSavedPreferencesNeverMakeNewStarter() {
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING, route(mediaExists = true))
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING, route(mediaExists = true, strict = true))
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING, route(saved = true, strict = true))
        assertEquals(CatalogStartupRoute.CONTINUE_EXISTING, route(fresh = false, strict = true))
    }

    @Test fun unverifiedOrConflictingMediaFailsClosedAfterPermission() {
        assertEquals(CatalogStartupRoute.RECOVERY_REQUIRED, route(strict = true))
        assertEquals(
            CatalogStartupRoute.CONTINUE_EXISTING,
            route(mediaEmpty = true, mediaExists = true, strict = true),
        )
    }
}
