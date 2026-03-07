package com.dudoziworkshop.dzlog.feature.settings.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRootScreen(
    onBack: () -> Unit,
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
                .padding(horizontal = DDZSpacing.screenPadding, vertical = DDZSpacing.itemGap),
            verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
        ) {
            SectionCard(title = "빠른 설정") {
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

            SectionCard(title = "사진 품질") {
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

            SectionCard(title = "촬영 피드백") {
                ToggleOptionRow(
                    title = "촬영 진동",
                    description = "촬영 버튼 입력 시 진동 피드백을 제공해요",
                    checked = settings.captureHapticEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            applySettingsAction(context, SettingsAction.CaptureHapticEnabledChanged(enabled))
                        }
                    }
                )

                ToggleOptionRow(
                    title = "촬영 소리",
                    description = "촬영 버튼 입력 시 셔터 사운드를 재생해요",
                    checked = settings.captureSoundEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            applySettingsAction(context, SettingsAction.CaptureSoundEnabledChanged(enabled))
                        }
                    }
                )
            }

            SectionCard(title = "작업 흐름") {
                ToggleOptionRow(
                    title = "카운터 범위에 저장경로 반영",
                    description = "경로가 다르면 카운터 범위를 분리해요",
                    checked = settings.includePathInCounterScope,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            applySettingsAction(context, SettingsAction.IncludePathInCounterScopeChanged(enabled))
                        }
                    }
                )

                ToggleOptionRow(
                    title = "카운터 범위에 파일명 반영",
                    description = "파일명 패턴이 다르면 카운터 범위를 분리해요",
                    checked = settings.includeFilenameInCounterScope,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            applySettingsAction(context, SettingsAction.IncludeFilenameInCounterScopeChanged(enabled))
                        }
                    }
                )
            }

            Text(
                text = appVersion,
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
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
            Text(
                text = title,
                style = DDZTypography.SectionTitle,
                color = DDZColor.Primary
            )
            content()
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

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
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (selected) DDZColor.SageDark else DDZColor.TextMuted,
                modifier = Modifier.size(20.dp)
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
private fun ToggleOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary
            )
            Text(
                text = description,
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
