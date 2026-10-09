package com.dudoziworkshop.dzlog.feature.settings.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.feature.settings.policy.SettingsAction
import com.dudoziworkshop.dzlog.feature.settings.policy.applySettingsAction
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.launch

private val SettingsGroupShape = RoundedCornerShape(14.dp)
private val SETTINGS_QUALITY_ITEMS = listOf(
    QualityUiItem(PhotoQualityMode.SPEED, "속도 우선", "저장 속도가 빠르고 용량이 작아요", Icons.Default.Bolt),
    QualityUiItem(PhotoQualityMode.BALANCED, "균형", "속도와 화질의 균형을 맞춰요", Icons.Default.Tune),
    QualityUiItem(PhotoQualityMode.QUALITY, "화질 우선", "더 선명하지만 저장이 느릴 수 있어요", Icons.Default.Hd),
)

private data class QualityUiItem(
    val mode: PhotoQualityMode,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

@Composable
fun SettingsRootScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppSettingsStore.flow(context).collectAsState(initial = AppSettings.Default)
    var showingQuality by rememberSaveable { mutableStateOf(false) }
    val version = remember(context) { buildAppVersionLabel(context) }

    BackHandler(enabled = showingQuality) { showingQuality = false }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            DDZTopBar(
                title = if (showingQuality) "사진 품질" else "설정",
                onBack = if (showingQuality) ({ showingQuality = false }) else onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DDZSpacing.screenPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                if (showingQuality) {
                    Text(
                        text = "사진 저장 품질을 선택하세요.",
                        style = DDZTypography.Body,
                        color = DDZColor.TextSecondary,
                    )
                    SettingsGroup {
                        SETTINGS_QUALITY_ITEMS.forEachIndexed { index, item ->
                            SettingsActionRow(
                                icon = item.icon,
                                title = item.title,
                                subtitle = item.description,
                                onClick = {
                                    scope.launch {
                                        applySettingsAction(
                                            context,
                                            SettingsAction.PhotoQualityModeChanged(item.mode),
                                        )
                                    }
                                    showingQuality = false
                                },
                                trailing = {
                                    if (settings.photoQualityMode == item.mode) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "선택됨",
                                            tint = DDZColor.SelectedDark,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                },
                            )
                            if (index != SETTINGS_QUALITY_ITEMS.lastIndex) SettingsDivider()
                        }
                    }
                } else {
                    SettingsBrandHeader()

                    SettingsSection(title = "앱 환경") {
                        SettingsGroup {
                            val qualityLabel = SETTINGS_QUALITY_ITEMS
                                .firstOrNull { it.mode == settings.photoQualityMode }?.title ?: "균형"
                            SettingsActionRow(
                                icon = Icons.Default.Image,
                                title = "사진 품질",
                                subtitle = "화질과 저장 성능",
                                onClick = { showingQuality = true },
                                trailing = {
                                    Text(
                                        text = qualityLabel,
                                        style = DDZTypography.Caption,
                                        color = DDZColor.TextSecondary,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = DDZColor.IconMuted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Default.CameraAlt,
                                title = "플로팅 보조 셔터",
                                subtitle = "촬영 화면에 보조 버튼 표시",
                                onClick = {
                                    scope.launch {
                                        applySettingsAction(
                                            context,
                                            SettingsAction.AssistShutterEnabledChanged(!settings.assistShutterEnabled),
                                        )
                                    }
                                },
                                trailing = {
                                    Switch(
                                        checked = settings.assistShutterEnabled,
                                        onCheckedChange = { enabled ->
                                            scope.launch {
                                                applySettingsAction(
                                                    context,
                                                    SettingsAction.AssistShutterEnabledChanged(enabled),
                                                )
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = DDZColor.Selected,
                                            uncheckedTrackColor = DDZColor.Border,
                                            checkedThumbColor = DDZColor.Surface,
                                            uncheckedThumbColor = DDZColor.Surface,
                                        ),
                                    )
                                },
                            )
                        }
                    }

                    // Data management appears here once local backup/import is implemented.
                    // Do not expose nonfunctional actions or alter camera/table editor settings.

                    SettingsSection(title = "정보 및 지원") {
                        SettingsGroup {
                            SettingsActionRow(
                                icon = Icons.Default.FavoriteBorder,
                                title = "도움 주신 분들",
                                subtitle = "DZlog를 함께 만들어주신 분들",
                                onClick = onOpenCredits,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = DDZColor.IconMuted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (!showingQuality) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "DZlog", style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                    Text(text = version, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SettingsBrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DDZColor.SelectedSoft.copy(alpha = 0.65f),
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = DDZColor.PrimaryDark,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = "DZlog", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
            Text(text = "일상의 기록을 더 편리하게", style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = DDZTypography.Caption,
            color = DDZColor.PrimaryDark,
            modifier = Modifier.padding(start = 4.dp),
        )
        content()
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsGroupShape,
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border.copy(alpha = 0.75f)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = DDZColor.Border.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DDZColor.PrimaryDark,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = DDZTypography.Caption,
                color = DDZColor.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

private fun buildAppVersionLabel(context: android.content.Context): String {
    val pkg = context.packageManager
    val info = runCatching { pkg.getPackageInfo(context.packageName, 0) }.getOrNull()
    val version = info?.versionName ?: "-"
    val code = if (Build.VERSION.SDK_INT >= 28) info?.longVersionCode else {
        @Suppress("DEPRECATION")
        info?.versionCode?.toLong()
    }
    return "버전 $version (${code ?: "-"})"
}
