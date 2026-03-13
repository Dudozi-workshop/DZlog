package com.dudoziworkshop.dzlog.feature.counter.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import com.dudoziworkshop.dzlog.ui.camera.CameraUiState

@Composable
internal fun CameraCounterSyncEffect(
    counterScope: CounterScope,
    scanPrefix: String,
    resumeTick: Int,
    counterEventTick: Int,
    latestCounterEvent: CameraCounterSyncEvent?,
    isTemplateReady: Boolean,
    appSettings: AppSettings,
    ui: CameraUiState,
    counterFacade: CounterFacade,
) {
    var isInitial by remember { mutableStateOf(true) }
    var lastHandledResumeTick by remember { mutableIntStateOf(-1) }
    var lastHandledCounterEventTick by remember { mutableIntStateOf(-1) }
    var previousSaveMode by remember { mutableStateOf<SaveMode?>(null) }
    var previousRequestKey by remember { mutableStateOf<String?>(null) }

    val counterRequest = remember(
        appSettings.saveMode,
        counterScope.relativePathKey,
        counterScope.streamPrefix,
        scanPrefix,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
    ) {
        CounterRequestResolver.fromCamera(
            saveMode = appSettings.saveMode,
            relativePathKey = counterScope.relativePathKey,
            prefix = counterScope.streamPrefix,
            scanPrefix = scanPrefix,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )
    }
    val requestKey = remember(counterRequest) {
        buildCameraRequestKey(
            relativePathKey = counterRequest.relativePathKey,
            prefix = counterRequest.prefix,
            scanPrefix = counterRequest.scanPrefix,
            includePathInScope = counterRequest.includePathInScope,
            includeFilenameInScope = counterRequest.includeFilenameInScope,
        )
    }

    LaunchedEffect(
        isTemplateReady,
        counterRequest,
        requestKey,
        resumeTick,
        counterEventTick,
        appSettings.saveMode,
    ) {
        if (!isTemplateReady || ui.capture.isCapturing) return@LaunchedEffect

        val isResumeEvent = resumeTick > lastHandledResumeTick
        val hasCounterEvent = counterEventTick > lastHandledCounterEventTick
        val counterEvent = if (hasCounterEvent) latestCounterEvent else null

        // 카메라 카운터 동기화는 detect -> read -> decide/apply 구조를 사용한다.
        // - saveMode 변경은 새 stream 전환으로 처리
        // - CAPTURE_COMMITTED는 같은 stream 전진 이벤트
        // - UNDO_COMMITTED는 하향 동기화 허용
        // - same stream resume/re-entry에서는 불필요한 하향을 방지
        val reason = detectCameraSyncReason(
            isInitial = isInitial,
            isResumeEvent = isResumeEvent,
            counterEvent = counterEvent,
            previousSaveMode = previousSaveMode,
            currentSaveMode = appSettings.saveMode,
            previousRequestKey = previousRequestKey,
            currentRequestKey = requestKey,
        )
        val read = counterFacade.read(counterRequest)
        val currentDisplayedNext = (ui.counter.scopeNextCounter ?: 1).coerceAtLeast(1)
        ui.counter.scopeNextCounter = applyCameraSyncedNext(
            reason = reason,
            currentDisplayedNext = currentDisplayedNext,
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
}
