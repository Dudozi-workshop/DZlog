package com.example.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.log.LogEntity
import com.example.dzlog.data.log.LogRepository
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.ui.common.DDZButton
import com.example.dzlog.ui.common.DDZButtonStyle
import com.example.dzlog.ui.common.DDZCard
import com.example.dzlog.ui.common.DisplayTablePreview
import com.example.dzlog.ui.log.DzThumbnail
import com.example.dzlog.ui.log.dzFormatDate
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun HomeScreen(
    tableTemplateState: TableTemplateState,
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenLogDetail: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val logRepository = remember { LogRepository.getInstance(context) }
    val settings by AppSettingsStore.flow(context).collectAsState(
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

    var latest by remember { mutableStateOf<LogEntity?>(null) }
    LaunchedEffect(Unit) {
        latest = withContext(Dispatchers.IO) {
            runCatching { logRepository.getLatest() }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DDZColor.Background)
            .padding(DDZSpacing.screenPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("DZlog", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
            Spacer(Modifier.height(DDZSpacing.sectionGap))

            DDZButton(
                text = "촬영 시작",
                onClick = onStartCamera,
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Primary
            )
            Spacer(Modifier.height(DDZSpacing.itemGap))
            DDZButton(
                text = "앱 내 로그",
                onClick = onOpenLog,
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Secondary
            )
            Spacer(Modifier.height(DDZSpacing.itemGap))
            DDZButton(
                text = "설정",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Secondary
            )

            Spacer(Modifier.weight(1f))

            // 하단: 6:4 (표 상세설정 / 최근 로그)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                DDZCard(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .clickable(onClick = onOpenTableEditor)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
                        Text("표 상세설정", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            DisplayTablePreview(
                                templateState = tableTemplateState,
                                counterDigits = settings.counterPadding,
                                now = Date(),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(0.04f))

                DDZCard(
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight()
                        .clickable {
                            val it = latest
                            if (it != null) {
                                onOpenLogDetail(it.id)
                            } else {
                                onOpenLog()
                            }
                        }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
                        Text("최근 로그", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
                        val it = latest
                        if (it == null) {
                            Text("최근 항목 없음", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                        } else {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)) {
                                DzThumbnail(it.imageUri)
                            }
                            Spacer(Modifier.height(DDZSpacing.itemGap))
                            Text(
                                dzFormatDate(it.createdAt / 1000L),
                                style = DDZTypography.Caption,
                                color = DDZColor.TextMuted
                            )
                            val path = it.relativePath ?: "-"
                            Text(path, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    tableTemplateStateProvider: () -> TableTemplateState,
    onBack: () -> Unit,
    onOpenTableDetail: () -> Unit,
    onOpenCaptureSettings: () -> Unit
) {
    com.example.dzlog.ui.settings.SettingsRootScreen(
        tableTemplateStateProvider = tableTemplateStateProvider,
        onBack = onBack,
        onOpenTableDetail = onOpenTableDetail,
        onOpenCaptureSettings = onOpenCaptureSettings
    )
}
