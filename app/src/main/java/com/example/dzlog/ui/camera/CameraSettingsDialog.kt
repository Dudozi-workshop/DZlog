package com.example.dzlog.ui.camera

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.dzlog.ui.common.DDZButton
import com.example.dzlog.ui.common.DDZButtonStyle
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
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
                    Text(
                        "촬영 화면에 워터마크 미리보기 표시",
                        style = DDZTypography.Body,
                        color = DDZColor.Surface
                    )
                }
                Spacer(Modifier.height(DDZSpacing.sectionGap))

                Text("연속 촬영 미리보기", style = DDZTypography.Body, color = DDZColor.Surface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ContinuousPreviewMode.entries.forEach { mode ->
                        RadioButton(
                            selected = continuousPreviewMode == mode,
                            onClick = {
                                onContinuousPreviewModeChange(mode)
                                scope.launch {
                                    context.dataStore.edit {
                                        it[KEY_CONTINUOUS_PREVIEW_MODE] = mode.v
                                    }
                                }
                            }
                        )
                        Text(mode.name, style = DDZTypography.Body, color = DDZColor.Surface)
                        Spacer(Modifier.width(8.dp))
                    }
                }

                Spacer(Modifier.height(DDZSpacing.sectionGap))
                DDZButton(
                    text = "표 편집",
                    onClick = {
                        onDismiss()
                        onOpenTableEditor()
                    },
                    style = DDZButtonStyle.Primary
                )
                Text(
                    text = "셀 속성/그룹/G1·G2 설정",
                    color = DDZColor.Surface.copy(alpha = 0.7f),
                    style = DDZTypography.Caption
                )
                Spacer(Modifier.height(DDZSpacing.screenPadding))

                Text(
                    "표 위치/크기/스타일은 표 상세설정에서 변경",
                    color = DDZColor.Surface.copy(alpha = 0.7f),
                    style = DDZTypography.Caption
                )

                Spacer(Modifier.height(DDZSpacing.sectionGap + DDZSpacing.itemGap))

                Text("촬영 비율", style = DDZTypography.Body, color = DDZColor.Surface)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (captureAspect == CaptureAspect.R3_4),
                        onClick = {
                            onCaptureAspectChange(CaptureAspect.R3_4)
                            scope.launch(Dispatchers.IO) {
                                persistCaptureAspect(
                                    context,
                                    CaptureAspect.R3_4
                                )
                            }
                        }
                    )
                    Text("3:4", style = DDZTypography.Body, color = DDZColor.Surface)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (captureAspect == CaptureAspect.R9_16),
                        onClick = {
                            onCaptureAspectChange(CaptureAspect.R9_16)
                            scope.launch(Dispatchers.IO) {
                                persistCaptureAspect(
                                    context,
                                    CaptureAspect.R9_16
                                )
                            }
                        }
                    )
                    Text("9:16", style = DDZTypography.Body, color = DDZColor.Surface)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (captureAspect == CaptureAspect.R1_1),
                        onClick = {
                            onCaptureAspectChange(CaptureAspect.R1_1)
                            scope.launch(Dispatchers.IO) {
                                persistCaptureAspect(
                                    context,
                                    CaptureAspect.R1_1
                                )
                            }
                        }
                    )
                    Text("1:1", style = DDZTypography.Body, color = DDZColor.Surface)
                }
                Spacer(Modifier.height(DDZSpacing.screenPadding))

                Text("저장 모드", style = DDZTypography.Body, color = DDZColor.Surface)
                Spacer(Modifier.height(DDZSpacing.itemGap))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = saveMode == SaveMode.WATERMARK_ONLY,
                        onClick = {
                            onSaveModeChange(SaveMode.WATERMARK_ONLY)
                            scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = 0 } }
                        }
                    )
                    Text("워터마크", style = DDZTypography.Body, color = DDZColor.Surface)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = saveMode == SaveMode.ORIGINAL_ONLY,
                        onClick = {
                            onSaveModeChange(SaveMode.ORIGINAL_ONLY)
                            scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = 2 } }
                        }
                    )
                    Text("원본", style = DDZTypography.Body, color = DDZColor.Surface)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = saveMode == SaveMode.BOTH,
                        onClick = {
                            onSaveModeChange(SaveMode.BOTH)
                            scope.launch {
                                context.dataStore.edit { prefs: MutablePreferences ->
                                    prefs[KEY_SAVE_MODE] = 1
                                }
                            }
                        }
                    )
                    Text("원본+워터마크", style = DDZTypography.Body, color = DDZColor.Surface)
                }

                Spacer(Modifier.height(DDZSpacing.sectionGap + DDZSpacing.itemGap))

                Text("카운터 자릿수", style = DDZTypography.Body, color = DDZColor.Surface)
                Text(
                    "예: 4자리면 0001",
                    color = DDZColor.Surface.copy(alpha = 0.7f),
                    style = DDZTypography.Caption
                )
                Spacer(Modifier.height(DDZSpacing.itemGap))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val next = clampCounterDigits(counterDigits - 1)
                            onCounterDigitsChange(next)
                            scope.launch {
                                context.dataStore.edit {
                                    it[KEY_COUNTER_DIGITS] = next
                                }
                            }
                        }
                    ) { Text("-", style = DDZTypography.ButtonText, color = DDZColor.Surface) }

                    Spacer(Modifier.width(DDZSpacing.sectionGap))
                    Text(
                        counterDigits.toString(),
                        style = DDZTypography.CardTitle,
                        color = DDZColor.Surface
                    )
                    Spacer(Modifier.width(DDZSpacing.sectionGap))

                    Button(
                        onClick = {
                            val next = clampCounterDigits(counterDigits + 1)
                            onCounterDigitsChange(next)
                            scope.launch {
                                context.dataStore.edit {
                                    it[KEY_COUNTER_DIGITS] = next
                                }
                            }
                        }
                    ) { Text("+", style = DDZTypography.ButtonText, color = DDZColor.Surface) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("닫기", style = DDZTypography.ButtonText)
            }
        }
    )
}
