@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.dudoziworkshop.dzlog.ui.home
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.debug.CounterDebugDump
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import com.dudoziworkshop.dzlog.feature.table.preview.TablePreviewCard
import com.dudoziworkshop.dzlog.feature.table.state.rememberTablePreviewSettings
import com.dudoziworkshop.dzlog.ui.common.CounterAwareFileNameText
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZCard
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.common.rememberThreeButtonNavEquivalentBottomPadding
import com.dudoziworkshop.dzlog.ui.log.DzThumbnail
import com.dudoziworkshop.dzlog.ui.log.dzFormatDate
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    val counterFacade = remember(context, settings.counterPadding) {
        CounterFacade(
            context = context,
            counterDigits = settings.counterPadding,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        )
    }

    var savePathPreview by remember { mutableStateOf("Pictures/DZlog/") }
    var filenamePreview by remember { mutableStateOf("DZlog_1.jpg") }
    var previewNow by remember { mutableStateOf(Date()) }
    // Home은 편집 화면이 아니므로 preview용 phrase cursor는 고정값으로 유지한다.
    val phraseProgressCursor = 1

    LaunchedEffect(tableTemplateState.cells, lifecycleOwner) {
        val unit = decideTickUnitFromTemplate(tableTemplateState.cells)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(computeNextDelayMillis(unit))
                previewNow = Date()
            }
        }
    }

    LaunchedEffect(
        tableTemplateState.cells,
        tableTemplateState.fileNameSlotDrafts,
        tableTemplateState.pathSlotDrafts,
        tableTemplateState.phraseSets,
        settings.saveMode,
        settings.counterPadding,
        settings.includePathInCounterScope,
        settings.includeFilenameInCounterScope,
        previewNow,
        phraseProgressCursor,
        counterFacade,
    ) {
        val now = previewNow
        val previewPipeline = buildPreview(
            PreviewInput(
                templateState = tableTemplateState,
                captureNow = now,
                counterDigits = settings.counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                includePathInCounterScope = settings.includePathInCounterScope,
                includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                saveMode = settings.saveMode,
                scopeNextCounter = 1,
                phraseProgressCursor = phraseProgressCursor,
            )
        )
        // 주요 정책: 홈 preview도 request 정규화 -> facade read 단일 경로를 사용한다.
        val counterScope = previewPipeline.previewNaming.counterScope
        val scanPrefix = previewPipeline.previewNaming.scanPrefix
        val counterRequest = CounterRequestResolver.fromHome(
            counterScope = counterScope,
            saveMode = settings.saveMode,
            scanPrefix = scanPrefix,
            includePathInScope = settings.includePathInCounterScope,
            includeFilenameInScope = settings.includeFilenameInCounterScope,
        )
        val counterRead = counterFacade.read(counterRequest)
        val streamNext = counterRead.next.coerceAtLeast(1)
        // 파일명/경로 표시도 공용 pipeline 결과를 사용하되, 사용 카운터만 streamNext로 맞춘다.
        val displayPreviewPipeline = buildPreview(
            PreviewInput(
                templateState = tableTemplateState,
                captureNow = now,
                counterDigits = settings.counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                includePathInCounterScope = settings.includePathInCounterScope,
                includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                saveMode = settings.saveMode,
                scopeNextCounter = streamNext,
                phraseProgressCursor = phraseProgressCursor,
                selectedPhraseTextByCellIdOverride = previewPipeline.selectedPhraseTextByCellId,
            )
        )
        CounterDebugDump.dump(
            tag = "HomePreview",
            context = context,
            scopedStream = counterRead.scopedStream,
            appSettings = settings,
            nextSeed = streamNext,
            note = null,
        )
        savePathPreview = displayPreviewPipeline.previewNaming.relativePath
        filenamePreview = displayPreviewPipeline.previewNaming.displayName
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
            .dzScreen()
    ) {
        val threeButtonEquivalentBottomPadding = rememberThreeButtonNavEquivalentBottomPadding()
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val horizontalPad = clampDp(screenWidth * 0.045f, 12.dp, 20.dp)
        val homeBottomGap = clampDp(screenHeight * 0.004f, 0.dp, 8.dp)
        val gap = clampDp(screenHeight * 0.012f, 6.dp, 14.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPad,
                    end = horizontalPad,
                    bottom = homeBottomGap + threeButtonEquivalentBottomPadding
                )
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
                                Box(modifier = Modifier.weight(1f)) {
                                    val valueStyle = DDZTypography.Body.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                    val displayPath = remember(savePathPreview) {
                                        savePathPreview.ifBlank { "Pictures/DZlog/" }.trimEnd('/')
                                    }
                                    Text(
                                        text = displayPath,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = valueStyle,
                                        color = DDZColor.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                                CounterAwareFileNameText(
                                    fileName = filenamePreview,
                                    modifier = Modifier.weight(1f),
                                    style = DDZTypography.Body.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = DDZColor.TextPrimary,
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
                // 홈 액션 버튼 정책: 두 버튼의 높이/라운딩을 통일해 라운드 사각형 톤을 유지한다.
                // 토큰 정책: 홈의 대표 액션 버튼 높이는 공통 Button 기준선을 사용한다.
                val unifiedActionButtonHeight = DDZLayout.Control.Button
                // 1단계 라운딩 토큰: 홈 액션 버튼은 Medium 기준선을 사용한다.
                val unifiedActionButtonShape = RoundedCornerShape(DDZLayout.Radius.Medium)

                DDZButton(
                    text = "촬영 시작",
                    onClick = onStartCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 7.dp,
                            shape = unifiedActionButtonShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.20f),
                            spotColor = Color.Black.copy(alpha = 0.20f)
                        ),
                    style = DDZButtonStyle.Primary,
                    minHeight = unifiedActionButtonHeight,
                    shape = unifiedActionButtonShape,
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
                    minHeight = unifiedActionButtonHeight,
                    shape = unifiedActionButtonShape
                )

            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 15.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(DDZLayout.Radius.Large))
                    .background(DDZColor.Card)
                    .border(1.dp, DDZColor.Border.copy(alpha = 0.95f), RoundedCornerShape(DDZLayout.Radius.Large))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "표 상세설정",
                            style = DDZTypography.HomeSectionLabel.copy(fontWeight = FontWeight.Normal),
                            color = DDZColor.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        // 주요 정책: 표 상세설정은 기존 평평한 프리뷰 구조를 유지한다.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = onOpenTableEditor)
                                .padding(2.dp)
                        ) {
                            TablePreviewCard(
                                templateState = tableTemplateState,
                                counterDigits = settings.counterPadding,
                                now = previewNow,
                                wmBgStyle = previewSettings.wmBgStyle,
                                wmBgAlpha = previewSettings.wmBgAlpha,
                                wmValueScale = previewSettings.wmValueScale,
                                wmTextColorMode = previewSettings.wmTextColorMode,
                                wmManualTextColor = previewSettings.wmManualTextColor,
                                wmTextAlign = previewSettings.wmTextAlign,
                                tableDetailGridEnabled = previewSettings.tableDetailGridEnabled,
                                wmWidthRatio = previewSettings.wmWidthRatio,
                                wmHeightRatio = previewSettings.wmHeightRatio,
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
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "최근 촬영",
                            style = DDZTypography.HomeSectionLabel.copy(fontWeight = FontWeight.Normal),
                            color = DDZColor.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(0.74f)
                                .fillMaxWidth()
                                .shadow(3.dp, RoundedCornerShape(18.dp), clip = false)
                                .clip(RoundedCornerShape(18.dp))
                                .background(DDZColor.Surface)
                                .border(1.dp, DDZColor.Primary.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                                .clickable {
                                    val it = latestImage
                                    if (it == null) {
                                        onOpenAlbum()
                                    } else {
                                        val (g1, g2) = parseG1G2FromRelativePath(it.relativePath)
                                        onOpenRecentCaptureGrid(g1, g2, it.relativePath, 0)
                                    }
                                }
                        ) {
                            val it = latestImage
                            if (it == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Collections,
                                            contentDescription = null,
                                            tint = DDZColor.TextMuted,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "최근 촬영 없음",
                                            style = DDZTypography.Caption,
                                            color = DDZColor.TextMuted
                                        )
                                    }
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
                                        style = DDZTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
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
                                .shadow(1.5.dp, RoundedCornerShape(DDZLayout.Radius.Medium), clip = false)
                                .clip(RoundedCornerShape(DDZLayout.Radius.Medium))
                                .background(DDZColor.SageLight.copy(alpha = 0.99f))
                                .border(1.dp, DDZColor.Sage.copy(alpha = 0.55f), RoundedCornerShape(DDZLayout.Radius.Medium))
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
