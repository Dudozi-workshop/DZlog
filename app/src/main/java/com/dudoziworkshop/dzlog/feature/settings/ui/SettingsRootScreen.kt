package com.dudoziworkshop.dzlog.feature.settings.ui

import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val SettingsGroupShape = RoundedCornerShape(14.dp)
private val SETTINGS_QUALITY_ITEMS = listOf(
    QualityUiItem(PhotoQualityMode.SPEED, "속도 우선", "빠르게 저장하고 용량을 줄여요"),
    QualityUiItem(PhotoQualityMode.BALANCED, "균형", "속도와 화질을 균형 있게 유지해요"),
    QualityUiItem(PhotoQualityMode.QUALITY, "화질 우선", "더 선명하지만 저장이 느릴 수 있어요"),
)

private data class QualityUiItem(
    val mode: PhotoQualityMode,
    val title: String,
    val description: String,
)

@Composable
fun SettingsRootScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppSettingsStore.flow(context).collectAsState(initial = AppSettings.Default)
    var isQualityExpanded by remember { mutableStateOf(false) }
    var isQualitySaving by remember { mutableStateOf(false) }
    val version = remember(context) { buildAppVersionLabel(context) }

    fun selectQuality(item: QualityUiItem) {
        if (isQualitySaving) return
        if (settings.photoQualityMode == item.mode) {
            isQualityExpanded = false
            return
        }
        isQualitySaving = true
        scope.launch {
            try {
                applySettingsAction(context, SettingsAction.PhotoQualityModeChanged(item.mode))
                isQualityExpanded = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, "사진 품질을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                isQualitySaving = false
            }
        }
    }

    fun changeAssistShutter(enabled: Boolean) {
        scope.launch {
            try {
                applySettingsAction(context, SettingsAction.AssistShutterEnabledChanged(enabled))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Toast.makeText(context, "보조 셔터 설정을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = isQualityExpanded) { isQualityExpanded = false }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            DDZTopBar(
                title = "설정",
                onBack = {
                    if (isQualityExpanded) isQualityExpanded = false else onBack()
                },
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
                SettingsBrandHeader()

                SettingsSection(title = "앱 환경") {
                    SettingsGroup {
                        val qualityLabel = SETTINGS_QUALITY_ITEMS
                            .firstOrNull { it.mode == settings.photoQualityMode }?.title ?: "균형"
                        SettingsActionRow(
                            icon = Icons.Default.Image,
                            title = "사진 품질",
                            subtitle = "화질과 저장 성능",
                            onClick = { isQualityExpanded = !isQualityExpanded },
                            trailing = {
                                Text(
                                    text = qualityLabel,
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextSecondary,
                                )
                                Icon(
                                    imageVector = if (isQualityExpanded) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = if (isQualityExpanded) "접기" else "펼치기",
                                    tint = DDZColor.IconMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                        if (isQualityExpanded) {
                            SettingsDivider()
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                SETTINGS_QUALITY_ITEMS.forEach { item ->
                                    QualityInlineOptionRow(
                                        item = item,
                                        selected = settings.photoQualityMode == item.mode,
                                        enabled = !isQualitySaving,
                                        onClick = { selectQuality(item) },
                                    )
                                }
                            }
                        }
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Default.CameraAlt,
                            title = "플로팅 보조 셔터",
                            subtitle = "촬영 화면에 보조 버튼 표시",
                            onClick = {
                                changeAssistShutter(!settings.assistShutterEnabled)
                            },
                            trailing = {
                                Switch(
                                    checked = settings.assistShutterEnabled,
                                    onCheckedChange = ::changeAssistShutter,
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

                // Show data management only when local backup/import actually works.
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

@Composable
private fun QualityInlineOptionRow(
    item: QualityUiItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) DDZColor.SelectedSoft.copy(alpha = 0.8f) else DDZColor.Surface,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                )
                Text(
                    text = item.description,
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
            }
            Icon(
                imageVector = if (selected) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.RadioButtonUnchecked
                },
                contentDescription = if (selected) "선택됨" else null,
                tint = if (selected) DDZColor.SelectedDark else DDZColor.IconMuted,
                modifier = Modifier.size(20.dp),
            )
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
