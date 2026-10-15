package com.dudoziworkshop.dzlog.ui.onboarding

import android.content.Context
import android.util.AtomicFile
import com.dudoziworkshop.dzlog.feature.table.policy.OnboardingInstallProbe
import java.io.File

/**
 * Install-local onboarding state. Files under noBackupFilesDir are not restored
 * from Android Auto Backup; onboarding therefore restarts after uninstall/reinstall,
 * while normal app updates retain the same state.
 *
 * Never use Android runtime permissions to decide if onboarding was completed.
 */
internal enum class OnboardingInstallStage { INTRO, SETUP, COMPLETE }

internal object OnboardingInstallState {
    private const val FILE_NAME = "onboarding_install_stage_v1"

    fun loadOrInitialize(context: Context): OnboardingInstallStage {
        val file = File(context.noBackupFilesDir, FILE_NAME)
        if (file.exists()) {
            return OnboardingInstallStage.valueOf(AtomicFile(file).openRead().bufferedReader().use {
                it.readText().trim()
            })
        }
        // One-time compatibility rule for people upgrading from older builds:
        // they did not have an install-local marker, and must not see onboarding again.
        val phase = if (OnboardingInstallProbe.isUnupdatedFirstInstall(context)) {
            OnboardingInstallStage.INTRO
        } else {
            OnboardingInstallStage.COMPLETE
        }
        save(context, phase)
        return phase
    }

    fun save(context: Context, phase: OnboardingInstallStage) {
        val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))
        val stream = file.startWrite()
        try {
            stream.write(phase.name.toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
}
