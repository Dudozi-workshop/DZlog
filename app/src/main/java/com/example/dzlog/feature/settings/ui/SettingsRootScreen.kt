package com.example.dzlog.feature.settings.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dzlog.R
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.feature.settings.components.SegmentedControl
import com.example.dzlog.feature.settings.policy.SettingsAction
import com.example.dzlog.feature.settings.policy.applySettingsAction
import com.example.dzlog.feature.settings.policy.isStorageReadGranted
import com.example.dzlog.ui.common.DDZButton
import com.example.dzlog.ui.common.DDZButtonStyle
import com.example.dzlog.ui.common.DDZCard
import com.example.dzlog.ui.common.DDZSectionHeader
import com.example.dzlog.ui.common.TablePreviewCard
import com.example.dzlog.ui.common.rememberTablePreviewSettings
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.launch
import java.util.Date

/**
 * SettingsRootScreen (MVP)
 * - 상태 요약 + 빠른 조정 + 상세 화면 이동
 * - 값 입력(Active Values)은 여기서 하지 않는다.
 */
@Composable
fun SettingsRootScreen(
    tableTemplateStateProvider: () -> com.example.dzlog.domain.model.TableTemplateState,
    onBack: () -> Unit,
    onOpenTableDetail: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppSettingsStore.flow(context).collectAsState(
        initial = com.example.dzlog.data.datastore.AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            counterPadding = 0,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            blankWarningEnabled = true,
        )
    )

    val templateState = tableTemplateStateProvider()

    val nowForPreview = remember { Date() }
    val previewSettings = rememberTablePreviewSettings()

    val isStorageGranted = remember { isStorageReadGranted(context) }


    fun showSettingsToast(message: String) {
        if (settings.toastEnabled) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(DDZSpacing.screenPadding)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "설정",
                style = DDZTypography.ScreenTitle,
                color = DDZColor.TextPrimary
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back), style = DDZTypography.ButtonText) }
        }
        Spacer(Modifier.height(DDZSpacing.itemGap))

        Spacer(Modifier.height(DDZSpacing.sectionGap))

        // B) Quick Controls
        QuickControlsCard(
            saveMode = settings.saveMode,
            continuousPreviewMode = settings.continuousPreviewMode,
            counterPadding = settings.counterPadding,
            includePathInCounterScope = settings.includePathInCounterScope,
            includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
            onSaveModeChange = { mode ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.SaveModeChanged(mode))?.let(::showSettingsToast)
                }
            },
            onContinuousPreviewModeChange = { mode ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.ContinuousPreviewModeChanged(mode))?.let(::showSettingsToast)
                }
            },
            onCounterPaddingChange = { digits ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.CounterPaddingChanged(digits))?.let(::showSettingsToast)
                }
            },
            onIncludePathInCounterScopeChange = { enabled ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.IncludePathInCounterScopeChanged(enabled))?.let(::showSettingsToast)
                }
            },
            onIncludeFilenameInCounterScopeChange = { enabled ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.IncludeFilenameInCounterScopeChanged(enabled))?.let(::showSettingsToast)
                }
            }
        )

        Spacer(Modifier.height(DDZSpacing.sectionGap))

        // C) Template / Table
        DDZCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
                DDZSectionHeader(title = "Template / Table")
                Text("현재 템플릿", style = DDZTypography.Body, color = DDZColor.TextPrimary)

                // 설정 화면도 TablePreviewCard로 통합 (크롬 없이 프리뷰만)
                TablePreviewCard(
                    templateState = templateState,
                    now = nowForPreview,
                    counterDigits = settings.counterPadding,
                    wmBgStyle = previewSettings.wmBgStyle,
                    wmBgAlpha = previewSettings.wmBgAlpha,
                    wmValueScale = previewSettings.wmValueScale,
                    modifier = Modifier.fillMaxWidth(),
                    chrome = false,
                    // 설정 화면은 높이 제약이 없는 스크롤 컬럼이므로 프리뷰 높이를 명시
                    // 전체설정: 표가 영역을 꽉 채우도록(불필요한 여백 제거)
                    previewModifier = Modifier
                        .heightIn(min = 96.dp, max = 200.dp)
                        .fillMaxWidth()
                )
                DDZButton(
                    text = "표 상세설정으로 이동",
                    onClick = onOpenTableDetail,
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Primary
                )
                // 템플릿 변경 버튼은 아직 구현이 없으므로 숨김(MVP)
            }
        }

        Spacer(Modifier.height(DDZSpacing.sectionGap))

        // E) System & App
        SystemAppCard(
            toastEnabled = settings.toastEnabled,
            hapticEnabled = settings.hapticEnabled,
            blankWarningEnabled = settings.blankWarningEnabled,
            isStorageGranted = isStorageGranted,
            onToastEnabledChange = { enabled ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.ToastEnabledChanged(enabled))?.let(::showSettingsToast)
                }
            },
            onHapticEnabledChange = { enabled ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.HapticEnabledChanged(enabled))?.let(::showSettingsToast)
                }
            },
            onBlankWarningEnabledChange = { enabled ->
                scope.launch {
                    applySettingsAction(context, SettingsAction.BlankWarningEnabledChanged(enabled))?.let(::showSettingsToast)
                }
            }
        )

        Spacer(Modifier.height(DDZSpacing.itemGap))
    }
}

@Composable
private fun QuickControlsCard(
    saveMode: SaveMode,
    continuousPreviewMode: ContinuousPreviewMode,
    counterPadding: Int,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    onSaveModeChange: (SaveMode) -> Unit,
    onContinuousPreviewModeChange: (ContinuousPreviewMode) -> Unit,
    onCounterPaddingChange: (Int) -> Unit,
    onIncludePathInCounterScopeChange: (Boolean) -> Unit,
    onIncludeFilenameInCounterScopeChange: (Boolean) -> Unit
) {
    DDZCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.sectionGap)) {
            DDZSectionHeader(title = "Quick Controls")

            Text("저장 대상", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            SegmentedControl(
                options = listOf("Original", "Watermark", stringResource(R.string.option_both)),
                selectedIndex = when (saveMode) {
                    SaveMode.ORIGINAL_ONLY -> 0
                    SaveMode.WATERMARK_ONLY -> 1
                    SaveMode.BOTH -> 2
                },
                onSelect = { idx ->
                    val m = when (idx) {
                        0 -> SaveMode.ORIGINAL_ONLY
                        1 -> SaveMode.WATERMARK_ONLY
                        else -> SaveMode.BOTH
                    }
                    onSaveModeChange(m)
                }
            )

            Text("연속 촬영 미리보기", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            SegmentedControl(
                options = listOf("Off", "Short", stringResource(R.string.option_hold)),
                selectedIndex = when (continuousPreviewMode) {
                    ContinuousPreviewMode.OFF -> 0
                    ContinuousPreviewMode.SHORT -> 1
                    ContinuousPreviewMode.HOLD -> 2
                },
                onSelect = { idx ->
                    val m = when (idx) {
                        0 -> ContinuousPreviewMode.OFF
                        1 -> ContinuousPreviewMode.SHORT
                        else -> ContinuousPreviewMode.HOLD
                    }
                    onContinuousPreviewModeChange(m)
                }
            )
            // MVP 정책: 카운터 자동부착 ON 고정 → UI 제거

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("카운터 패딩", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                    SegmentedControl(
                        options = listOf("0", "2", "3", "4"),
                        selectedIndex = when (counterPadding) {
                            0 -> 0
                            2 -> 1
                            3 -> 2
                            else -> 3
                        },
                        onSelect = { idx ->
                            val d = when (idx) {
                                0 -> 0
                                1 -> 2
                                2 -> 3
                                else -> 4
                            }
                            onCounterPaddingChange(d)
                        }
                    )
                }
            }

            Text(
                "카운터 범위(scope) 기준: 저장경로/파일명 반영 조합",
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("카운터 범위에 저장경로 반영", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                }
                Switch(checked = includePathInCounterScope, onCheckedChange = onIncludePathInCounterScopeChange)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("카운터 범위에 파일명 반영", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                }
                Switch(checked = includeFilenameInCounterScope, onCheckedChange = onIncludeFilenameInCounterScopeChange)
            }
        }
    }
}

@Composable
private fun SystemAppCard(
    toastEnabled: Boolean,
    hapticEnabled: Boolean,
    blankWarningEnabled: Boolean,
    isStorageGranted: Boolean,
    onToastEnabledChange: (Boolean) -> Unit,
    onHapticEnabledChange: (Boolean) -> Unit,
    onBlankWarningEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val pkg = context.packageManager
    val verName = runCatching { pkg.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    val verCode = if (Build.VERSION.SDK_INT >= 28) {
        runCatching { pkg.getPackageInfo(context.packageName, 0).longVersionCode }.getOrNull()?.toString() ?: "-"
    } else {
        @Suppress("DEPRECATION")
        runCatching { pkg.getPackageInfo(context.packageName, 0).versionCode }.getOrNull()?.toString() ?: "-"
    }

    DDZCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.sectionGap)) {
            DDZSectionHeader(title = "System & App")

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { Text("토스트 피드백", style = DDZTypography.Body, color = DDZColor.TextPrimary) }
                Switch(checked = toastEnabled, onCheckedChange = onToastEnabledChange)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { Text("진동", style = DDZTypography.Body, color = DDZColor.TextPrimary) }
                Switch(checked = hapticEnabled, onCheckedChange = onHapticEnabledChange)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { Text("공백 경고", style = DDZTypography.Body, color = DDZColor.TextPrimary) }
                Switch(checked = blankWarningEnabled, onCheckedChange = onBlankWarningEnabledChange)
            }

            HorizontalDivider(color = DDZColor.Border)

            Text(
                "저장 권한: ${if (isStorageGranted) "OK" else "NOT GRANTED"}",
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary
            )
            Text(
                "앱 버전: $verName ($verCode)",
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary
            )
        }
    }
}
