package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateMergeResult
import com.dudoziworkshop.dzlog.ui.onboarding.OnboardingFlowScreen

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit = {},
    onImported: (LocalTemplateMergeResult) -> Unit = {},
) {
    var backupMode by remember { mutableStateOf<BackupScreenMode?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    if (showGuide) {
        OnboardingFlowScreen(
            busy = false,
            onChoose = {},
            onSkip = {},
            readOnly = true,
            onClose = { showGuide = false },
        )
    } else if (backupMode == null) {
        SettingsRootScreen(
            onBack = onBack,
            onOpenCredits = onOpenCredits,
            onOpenGuide = { showGuide = true },
            onOpenExport = { backupMode = BackupScreenMode.EXPORT },
            onOpenImport = { backupMode = BackupScreenMode.IMPORT },
        )
    } else {
        SettingsBackupScreen(
            mode = requireNotNull(backupMode),
            onBack = { backupMode = null },
            onImported = onImported,
        )
    }
}
