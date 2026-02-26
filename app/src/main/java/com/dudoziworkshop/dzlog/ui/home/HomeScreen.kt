@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.dudoziworkshop.dzlog.ui.home
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureContext
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.counter.buildCounterStreamContext
import com.dudoziworkshop.dzlog.domain.counter.toCaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZCard
import com.dudoziworkshop.dzlog.ui.common.TablePreviewCard
import com.dudoziworkshop.dzlog.ui.common.rememberTablePreviewSettings
import com.dudoziworkshop.dzlog.ui.log.DzThumbnail
import com.dudoziworkshop.dzlog.ui.log.dzFormatDate
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        initial = com.dudoziworkshop.dzlog.data.datastore.AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
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
            ),
            phraseSets = tableTemplateState.phraseSets
        )
        val streamContext = buildCounterStreamContext(
            resolvedCells = plan.resolvedCells,
            fileNameSlots = tableTemplateState.fileNameSlots,
            nextCounter = 1,
            isManualMode = false,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            includeFilenameInScope = settings.includeFilenameInCounterScope,
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
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            saveMode = settings.saveMode,
        ).coerceAtLeast(1)
        val preview = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = CaptureContext(
                resolvedCells = plan.resolvedCells,
                fileNameSlots = tableTemplateState.fileNameSlots,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                counterDigits = settings.counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
                includePathInCounterScope = settings.includePathInCounterScope,
                includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp, max = 72.dp)
                        .statusBarsPadding()
                        .padding(vertical = gap * 0.35f),
                ) {
                    Text(
                        text = "DZlog",
                        style = DDZTypography.HomeMainTitle.copy(letterSpacing = 1.6.sp),
                        color = DDZColor.Primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                    ) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "설정",
                                tint = DDZColor.Primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val labelColumnWidth = remember(textMeasurer, density) {
                    val savePathLabelWidth = with(density) {
                        textMeasurer.measure(
                            text = "저장경로",
                            style = DDZTypography.Caption
                        ).size.width.toDp()
                    }
                    val fileNameLabelWidth = with(density) {
                        textMeasurer.measure(
                            text = "파일명",
                            style = DDZTypography.Caption
                        ).size.width.toDp()
                    }
                    clampDp(maxOf(savePathLabelWidth, fileNameLabelWidth), 42.dp, 72.dp)
                }

                DDZCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(onClick = onOpenSettings),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DDZColor.Background)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = DDZColor.TextMuted,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "저장경로",
                                    modifier = Modifier.width(labelColumnWidth),
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = savePathPreview,
                                    modifier = Modifier.weight(1f),
                                    style = DDZTypography.Body.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = DDZColor.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "파일명",
                                    modifier = Modifier.width(labelColumnWidth),
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = filenamePreview,
                                    modifier = Modifier.weight(1f),
                                    style = DDZTypography.Body.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = DDZColor.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(gap + 4.dp)
            ) {
                val mainButtonShape = RoundedCornerShape(30.dp)

                DDZButton(
                    text = "촬영 시작",
                    onClick = onStartCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 7.dp,
                            shape = mainButtonShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.20f),
                            spotColor = Color.Black.copy(alpha = 0.20f)
                        ),
                    style = DDZButtonStyle.Primary,
                    minHeight = 58.dp,
                    shape = mainButtonShape,
                    containerColorOverride = DDZColor.PrimaryElevated
                )
                DDZButton(
                    text = "기존 사진 편집",
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    style = DDZButtonStyle.Secondary,
                    enabled = false,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 15.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DDZColor.Card)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "표 상세설정",
                            style = DDZTypography.HomeSectionLabel,
                            color = DDZColor.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(DDZColor.Surface)
                                .border(
                                    width = 1.dp,
                                    color = DDZColor.Primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable(onClick = onOpenTableEditor)
                                .padding(14.dp)
                        ) {
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

                    Spacer(Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "최근 촬영",
                            style = DDZTypography.HomeSectionLabel,
                            color = DDZColor.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(0.74f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(DDZColor.Surface)
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
                            val it = latestImage
                            if (it == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("최근 항목 없음", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                                }
                            } else {
                                DzThumbnail(it.uri.toString())
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    DDZColor.Primary.copy(alpha = 0.75f)
                                                )
                                            )
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = dzFormatDate(it.dateAddedSeconds),
                                        style = DDZTypography.Caption.copy(fontSize = 11.sp),
                                        color = DDZColor.Surface
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .weight(0.26f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(DDZColor.SageLight.copy(alpha = 0.45f))
                                .clickable(onClick = onOpenAlbum),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = "앨범",
                                tint = DDZColor.Sage
                            )
                        }
                    }
                }
            }
        }
    }
}
