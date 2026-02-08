package com.example.dzlog.ui.camera

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_SAVE_MODE
import com.example.dzlog.data.preferences.KEY_SHOW_WM_PREVIEW
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.preferences.persistCaptureAspect
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.ui.camera.settings.CaptureAspectSection
import com.example.dzlog.ui.camera.settings.ContinuousPreviewSection
import com.example.dzlog.ui.camera.settings.CounterDigitsSection
import com.example.dzlog.ui.camera.settings.SaveModeSection
import com.example.dzlog.ui.camera.settings.TableEditSection
import com.example.dzlog.ui.camera.settings.WatermarkPreviewToggleSection
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [CameraSettingsDialog]
 * - 목적: 촬영 화면의 설정(AlertDialog) UI를 분리해 CameraScreen을 “조립자”로 유지함
 * - 포함: 기존 AlertDialog UI + DataStore 저장 로직(현 단계에서는 그대로 유지)
 * - 제외: CameraX 바인딩/촬영 저장 로직
 */
@Composable
internal fun CameraSettingsDialog(
    context: Context,
    scope: CoroutineScope,
    showWmPreview: Boolean,
    onShowWmPreviewChange: (Boolean) -> Unit,
    continuousPreviewMode: ContinuousPreviewMode,
    onContinuousPreviewModeChange: (ContinuousPreviewMode) -> Unit,
    onOpenTableEditor: () -> Unit,
    captureAspect: CaptureAspect,
    onCaptureAspectChange: (CaptureAspect) -> Unit,
    saveMode: SaveMode,
    onSaveModeChange: (SaveMode) -> Unit,
    counterDigits: Int,
    onCounterDigitsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("설정", style = DDZTypography.ScreenTitle, color = DDZColor.Surface) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
            ) {
                WatermarkPreviewToggleSection(
                    checked = showWmPreview,
                    onCheckedChange = { checked ->
                        onShowWmPreviewChange(checked)
                        scope.launch {
                            context.dataStore.edit { prefs: MutablePreferences ->
                                prefs[KEY_SHOW_WM_PREVIEW] = if (checked) 1 else 0
                            }
                        }
                    }
                )
                Spacer(Modifier.height(DDZSpacing.sectionGap))

                ContinuousPreviewSection(
                    value = continuousPreviewMode,
                    onChange = { mode ->
                        onContinuousPreviewModeChange(mode)
                        scope.launch { context.dataStore.edit { it[KEY_CONTINUOUS_PREVIEW_MODE] = mode.v } }
                    }
                )

                TableEditSection(
                    onOpenTableEditor = {
                        onDismiss()
                        onOpenTableEditor()
                    }
                )

                CaptureAspectSection(
                    value = captureAspect,
                    onChange = { asp ->
                        onCaptureAspectChange(asp)
                        scope.launch(Dispatchers.IO) { persistCaptureAspect(context, asp) }
                    }
                )

                SaveModeSection(
                    value = saveMode,
                    onChange = { mode ->
                        onSaveModeChange(mode)
                        scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = mode.v } }
                    }
                )

                CounterDigitsSection(
                    value = counterDigits,
                    onMinus = {
                        val next = clampCounterDigits(counterDigits - 1)
                        onCounterDigitsChange(next)
                        scope.launch { context.dataStore.edit { it[KEY_COUNTER_DIGITS] = next } }
                    },
                    onPlus = {
                        val next = clampCounterDigits(counterDigits + 1)
                        onCounterDigitsChange(next)
                        scope.launch { context.dataStore.edit { it[KEY_COUNTER_DIGITS] = next } }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("닫기", style = DDZTypography.ButtonText)
            }
        }
    )
}
