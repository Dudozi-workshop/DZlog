@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.dzlog.ui.home
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.mediastore.DzlogMediaStoreReader
import com.example.dzlog.domain.capturepolicy.CaptureContext
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.example.dzlog.domain.counter.buildCounterStreamContext
import com.example.dzlog.domain.counter.toCaptureScopedCounterStream
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.MediaImageItem
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.naming.NamingFormatDefaults
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.ui.common.DDZButton
import com.example.dzlog.ui.common.DDZButtonStyle
import com.example.dzlog.ui.common.DDZCard
import com.example.dzlog.ui.common.TablePreviewCard
import com.example.dzlog.ui.common.rememberTablePreviewSettings
import com.example.dzlog.ui.log.DzThumbnail
import com.example.dzlog.ui.log.dzFormatDate
import com.example.dzlog.ui.log.parseG1G2FromRelativePath
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

private fun clampDp(value: Dp, min: Dp, max: Dp): Dp {
    return when {
        value < min -> min
        value > max -> max
        else -> value
    }
}

@Composable
fun HomeScreen(
    tableTemplateState: TableTemplateState,
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, startIndex: Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    // ✅ 즉시 반영(Flow 구독) - 표 프리뷰 설정 묶음
    val previewSettings = rememberTablePreviewSettings()

    var savePathPreview by remember { mutableStateOf("Pictures/DZlog/") }
    var filenamePreview by remember { mutableStateOf("DZlog_1.jpg") }

    LaunchedEffect(
        tableTemplateState,
        settings.counterPadding,
        settings.includePathInCounterScope,
        settings.includeFilenameInCounterScope
    ) {
        val now = Date()
        val resolver = TableResolver()
        val plan = resolver.plan(
            cells = tableTemplateState.cells,
            captureNow = now,
            config = TableResolver.Config(
                counterDigits = settings.counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT
            )
        )
        val streamContext = buildCounterStreamContext(
            resolvedCells = plan.resolvedCells,
            nextCounter = 1,
            isManualMode = false,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
        )
        val scopedStream = toCaptureScopedCounterStream(
            streamContext = streamContext,
            includePathInScope = settings.includePathInCounterScope,
            includeFilenameInScope = settings.includeFilenameInCounterScope,
        )
        val streamNext = CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = scopedStream,
            counterDigits = settings.counterPadding,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
        ).coerceAtLeast(1)
        val preview = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = CaptureContext(
                resolvedCells = plan.resolvedCells,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                counterDigits = settings.counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            ),
            usedCounter = streamNext
        )
        savePathPreview = preview.relativePath
        filenamePreview = preview.displayName
    }

    var latestImage by remember { mutableStateOf<MediaImageItem?>(null) }
    LaunchedEffect(Unit) {
        latestImage = withContext(Dispatchers.IO) {
            val reader = DzlogMediaStoreReader(context.contentResolver)
            runCatching { reader.loadLatestImage() }.getOrNull()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DDZColor.Background)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val pad = clampDp(screenWidth * 0.045f, 12.dp, 20.dp)
        val gap = clampDp(screenHeight * 0.012f, 6.dp, 14.dp)
        val radius = clampDp(screenWidth * 0.03f, 10.dp, 18.dp)

        val headerWeight = 0.10f
        val infoWeight = 0.15f
        val mainWeight = 0.45f
        val bottomWeight = 0.30f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(headerWeight)
                    .statusBarsPadding()
                    .padding(vertical = gap / 2f),
            ) {
                Text(
                    text = "DZlog",
                    style = DDZTypography.HomeMainTitle,
                    color = DDZColor.Primary,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = DDZColor.Primary
                    )
                }
            }

            Spacer(Modifier.height(gap))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(infoWeight)
            ) {
                DDZCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onOpenSettings)
                ) {
                    Column(
                        modifier = Modifier.padding(radius * 0.55f),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        Text(
                            text = "저장경로: $savePathPreview",
                            style = DDZTypography.Body,
                            color = DDZColor.TextPrimary
                        )
                        Text(
                            text = "파일명: $filenamePreview",
                            style = DDZTypography.Body,
                            color = DDZColor.TextPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(gap))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(mainWeight)
            ) {
                val mainH = maxHeight
                val btnH = clampDp(mainH * 0.22f, 44.dp, 64.dp)

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    DDZButton(
                        text = "촬영 시작",
                        onClick = onStartCamera,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(btnH),
                        style = DDZButtonStyle.Primary
                    )
                    Spacer(Modifier.height(gap))
                    DDZButton(
                        text = "기존 사진 편집",
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(btnH),
                        style = DDZButtonStyle.Secondary,
                        enabled = false
                    )
                }
            }

            Spacer(Modifier.height(gap))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(bottomWeight)
            ) {
                val bottomH = maxHeight
                val titleH = clampDp(bottomH * 0.18f, 28.dp, 44.dp)

                Row(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DDZCard(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .clickable(onClick = onOpenTableEditor)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(titleH),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "표 상세설정",
                                    style = DDZTypography.HomeSectionLabel,
                                    color = DDZColor.TextPrimary
                                )
                            }
                            Box(modifier = Modifier.weight(1f, fill = true)) {
                                TablePreviewCard(
                                    templateState = tableTemplateState,
                                    counterDigits = 0,
                                    now = Date(),
                                    wmBgStyle = previewSettings.wmBgStyle,
                                    wmBgAlpha = previewSettings.wmBgAlpha,
                                    wmValueScale = previewSettings.wmValueScale,
                                    modifier = Modifier.fillMaxSize(),
                                    previewModifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(0.04f))

                    Column(
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        DDZCard(
                            modifier = Modifier
                                .weight(0.75f, fill = true)
                                .fillMaxWidth()
                                .clickable {
                                    val it = latestImage
                                    if (it == null) {
                                        onOpenAlbum()
                                    } else {
                                        val (g1, g2) = parseG1G2FromRelativePath(it.relativePath)
                                        onOpenRecentCaptureGrid(g1, g2, 0)
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(titleH),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        "최근 촬영",
                                        style = DDZTypography.HomeSectionLabel,
                                        color = DDZColor.TextPrimary
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(gap)
                                ) {
                                    val it = latestImage
                                    if (it == null) {
                                        Text("최근 항목 없음", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        ) {
                                            DzThumbnail(it.uri.toString())
                                        }
                                        Text(
                                            dzFormatDate(it.dateAddedSeconds),
                                            style = DDZTypography.Caption,
                                            color = DDZColor.TextMuted
                                        )
                                        Text(it.relativePath, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                                    }
                                }
                            }
                        }

                        DDZCard(
                            modifier = Modifier
                                .weight(0.25f, fill = true)
                                .fillMaxWidth()
                                .clickable(onClick = onOpenAlbum)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = "앨범",
                                    tint = DDZColor.Primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
