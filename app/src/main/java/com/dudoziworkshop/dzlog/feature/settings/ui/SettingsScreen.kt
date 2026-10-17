package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateMergeResult
import com.dudoziworkshop.dzlog.ui.onboarding.OnboardingFlowScreen
import com.dudoziworkshop.dzlog.feature.help.ui.DzlogHelpScreen

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    startWithHelp: Boolean = false,
    tutorialTemplate: com.dudoziworkshop.dzlog.domain.model.TableTemplateState? = null,
    tutorialStyle: com.dudoziworkshop.dzlog.feature.table.model.TableStyleState = com.dudoziworkshop.dzlog.feature.table.model.TableStyleState(),
    tutorialTemplateName: String = "",
    onOpenCredits: () -> Unit = {},
    onImported: (LocalTemplateMergeResult) -> Unit = {},
) {
    var backupMode by remember { mutableStateOf<BackupScreenMode?>(null) }
    // index -> one of five module IDs -> index; intro reuses the read-only onboarding.
    var helpRoute by rememberSaveable { mutableStateOf<String?>(if (startWithHelp) "index" else null) }
    var liveArea by rememberSaveable { mutableStateOf<String?>(null) }
    var liveSession by rememberSaveable { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore.flow(context)
        .collectAsState(initial = com.dudoziworkshop.dzlog.data.datastore.AppSettings.Default)
    if (liveArea != null && liveSession != null && tutorialTemplate != null) {
        com.dudoziworkshop.dzlog.feature.help.ui.LiveTutorialHost(
            initialAreaId = requireNotNull(liveArea), initialSessionId = requireNotNull(liveSession),
            onClose = { liveArea = null; liveSession = null },
        ) { location ->
            com.dudoziworkshop.dzlog.feature.help.ui.LiveTutorialScreens(location, tutorialTemplate,
                tutorialStyle, settings, tutorialTemplateName)
        }
    } else if (helpRoute == "intro") {
        OnboardingFlowScreen(
            busy = false,
            onChoose = {},
            onSkip = {},
            readOnly = true,
            onClose = { helpRoute = "index" },
        )
    } else if (helpRoute != null) {
        DzlogHelpScreen(
            moduleId = helpRoute?.takeUnless { it == "index" },
            onOpenModule = { helpRoute = it },
            onOpenIntroduction = { helpRoute = "intro" },
            onOpenLiveSession = { area, session -> liveArea = area; liveSession = session },
            onBack = { helpRoute = if (helpRoute == "index") null else "index" },
        )
    } else if (backupMode == null) {
        SettingsRootScreen(
            onBack = onBack,
            onOpenCredits = onOpenCredits,
            onOpenGuide = { helpRoute = "index" },
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
