package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OnboardingInstallStateTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val marker = File(context.noBackupFilesDir, "onboarding_install_stage_v1")

    @After fun cleanup() {
        marker.delete()
        File(marker.path + ".bak").delete()
        File(marker.path + ".new").delete()
    }

    @Test fun aUserCanResumePermissionSetupWithoutAnyPermissionGrant() {
        OnboardingInstallState.save(context, OnboardingInstallStage.SETUP)
        assertEquals(OnboardingInstallStage.SETUP, OnboardingInstallState.loadOrInitialize(context))
    }

    @Test fun aCompletedInstallStaysCompleteAcrossSubsequentLaunches() {
        OnboardingInstallState.save(context, OnboardingInstallStage.COMPLETE)
        assertEquals(OnboardingInstallStage.COMPLETE, OnboardingInstallState.loadOrInitialize(context))
    }

    @Test fun introductionCanResumeOnTheSameInstall() {
        OnboardingInstallState.save(context, OnboardingInstallStage.INTRO)
        assertEquals(OnboardingInstallStage.INTRO, OnboardingInstallState.loadOrInitialize(context))
    }
    @Test fun newInstallStartsWithIntroWithoutAskingAndroidPermissions() {
        assertEquals(
            OnboardingInstallStage.INTRO,
            OnboardingInstallState.initialStageForMissingMarker(true),
        )
    }

    @Test fun updatingExistingInstallDoesNotRestartOnboarding() {
        assertEquals(
            OnboardingInstallStage.COMPLETE,
            OnboardingInstallState.initialStageForMissingMarker(false),
        )
    }

    @Test fun interruptedAtomicWriteUsesPreviousCompletedState() {
        OnboardingInstallState.save(context, OnboardingInstallStage.COMPLETE)
        val backup = File(marker.path + ".bak")
        assertTrue(marker.renameTo(backup))
        assertEquals(OnboardingInstallStage.COMPLETE, OnboardingInstallState.loadOrInitialize(context))
    }

    @Test fun unreadableStageDoesNotGetSilentlyReplacedWithIntro() {
        marker.parentFile?.mkdirs()
        marker.writeText("UNKNOWN_STATE")
        assertTrue(runCatching { OnboardingInstallState.loadOrInitialize(context) }.isFailure)
    }

}
