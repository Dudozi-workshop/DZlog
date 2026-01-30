package com.example.dzlog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.dzlog.ui.camera.CameraScreen
import com.example.dzlog.ui.home.HomeScreen
import com.example.dzlog.ui.home.SettingsScreen
import com.example.dzlog.ui.log.SimpleLogScreen
import com.example.dzlog.ui.table.TableEditorScreen
import kotlinx.coroutines.flow.first

enum class AppScreen {
    HOME,
    CAMERA,
    TABLE_EDITOR,
    SETTINGS,
    LOGS
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
    var orientationMode by remember { mutableStateOf(OrientationMode.PORTRAIT_LOCK) }
    val tableTemplateViewModel: TableTemplateViewModel = viewModel()
    val tableTemplateState = tableTemplateViewModel.tableTemplateState

    val context = LocalContext.current
    val activity = context as? android.app.Activity

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

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            onOpenSettings = { navigateTo(AppScreen.SETTINGS) },
            onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
            onStartCamera = { navigateTo(AppScreen.CAMERA) },
            onOpenLogs = { navigateTo(AppScreen.LOGS) },
            tableTemplateState = tableTemplateState
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
            onBack = { screen = AppScreen.HOME }
        )

        AppScreen.LOGS -> SimpleLogScreen(
            onBack = { screen = AppScreen.HOME }
        )
    }
}
