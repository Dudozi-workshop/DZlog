package com.dudoziworkshop.dzlog.feature.settings.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import com.dudoziworkshop.dzlog.feature.settings.components.SegmentedControl
import com.dudoziworkshop.dzlog.feature.settings.policy.SettingsAction
import com.dudoziworkshop.dzlog.feature.settings.policy.applySettingsAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.launch

private val SETTINGS_QUALITY_ITEMS = listOf(
    QualityUiItem(
        mode = PhotoQualityMode.SPEED,
        icon = Icons.Default.Bolt,
        title = "속도 우선",
        description = "저장 속도가 빠르고 용량이 작아요"
    ),
    QualityUiItem(
        mode = PhotoQualityMode.BALANCED,
        icon = Icons.Default.Tune,
        title = "균형",
        description = "속도와 화질의 균형을 맞춰요"
    ),
    QualityUiItem(
        mode = PhotoQualityMode.QUALITY,
        icon = Icons.Default.Hd,
        title = "화질 우선",
        description = "더 선명하지만 저장이 느릴 수 있어요"
    )
)

private val SETTINGS_CAPTURE_FEEDBACK_ITEMS = listOf(
    ToggleUiItem(
        key = ToggleKey.CAPTURE_SOUND,
        icon = Icons.Default.VolumeUp,
        title = "촬영 소리",
        description = "촬영 시 셔터 사운드를 제공해요."
    ),
    ToggleUiItem(
        key = ToggleKey.CAPTURE_HAPTIC,
        icon = Icons.Default.Vibration,
        title = "촬영 진동",
        description = "촬영 시 진동 피드백을 제공해요."
    )
)

private val SETTINGS_COUNTER_ITEMS = listOf(
    ToggleUiItem(
        key = ToggleKey.INCLUDE_PATH,
        icon = Icons.Default.Folder,
        title = "저장경로에 따라 분리",
        description = "저장경로가 다르면 카운터 범위를 분리해요."
    ),
    ToggleUiItem(
        key = ToggleKey.INCLUDE_FILENAME,
        icon = Icons.Default.Sell,
        title = "파일명에 따라 분리",
        description = "파일명이 다르면 카운터 범위를 분리해요."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRootScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // 주요 정책: UI만 재배치하고 기존 DataStore 저장/복원 로직은 그대로 사용한다.
    val settings by AppSettingsStore.flow(context).collectAsState(
        initial = AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
            counterPadding = 0,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            captureHapticEnabled = true,
            captureSoundEnabled = true,
            volumeKeyAction = VolumeKeyAction.NONE,
            blankWarningEnabled = true,
        )
    )

    val appVersion = buildAppVersionLabel()

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "설정",
                        style = DDZTypography.ScreenTitle,
                        color = DDZColor.Primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = DDZColor.Primary
                        )
                    }
                },
                actions = {
                    // 헤더 우측은 의도적으로 비워서 다른 화면 패턴과 맞춘다.
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DDZColor.Background,
                    navigationIconContentColor = DDZColor.Primary,
                    titleContentColor = DDZColor.Primary,
                    actionIconContentColor = DDZColor.Primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DDZSpacing.screenPadding, vertical = DDZSpacing.itemGap)
                .padding(bottom = DDZSpacing.itemGap),
            // 주요 정책: 부모 컬럼은 섹션(블록) 단위 간격만 담당한다.
            verticalArrangement = Arrangement.spacedBy(DDZSpacing.sectionGap)
        ) {
            SectionBlock(title = "빠른 설정") {
                OptionRow(title = "저장 대상") {
                    SegmentedControl(
                        options = listOf("원본", "워터마크", "둘 다"),
                        selectedIndex = when (settings.saveMode) {
                            SaveMode.ORIGINAL_ONLY -> 0
                            SaveMode.WATERMARK_ONLY -> 1
                            SaveMode.BOTH -> 2
                        },
                        onSelect = { idx ->
                            // 저장 대상 변경 즉시 반영 + 재진입 복원은 기존 액션 파이프라인을 그대로 사용.
                            val mode = when (idx) {
                                0 -> SaveMode.ORIGINAL_ONLY
                                1 -> SaveMode.WATERMARK_ONLY
                                else -> SaveMode.BOTH
                            }
                            scope.launch { applySettingsAction(context, SettingsAction.SaveModeChanged(mode)) }
                        }
                    )
                }

                OptionRow(title = "음량키 동작") {
                    SegmentedControl(
                        options = listOf("없음", "촬영", "배율"),
                        selectedIndex = when (settings.volumeKeyAction) {
                            VolumeKeyAction.NONE -> 0
                            VolumeKeyAction.CAPTURE -> 1
                            VolumeKeyAction.ZOOM -> 2
                        },
                        onSelect = { idx ->
                            val action = when (idx) {
                                1 -> VolumeKeyAction.CAPTURE
                                2 -> VolumeKeyAction.ZOOM
                                else -> VolumeKeyAction.NONE
                            }
                            scope.launch {
                                applySettingsAction(context, SettingsAction.VolumeKeyActionChanged(action))
                            }
                        }
                    )
                }

                OptionRow(title = "연속촬영 미리보기") {
                    SegmentedControl(
                        options = listOf("없음", "짧게", "고정"),
                        selectedIndex = when (settings.continuousPreviewMode) {
                            ContinuousPreviewMode.OFF -> 0
                            ContinuousPreviewMode.SHORT -> 1
                            ContinuousPreviewMode.HOLD -> 2
                        },
                        onSelect = { idx ->
                            val mode = when (idx) {
                                0 -> ContinuousPreviewMode.OFF
                                1 -> ContinuousPreviewMode.SHORT
                                else -> ContinuousPreviewMode.HOLD
                            }
                            scope.launch { applySettingsAction(context, SettingsAction.ContinuousPreviewModeChanged(mode)) }
                        }
                    )
                }

                OptionRow(title = "카운터 패딩") {
                    SegmentedControl(
                        options = listOf("0", "2", "3", "4"),
                        selectedIndex = when (settings.counterPadding) {
                            0 -> 0
                            2 -> 1
                            3 -> 2
                            else -> 3
                        },
                        onSelect = { idx ->
                            val padding = when (idx) {
                                0 -> 0
                                1 -> 2
                                2 -> 3
                                else -> 4
                            }
                            scope.launch { applySettingsAction(context, SettingsAction.CounterPaddingChanged(padding)) }
                        }
                    )
                }
            }

            SectionBlock(title = "사진 품질") {
                SETTINGS_QUALITY_ITEMS.forEach { item ->
                    QualityOptionRow(
                        item = item,
                        selected = settings.photoQualityMode == item.mode,
                        onClick = {
                            scope.launch { applySettingsAction(context, SettingsAction.PhotoQualityModeChanged(item.mode)) }
                        }
                    )
                }
            }

            SectionBlock(title = "촬영 피드백") {
                // 주요 정책: 라벨 문자열이 아니라 key 기반으로 상태/액션을 연결해 문구 변경에도 동작이 깨지지 않게 유지한다.
                SETTINGS_CAPTURE_FEEDBACK_ITEMS.forEach { item ->
                    val checked = resolveToggleChecked(settings, item.key)
                    ToggleCardRow(
                        item = item,
                        checked = checked,
                        onToggle = {
                            scope.launch {
                                applySettingsAction(context, resolveToggleAction(item.key, !checked))
                            }
                        }
                    )
                }
            }

            SectionBlock(title = "카운터 설정") {
                SETTINGS_COUNTER_ITEMS.forEach { item ->
                    val checked = resolveToggleChecked(settings, item.key)
                    ToggleCardRow(
                        item = item,
                        checked = checked,
                        onToggle = {
                            scope.launch {
                                applySettingsAction(context, resolveToggleAction(item.key, !checked))
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            CreditsEntryRow(onClick = onOpenCredits)

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = appVersion,
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DDZSpacing.sectionGap),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    // 주요 정책: 제목-카드는 한 덩어리로 묶고(좁은 간격), 블록 간 간격은 부모 컬럼에서 더 크게 분리한다.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = DDZTypography.SectionTitle,
            color = DDZColor.Primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = DDZColor.Card,
            border = BorderStroke(1.dp, DDZColor.Border)
        ) {
            Column(
                modifier = Modifier.padding(DDZSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun CreditsEntryRow(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = DDZColor.Card,
        border = BorderStroke(1.dp, DDZColor.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DDZSpacing.cardPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VolunteerActivism,
                contentDescription = null,
                tint = DDZColor.SageDark,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "도움 주신 분들",
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = DDZColor.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = DDZTypography.Body,
            color = DDZColor.TextPrimary
        )
        content()
    }
}

private data class QualityUiItem(
    val mode: PhotoQualityMode,
    val icon: ImageVector,
    val title: String,
    val description: String
)

private enum class ToggleKey {
    CAPTURE_HAPTIC,
    CAPTURE_SOUND,
    INCLUDE_PATH,
    INCLUDE_FILENAME,
}

private data class ToggleUiItem(
    val key: ToggleKey,
    val icon: ImageVector?,
    val title: String,
    val description: String
)

private fun resolveToggleChecked(settings: AppSettings, key: ToggleKey): Boolean = when (key) {
    ToggleKey.CAPTURE_HAPTIC -> settings.captureHapticEnabled
    ToggleKey.CAPTURE_SOUND -> settings.captureSoundEnabled
    ToggleKey.INCLUDE_PATH -> settings.includePathInCounterScope
    ToggleKey.INCLUDE_FILENAME -> settings.includeFilenameInCounterScope
}

private fun resolveToggleAction(key: ToggleKey, enabled: Boolean): SettingsAction = when (key) {
    ToggleKey.CAPTURE_HAPTIC -> SettingsAction.CaptureHapticEnabledChanged(enabled)
    ToggleKey.CAPTURE_SOUND -> SettingsAction.CaptureSoundEnabledChanged(enabled)
    ToggleKey.INCLUDE_PATH -> SettingsAction.IncludePathInCounterScopeChanged(enabled)
    ToggleKey.INCLUDE_FILENAME -> SettingsAction.IncludeFilenameInCounterScopeChanged(enabled)
}

@Composable
private fun QualityOptionRow(
    item: QualityUiItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) DDZColor.SageDark else DDZColor.Border
    val selectedBg = if (selected) DDZColor.SageLight.copy(alpha = 0.28f) else DDZColor.Surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = selectedBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconSlot(
                icon = item.icon,
                selected = selected
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "선택됨",
                    tint = DDZColor.SageDark,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(18.dp)
                        .background(DDZColor.Border.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ToggleCardRow(
    item: ToggleUiItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (checked) DDZColor.SageDark else DDZColor.Border
    val selectedBg = if (checked) DDZColor.SageLight.copy(alpha = 0.28f) else DDZColor.Surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = selectedBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconSlot(
                icon = item.icon,
                selected = checked
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (checked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "선택됨",
                    tint = DDZColor.SageDark,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(18.dp)
                        .background(DDZColor.Border.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}


@Composable
private fun IconSlot(
    icon: ImageVector?,
    selected: Boolean
) {
    val tint = if (selected) DDZColor.SageDark else DDZColor.TextMuted
    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon ?: Icons.Default.ChevronRight,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun buildAppVersionLabel(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pkg = context.packageManager
    val verName = runCatching { pkg.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    val verCode = if (Build.VERSION.SDK_INT >= 28) {
        runCatching { pkg.getPackageInfo(context.packageName, 0).longVersionCode }.getOrNull()?.toString() ?: "-"
    } else {
        @Suppress("DEPRECATION")
        runCatching { pkg.getPackageInfo(context.packageName, 0).versionCode }.getOrNull()?.toString() ?: "-"
    }
    return "버전 $verName ($verCode)"
}
