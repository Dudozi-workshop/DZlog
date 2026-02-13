package com.example.dzlog.ui.home
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DDZColor.Background)
            .padding(DDZSpacing.screenPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 헤더: 중앙 타이틀 + 우측 설정 아이콘
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "DZlog",
                    style = DDZTypography.ScreenTitle,
                    color = DDZColor.TextPrimary,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    text = "설정",
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable(onClick = onOpenSettings)
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(DDZSpacing.itemGap))

            DDZCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

            Spacer(Modifier.height(DDZSpacing.sectionGap))

            // 중단: 메인 액션(촬영/기존 사진 편집) 영역을 가용 공간 중앙에 배치
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                DDZButton(
                    text = "촬영 시작",
                    onClick = onStartCamera,
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Primary
                )
                Spacer(Modifier.height(DDZSpacing.itemGap))
                DDZButton(
                    text = "기존 사진 편집",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Secondary,
                    enabled = false
                )
            }

// 하단: 좌(표 전체 미리보기) / 우(최근 촬영 + 앨범)
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
                    // 홈/설정 프리뷰 UI는 TablePreviewCard로 통합
                    TablePreviewCard(
                        templateState = tableTemplateState,
                        counterDigits = 0,
                        now = Date(),
                        title = "표 미리보기",
                        wmBgStyle = previewSettings.wmBgStyle,
                        wmBgAlpha = previewSettings.wmBgAlpha,
                        wmValueScale = previewSettings.wmValueScale,
                        modifier = Modifier.fillMaxSize(),
                        // 홈 카드 영역은 높이 제약이 있으므로 프리뷰도 카드 전체를 채움
                        previewModifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.weight(0.04f))

                Column(
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
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
                        Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
                            Text("최근 촬영", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
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

                    DDZButton(
                        text = "앨범",
                        onClick = onOpenAlbum,
                        modifier = Modifier
                            .weight(0.25f, fill = true)
                            .fillMaxWidth(),
                        style = DDZButtonStyle.Secondary
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    tableTemplateStateProvider: () -> TableTemplateState,
    onBack: () -> Unit,
    onOpenTableDetail: () -> Unit
) {
    com.example.dzlog.ui.settings.SettingsRootScreen(
        tableTemplateStateProvider = tableTemplateStateProvider,
        onBack = onBack,
        onOpenTableDetail = onOpenTableDetail
    )
}

/**
 * relativePath 예: "Pictures/DZlog/G1/G2/"
 * - G1/G2가 없으면 "(기본)"으로 채움
 */
private fun parseG1G2FromRelativePath(relativePath: String): Pair<String, String> {
    val default = "(기본)"
    // 슬래시 정리
    val p = relativePath.trim()
    // DZlog 이후 경로를 뽑는다
    val idx = p.indexOf("DZlog/")
    if (idx < 0) return default to default

    val tail = p.substring(idx + "DZlog/".length).trim('/')
    if (tail.isBlank()) return default to default

    val parts = tail.split('/').filter { it.isNotBlank() }
    val g1 = parts.getOrNull(0) ?: default
    val g2 = parts.getOrNull(1) ?: default
    return g1 to g2
}
