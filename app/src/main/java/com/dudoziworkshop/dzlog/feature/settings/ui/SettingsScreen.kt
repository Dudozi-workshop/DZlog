package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateMergeResult

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit = {},
    onImported: (LocalTemplateMergeResult) -> Unit = {},
) {
    var backupMode by remember { mutableStateOf<BackupScreenMode?>(null) }
    if (backupMode == null) {
        SettingsRootScreen(
            onBack = onBack,
            onOpenCredits = onOpenCredits,
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
