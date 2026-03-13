package com.dudoziworkshop.dzlog.ui.camera

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.feature.capture.permission.hasCameraPermission
import com.dudoziworkshop.dzlog.feature.counter.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.CounterRequestResolver
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * CameraScreenV2
 * - 기존 CameraScreen을 대체하지 않는 병행 검증용 화면이다.
 * - 실제 카메라 UI/촬영 흐름은 CameraScreen을 그대로 재사용한다.
 * - V2는 그 위에 새 counter probe를 얹어 완료 이벤트 기반 동기화를 검증한다
 *   (detect -> read -> decide/apply).
 */
@Composable
fun CameraScreenV2(
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    sessionCaptureStack: SnapshotStateList<List<Uri>>,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Text("카메라 권한이 필요합니다.")
        return
    }

    val appSettings by AppSettingsStore.flow(context).collectAsState(
        initial = AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
            counterPadding = 3,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            captureHapticEnabled = true,
            captureSoundEnabled = true,
            volumeKeyAction = com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction.NONE,
            blankWarningEnabled = true,
        )
    )

    val counterFacade = remember(context, appSettings.counterPadding) {
        CounterFacade(
            context = context,
            counterDigits = appSettings.counterPadding,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        )
    }

    var displayedNext by remember { mutableIntStateOf(1) }
    var previousSaveMode by remember { mutableStateOf<SaveMode?>(null) }
    var previousRequestKey by remember { mutableStateOf<String?>(null) }
    var isInitial by remember { mutableStateOf(true) }

    // edge-trigger 상태: sticky event 방지용
    var resumeTick by remember { mutableIntStateOf(0) }
    var lastHandledResumeTick by remember { mutableIntStateOf(-1) }
    var counterEventTick by remember { mutableIntStateOf(0) }
    var lastHandledCounterEventTick by remember { mutableIntStateOf(-1) }
    var latestCounterEvent by remember { mutableStateOf<CameraCounterSyncEvent?>(null) }

    val captureCount = sessionCaptureStack.sumOf { it.size }

    // lifecycle resume은 일회성 이벤트로 처리되도록 tick 기반 edge만 사용한다.
    LaunchedEffect(Unit) { resumeTick += 1 }

    val previewPipeline = buildPreview(
        PreviewInput(
            templateState = tableTemplateState,
            captureNow = java.util.Date(),
            counterDigits = appSettings.counterPadding,
            dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
            timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            includePathInCounterScope = appSettings.includePathInCounterScope,
            includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
            saveMode = appSettings.saveMode,
            scopeNextCounter = displayedNext,
            phraseProgressCursor = 1,
        )
    )

    val request = CounterRequestResolver.fromCamera(
        saveMode = appSettings.saveMode,
        relativePathKey = previewPipeline.previewNaming.counterScope.relativePathKey,
        prefix = previewPipeline.previewNaming.counterScope.streamPrefix,
        scanPrefix = previewPipeline.previewNaming.scanPrefix,
        includePathInScope = appSettings.includePathInCounterScope,
        includeFilenameInScope = appSettings.includeFilenameInCounterScope,
    )
    val requestKey = buildCameraV2RequestKey(
        relativePathKey = request.relativePathKey,
        prefix = request.prefix,
        scanPrefix = request.scanPrefix,
        includePathInScope = request.includePathInScope,
        includeFilenameInScope = request.includeFilenameInScope,
    )

    LaunchedEffect(requestKey, appSettings.saveMode, resumeTick, counterEventTick) {
        val isResumeEvent = resumeTick > lastHandledResumeTick
        val hasCounterEvent = counterEventTick > lastHandledCounterEventTick
        val counterEvent = if (hasCounterEvent) latestCounterEvent else null

        // V2 정책:
        // - saveMode 변경은 새 stream reason으로 처리해 empty stream이면 1을 채택한다.
        // - undo는 downward sync를 허용한다.
        // - 촬영/undo 재동기화는 session stack 추론이 아니라 실제 완료 이벤트 edge를 주 트리거로 사용한다.
        // - resume/event는 edge-trigger로만 처리하여 sticky event를 막는다.
        val reason = detectCameraV2SyncReason(
            isInitial = isInitial,
            isResumeEvent = isResumeEvent,
            counterEvent = counterEvent,
            previousSaveMode = previousSaveMode,
            currentSaveMode = appSettings.saveMode,
            previousRequestKey = previousRequestKey,
            currentRequestKey = requestKey,
        )

        // detect -> read -> decide/apply
        val read = counterFacade.read(request)
        displayedNext = applyCameraV2SyncedNext(
            reason = reason,
            currentDisplayedNext = displayedNext,
            read = read,
            previousRequestKey = previousRequestKey,
            currentRequestKey = requestKey,
        )

        isInitial = false
        previousSaveMode = appSettings.saveMode
        previousRequestKey = requestKey
        if (isResumeEvent) lastHandledResumeTick = resumeTick
        if (hasCounterEvent) lastHandledCounterEventTick = counterEventTick
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 실제 카메라 UI는 기존 화면을 그대로 재사용한다(병행 검증용).
        CameraScreen(
            tableTemplateState = tableTemplateState,
            onTemplateChange = onTemplateChange,
            onOpenTableEditor = onOpenTableEditor,
            onOpenAlbum = onOpenAlbum,
            onOpenRecentCaptureGrid = onOpenRecentCaptureGrid,
            sessionCaptureStack = sessionCaptureStack,
            onCounterSyncEvent = { event ->
                latestCounterEvent = event
                counterEventTick += 1
            },
        )

        // 병행 검증용 probe 오버레이: 기존 CameraScreen은 그대로 두고
        // 새 counter wiring의 reason/read/apply 결과만 가시화한다.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(DDZColor.Surface.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Camera V2 Counter Probe", color = DDZColor.TextPrimary)
            Text("next=$displayedNext", color = DDZColor.TextPrimary)
            Text("mode=${appSettings.saveMode}", color = DDZColor.TextPrimary)
            Text("captures=$captureCount", color = DDZColor.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { resumeTick += 1 }) {
                    Text("V2 Re-sync (manual)")
                }
            }
        }
    }
}
