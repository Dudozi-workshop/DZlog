package com.example.dzlog.ui.settings

import android.content.pm.PackageManager
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dzlog.data.counter.scanUsedCountersFromMediaStore
import com.example.dzlog.data.counterindex.CounterIndexRepository
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.buildFileNamePrefixFromResolvedCells
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.ui.common.DDZButton
import com.example.dzlog.ui.common.DDZButtonStyle
import com.example.dzlog.ui.common.DDZCard
import com.example.dzlog.ui.common.DDZSectionHeader
import com.example.dzlog.ui.common.DZTitle
import com.example.dzlog.ui.common.TablePreviewCard
import com.example.dzlog.ui.settings.components.SegmentedControl
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.flow.first
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
    onOpenCaptureSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppSettingsStore.flow(context).collectAsState(
        initial = com.example.dzlog.data.datastore.AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            counterPadding = 0,
            resetCounterOnPathChange = true,
            toastEnabled = true,
            hapticEnabled = true,
            blankWarningEnabled = true,
        )
    )

    val templateState = tableTemplateStateProvider()

    // A) 상단 상태 요약(읽기 전용)
    val nowForPreview = remember { Date() }

    // ✅ 표 상세설정에서 저장한 "워터마크 표 배경 스타일"을 전체설정 프리뷰에도 반영
    var wmBgStyle by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        runCatching {
            val prefs = context.dataStore.data.first()
            wmBgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
        }.onFailure {
            wmBgStyle = 0
        }
    }

    val (projectPathPreview, nextFilenamePreview) = remember(templateState, settings) {
        val relPath = runCatching { buildGalleryRelativePath(templateState.cells) }.getOrElse { "" }
        val resolver = TableResolver()
        val now = nowForPreview
        val counterDigits = settings.counterPadding
        val plan = runCatching {
            resolver.plan(
                cells = templateState.cells,
                captureNow = now,
                config = TableResolver.Config(
                    counterDigits = counterDigits,
                    dateFormat = "yyyy.MM.dd",
                    timeFormat = "HHmm"
                )
            )
        }.getOrNull()

        val name = plan?.let {
            buildDisplayNameFromResolvedCells(
                resolvedCells = it.resolvedCells,
                fnDelim = "_",
                includeDate = false,
                includeTime = false,
                now = now
            )
        } ?: "DZlog"

        relPath to name
    }

    val isStorageGranted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(DDZSpacing.screenPadding)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            DZTitle(text = "설정")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back", style = DDZTypography.ButtonText) }
        }
        Spacer(Modifier.height(DDZSpacing.itemGap))

        StatusBarCard(
            projectPath = projectPathPreview,
            nextFilename = nextFilenamePreview
        )

        Spacer(Modifier.height(DDZSpacing.sectionGap))

        // B) Quick Controls
        QuickControlsCard(
            saveMode = settings.saveMode,
            continuousPreviewMode = settings.continuousPreviewMode,
            counterPadding = settings.counterPadding,
            resetCounterOnPathChange = settings.resetCounterOnPathChange,
            onSaveModeChange = { mode ->
                scope.launch {
                    AppSettingsStore.setSaveMode(context, mode)
                    if (settings.toastEnabled) Toast.makeText(context, "저장 대상: ${mode.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onContinuousPreviewModeChange = { mode ->
                scope.launch {
                    AppSettingsStore.setContinuousPreviewMode(context, mode)
                    if (settings.toastEnabled) Toast.makeText(context, "미리보기: ${mode.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onCounterPaddingChange = { digits ->
                scope.launch {
                    AppSettingsStore.setCounterPadding(context, digits)
                    if (settings.toastEnabled) {
                        val msg = if (digits == 0) "카운터 패딩: 없음" else "카운터 자릿수: $digits"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },

            onResetCounterSync = {
                scope.launch {
                    val relPathPrefix = buildGalleryRelativePath(templateState.cells)
                    val resolver = TableResolver()
                    val plan = runCatching {
                        resolver.plan(
                            cells = templateState.cells,
                            captureNow = nowForPreview,
                            config = TableResolver.Config(
                                counterDigits = settings.counterPadding,
                                dateFormat = "yyyy.MM.dd",
                                timeFormat = "HHmm"
                            )
                        )
                    }.getOrNull()
                    val prefix = plan?.let {
                        buildFileNamePrefixFromResolvedCells(
                            resolvedCells = it.resolvedCells,
                            fnDelim = "_",
                            includeDate = false,
                            includeTime = false,
                            now = nowForPreview
                        )
                    } ?: "DZlog"
                    val scanned = runCatching {
                        scanUsedCountersFromMediaStore(
                            context = context,
                            relativePathPrefix = relPathPrefix,
                            fileNamePrefix = prefix,
                            counterDigits = settings.counterPadding,
                            fnDelim = "_"
                        )
                    }.getOrNull()
                    val effective = scanned ?: emptySet()
                    // 캐시 업데이트(다음 dup 판단/동기화 기준)
                    runCatching {
                        val repo = CounterIndexRepository.getInstance(context)
                        repo.backfillPlaceholders(relPathPrefix, prefix, effective)
                    }
                    if (settings.toastEnabled) {
                        val max = effective.maxOrNull() ?: 0
                        Toast.makeText(context, "카운터 동기화 완료 (max=$max)", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onResetCounterOnPathChangeChange = { enabled ->
                scope.launch {
                    AppSettingsStore.setResetCounterOnPathChange(context, enabled)
                    if (settings.toastEnabled) Toast.makeText(context, "경로 변경 시 카운터 초기화: ${if (enabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(Modifier.height(DDZSpacing.sectionGap))

        // C) Template / Table
        DDZCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
                DDZSectionHeader(title = "Template / Table")
                Text("현재 템플릿", style = DDZTypography.Body, color = DDZColor.TextPrimary)

                val rows = templateState.rows.coerceAtLeast(1)
                val cols = templateState.cols.coerceAtLeast(1)
                cols / rows.toFloat()

                // 설정 화면도 TablePreviewCard로 통합 (크롬 없이 프리뷰만)
                TablePreviewCard(
                    templateState = templateState,
                    now = nowForPreview,
                    counterDigits = settings.counterPadding,
                    wmBgStyle = wmBgStyle,
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

        // D) Capture Settings
        DDZCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
                DDZSectionHeader(title = "Capture Settings")

                val previewLabel = when (settings.continuousPreviewMode) {
                    ContinuousPreviewMode.OFF -> "연속촬영/미리보기: Off"
                    ContinuousPreviewMode.SHORT -> "연속촬영/미리보기: Short"
                    ContinuousPreviewMode.HOLD -> "연속촬영/미리보기: Hold"
                }
                Text(previewLabel, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                DDZButton(
                    text = "촬영설정으로 이동",
                    onClick = onOpenCaptureSettings,
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Primary
                )
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
                    AppSettingsStore.setToastEnabled(context, enabled)
                    if (enabled) Toast.makeText(context, "토스트 피드백 ON", Toast.LENGTH_SHORT).show()
                }
            },
            onHapticEnabledChange = { enabled ->
                scope.launch {
                    AppSettingsStore.setHapticEnabled(context, enabled)
                    if (settings.toastEnabled != false) Toast.makeText(context, "진동: ${if (enabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                }
            },
            onBlankWarningEnabledChange = { enabled ->
                scope.launch {
                    AppSettingsStore.setBlankWarningEnabled(context, enabled)
                    if (settings.toastEnabled != false) Toast.makeText(context, "공백 경고: ${if (enabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(Modifier.height(DDZSpacing.itemGap))
    }
}

@Composable
private fun StatusBarCard(projectPath: String, nextFilename: String) {
    DDZCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
            DDZSectionHeader(title = "STATUS")
            Text("Project Path: $projectPath", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            Text("Next Filename: $nextFilename", style = DDZTypography.Body, color = DDZColor.TextPrimary)
        }
    }
}

@Composable
private fun QuickControlsCard(
    saveMode: SaveMode,
    continuousPreviewMode: ContinuousPreviewMode,
    counterPadding: Int,
    resetCounterOnPathChange: Boolean,
    onSaveModeChange: (SaveMode) -> Unit,
    onContinuousPreviewModeChange: (ContinuousPreviewMode) -> Unit,
    onCounterPaddingChange: (Int) -> Unit,
    onResetCounterSync: () -> Unit,
    onResetCounterOnPathChangeChange: (Boolean) -> Unit
) {
    DDZCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.sectionGap)) {
            DDZSectionHeader(title = "Quick Controls")

            Text("저장 대상", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            SegmentedControl(
                options = listOf("Original", "Watermark", "Both"),
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
                options = listOf("Off", "Short", "Hold"),
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
                Spacer(Modifier.height(0.dp))
                TextButton(onClick = onResetCounterSync) {
                    Text("Reset", style = DDZTypography.ButtonText)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("경로 변경 시 카운터 초기화", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                }
                Switch(checked = resetCounterOnPathChange, onCheckedChange = onResetCounterOnPathChangeChange)
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

            Divider(color = DDZColor.Border)

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
