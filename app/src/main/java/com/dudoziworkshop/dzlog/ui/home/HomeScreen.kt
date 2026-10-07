@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZIconButton
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.common.rememberThreeButtonNavEquivalentBottomPadding
import com.dudoziworkshop.dzlog.ui.log.DzThumbnail
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun clampDp(value: Dp, min: Dp, max: Dp): Dp = when {
    value < min -> min
    value > max -> max
    else -> value
}

private fun formatRecentCaptureTime(dateAddedSeconds: Long): String {
    if (dateAddedSeconds <= 0L) return "-"
    val captureDate = Date(dateAddedSeconds * 1_000L)
    val todayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val isToday = todayKey.format(captureDate) == todayKey.format(Date())
    val pattern = if (isToday) "'오늘' HH:mm" else "M월 d일 HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(captureDate)
}

@Composable
fun HomeScreen(
    tableTemplateState: TableTemplateState,
    activeTemplateName: String = "기본 촬영",
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenSaveSettings: () -> Unit = onOpenTableEditor,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
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
        ),
    )

    val counterFacade = remember(context, settings.counterPadding) {
        CounterFacade(
            context = context,
            counterDigits = settings.counterPadding,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        )
    }

    var previewNow by remember { mutableStateOf(Date()) }
    var nextCounterPreview by remember { mutableStateOf(1) }
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
        val previewPipeline = buildPreview(
            PreviewInput(
                templateState = tableTemplateState,
                captureNow = previewNow,
                counterDigits = settings.counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                includePathInCounterScope = settings.includePathInCounterScope,
                includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                saveMode = settings.saveMode,
                scopeNextCounter = 1,
                phraseProgressCursor = phraseProgressCursor,
            ),
        )
        val counterRequest = CounterRequestResolver.fromHome(
            counterScope = previewPipeline.previewNaming.counterScope,
            saveMode = settings.saveMode,
            scanPrefix = previewPipeline.previewNaming.scanPrefix,
            includePathInScope = settings.includePathInCounterScope,
            includeFilenameInScope = settings.includeFilenameInCounterScope,
        )
        val counterRead = counterFacade.read(counterRequest)
        nextCounterPreview = counterRead.next.coerceAtLeast(1)

        CounterDebugDump.dump(
            tag = "HomePreview",
            context = context,
            scopedStream = counterRead.scopedStream,
            appSettings = settings,
            nextSeed = nextCounterPreview,
            note = null,
        )
    }

    var latestImage by remember { mutableStateOf<MediaImageItem?>(null) }
    LaunchedEffect(Unit) {
        latestImage = withContext(Dispatchers.IO) {
            runCatching {
                DzlogMediaStoreReader(context.contentResolver).loadLatestImage()
            }.getOrNull()
        }
    }

    val normalizedTemplateName = activeTemplateName.trim().ifBlank { "기본 촬영" }
    val nextCounterText = remember(nextCounterPreview, settings.counterPadding) {
        val raw = nextCounterPreview.toString()
        if (settings.counterPadding > 0) raw.padStart(settings.counterPadding, '0') else raw
    }

    BoxWithConstraints(modifier = Modifier.dzScreen()) {
        val bottomInset = rememberThreeButtonNavEquivalentBottomPadding()
        val horizontalPad = clampDp(maxWidth * 0.055f, 18.dp, 24.dp)
        val sectionGap = clampDp(maxHeight * 0.020f, 14.dp, 22.dp)
        val heroTopGap = clampDp(maxHeight * 0.055f, 30.dp, 52.dp)

        HomeAmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = horizontalPad,
                    end = horizontalPad,
                    bottom = 18.dp + bottomInset,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DZlog",
                    style = DDZTypography.HomeMainTitle.copy(letterSpacing = 1.1.sp),
                    color = DDZColor.PrimaryDark,
                    modifier = Modifier.weight(1f),
                )
                DDZIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "설정",
                    onClick = onOpenSettings,
                )
            }

            Spacer(Modifier.height(heroTopGap))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "현재 촬영",
                    style = DDZTypography.Caption.copy(fontWeight = FontWeight.Medium),
                    color = DDZColor.TextSecondary,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = normalizedTemplateName,
                    style = DDZTypography.ScreenTitle.copy(
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "다음 촬영 $nextCounterText",
                    style = DDZTypography.Secondary,
                    color = DDZColor.TextSecondary,
                )
            }

            Spacer(Modifier.height(24.dp))

            DDZButton(
                text = "촬영 시작",
                onClick = onStartCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                style = DDZButtonStyle.Primary,
                minHeight = 50.dp,
                shape = RoundedCornerShape(14.dp),
                textStyleOverride = DDZTypography.ButtonText.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Spacer(Modifier.height(sectionGap + 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "최근 촬영",
                    style = DDZTypography.SectionTitle,
                    color = DDZColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "앨범 보기 >",
                    style = DDZTypography.Secondary.copy(fontWeight = FontWeight.Medium),
                    color = DDZColor.PrimaryDark,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenAlbum)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }

            Spacer(Modifier.height(9.dp))

            val image = latestImage
            if (image != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .align(Alignment.CenterHorizontally),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.58f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(DDZColor.Surface)
                            .border(1.dp, DDZColor.Border, RoundedCornerShape(15.dp))
                            .clickable {
                                val (g1, g2) = parseG1G2FromRelativePath(image.relativePath)
                                onOpenRecentCaptureGrid(g1, g2, image.relativePath, 0)
                            },
                    ) {
                        DzThumbnail(image.uri.toString())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = image.displayName,
                        style = DDZTypography.Body.copy(fontWeight = FontWeight.Medium),
                        color = DDZColor.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatRecentCaptureTime(image.dateAddedSeconds),
                        style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "아직 촬영한 사진이 없어요.",
                        style = DDZTypography.Body,
                        color = DDZColor.TextPrimary,
                    )
                    Text(
                        text = "첫 기록을 남겨보세요.",
                        style = DDZTypography.Secondary,
                        color = DDZColor.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(sectionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DDZButton(
                    text = "표 상세설정",
                    leadingIcon = Icons.Default.GridView,
                    onClick = onOpenTableEditor,
                    modifier = Modifier.weight(1f),
                    style = DDZButtonStyle.Secondary,
                    minHeight = 44.dp,
                    shape = RoundedCornerShape(13.dp),
                    containerColorOverride = DDZColor.SurfaceSoft.copy(alpha = 0.68f),
                    textStyleOverride = DDZTypography.ButtonText.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                DDZButton(
                    text = "저장 설정",
                    leadingIcon = Icons.Default.Folder,
                    onClick = onOpenSaveSettings,
                    modifier = Modifier.weight(1f),
                    style = DDZButtonStyle.Secondary,
                    minHeight = 44.dp,
                    shape = RoundedCornerShape(13.dp),
                    containerColorOverride = DDZColor.SurfaceSoft.copy(alpha = 0.68f),
                    textStyleOverride = DDZTypography.ButtonText.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
