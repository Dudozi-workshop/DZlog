package com.example.dzlog.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dzlog.data.preferences.KEY_ORIENTATION_MODE
import com.example.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.example.dzlog.data.preferences.OrientationMode
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.template.defaultTableTemplateState
import com.example.dzlog.data.template.tableTemplateStateFromJson
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.ui.camera.CameraScreen
import com.example.dzlog.ui.home.HomeScreen
import com.example.dzlog.ui.home.SettingsScreen
import com.example.dzlog.ui.settings.CaptureSettingsScreen
import com.example.dzlog.ui.log.LogDetailScreen
import com.example.dzlog.ui.log.LogListScreen
import com.example.dzlog.ui.table.TableEditorScreen
import kotlinx.coroutines.flow.first


enum class AppScreen {
    HOME,
    CAMERA,
    TABLE_EDITOR,
    SETTINGS,
    CAPTURE_SETTINGS,
    LOG_LIST,
    LOG_DETAIL
}

class TableTemplateViewModel : ViewModel() {
    var tableTemplateState by mutableStateOf(defaultTableTemplateState())
        private set

    fun update(state: TableTemplateState) {
        tableTemplateState = state
    }

    fun reset() {
        tableTemplateState = defaultTableTemplateState()
    }
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var previousScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedLogId by remember { mutableStateOf<Long?>(null) }
    var orientationMode by remember { mutableStateOf(OrientationMode.PORTRAIT_LOCK) }
    val tableTemplateViewModel: TableTemplateViewModel = viewModel()
    val tableTemplateState = tableTemplateViewModel.tableTemplateState

    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val appSettings by AppSettingsStore.flow(context).collectAsState(
        initial = com.example.dzlog.data.datastore.AppSettings(
            saveMode = com.example.dzlog.domain.model.SaveMode.BOTH,
            continuousPreviewMode = com.example.dzlog.domain.model.ContinuousPreviewMode.OFF,
            counterPadding = 0,
            counterSuffixEnabled = true,
            resetCounterOnPathChange = true,
            toastEnabled = true,
            hapticEnabled = true,
            blankWarningEnabled = true,
            usedCounterValuesJson = null
        )
    )

    var lastBackPressedMs by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        orientationMode = try {
            val prefs = context.dataStore.data.first()
            OrientationMode.from(prefs[KEY_ORIENTATION_MODE] ?: OrientationMode.PORTRAIT_LOCK.v)
        } catch (_: Exception) {
            OrientationMode.PORTRAIT_LOCK
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val prefs = context.dataStore.data.first()
            val json = prefs[KEY_TABLE_TEMPLATE_JSON]
            if (!json.isNullOrBlank()) {
                tableTemplateStateFromJson(json)?.let { loaded ->
                    tableTemplateViewModel.update(loaded)
                }
            }
        }
    }

    LaunchedEffect(orientationMode) {
        val a = activity ?: return@LaunchedEffect
        a.requestedOrientation = when (orientationMode) {
            OrientationMode.PORTRAIT_LOCK ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            OrientationMode.AUTO_ROTATE ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun navigateTo(target: AppScreen) {
        previousScreen = screen
        screen = target
    }

    BackHandler(enabled = true) {
        // 기본 내비게이션(화면 기준)
        when (screen) {
            AppScreen.HOME -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedMs < 1500L) {
                    activity?.finish()
                } else {
                    lastBackPressedMs = now
                    if (appSettings.toastEnabled) {
                        Toast.makeText(context, "한 번 더 누르면 종료", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            AppScreen.SETTINGS -> screen = AppScreen.HOME
            AppScreen.CAPTURE_SETTINGS -> screen = AppScreen.SETTINGS
            AppScreen.TABLE_EDITOR -> screen = previousScreen
            AppScreen.CAMERA -> screen = AppScreen.HOME

            AppScreen.LOG_LIST -> screen = AppScreen.HOME
            AppScreen.LOG_DETAIL -> screen = AppScreen.LOG_LIST
        }
    }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            tableTemplateState = tableTemplateState,
            onOpenSettings = { navigateTo(AppScreen.SETTINGS) },
            onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
            onStartCamera = { navigateTo(AppScreen.CAMERA) },
            onOpenLog = {
                selectedLogId = null
                navigateTo(AppScreen.LOG_LIST)
            },
            onOpenLogDetail = { id ->
                selectedLogId = id
                navigateTo(AppScreen.LOG_DETAIL)
            }
        )

        AppScreen.CAMERA -> {
            CameraScreen(
                onExitToHome = { navigateTo(AppScreen.HOME) },
                tableTemplateState = tableTemplateState,
                onTemplateChange = tableTemplateViewModel::update,
                onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) }
            )
        }

        AppScreen.TABLE_EDITOR -> {
            TableEditorScreen(
                templateState = tableTemplateState,
                onTemplateChange = tableTemplateViewModel::update,
                onReset = tableTemplateViewModel::reset,
                onBack = { screen = previousScreen }
            )
        }

        AppScreen.SETTINGS -> SettingsScreen(
            tableTemplateStateProvider = { tableTemplateState },
            onBack = { screen = AppScreen.HOME },
            onOpenTableDetail = { navigateTo(AppScreen.TABLE_EDITOR) },
            onOpenCaptureSettings = { navigateTo(AppScreen.CAPTURE_SETTINGS) }
        )

        AppScreen.CAPTURE_SETTINGS -> {
            CaptureSettingsScreen(onBack = { screen = AppScreen.SETTINGS })
        }

        AppScreen.LOG_LIST -> {
            LogListScreen(
                onBack = { screen = AppScreen.HOME },
                onOpenDetail = { id ->
                    selectedLogId = id
                    navigateTo(AppScreen.LOG_DETAIL)
                }
            )
        }

        AppScreen.LOG_DETAIL -> {
            val logId = selectedLogId
            if (logId == null) {
                screen = AppScreen.LOG_LIST
            } else {
                LogDetailScreen(
                    logId = logId,
                    onBack = { screen = AppScreen.LOG_LIST },
                    onDeleted = {
                        selectedLogId = null
                        screen = AppScreen.LOG_LIST
                    }
                )
            }
        }
    }
}
