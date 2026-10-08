package com.dudoziworkshop.dzlog.feature.counter.camera

import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import com.dudoziworkshop.dzlog.ui.camera.state.CameraUiState

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
    val context = LocalContext.current
    // 이전 사이클 메모리는 단일 객체로 유지하여 read/write 의도를 명확히 한다.
    var syncMemory by remember { mutableStateOf(CameraCounterSyncMemory()) }

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
    val requestKey: String = remember(counterRequest) {
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
        ui.capture.isCapturing,
    ) {
        if (!isTemplateReady || ui.capture.isCapturing) return@LaunchedEffect

        val isResumeEvent = resumeTick > syncMemory.lastHandledResumeTick
        val hasCounterEvent = counterEventTick > syncMemory.lastHandledCounterEventTick
        val counterEvent = if (hasCounterEvent) latestCounterEvent else null

        // 카메라 카운터 동기화는 detect -> read -> decide/apply 구조를 사용한다.
        // - saveMode 변경은 새 stream 전환으로 처리
        // - CAPTURE_COMMITTED는 같은 stream 전진 이벤트
        // - UNDO_COMMITTED는 하향 동기화 허용
        // - same stream resume/re-entry에서는 불필요한 하향을 방지
        val reason = detectCameraSyncReason(
            isInitial = syncMemory.isInitial,
            isResumeEvent = isResumeEvent,
            counterEvent = counterEvent,
            previousSaveMode = syncMemory.previousSaveMode,
            currentSaveMode = appSettings.saveMode,
            previousRequestKey = syncMemory.previousRequestKey,
            currentRequestKey = requestKey,
        )
        val readResult = readCameraCounterSafely { counterFacade.read(counterRequest) }
        currentCoroutineContext().ensureActive()
        val read = readResult.getOrElse { error ->
            ui.counter.scopeNextCounter = null
            Log.e("CounterReadback", "Camera counter read failed", error)
            Toast.makeText(context, "저장 이력을 확인하지 못했습니다. 카메라 화면을 다시 열어주세요.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }
        val currentDisplayedNext = (ui.counter.scopeNextCounter ?: 1).coerceAtLeast(1)
        ui.counter.scopeNextCounter = applyCameraSyncedNext(
            reason = reason,
            currentDisplayedNext = currentDisplayedNext,
            read = read,
            previousRequestKey = syncMemory.previousRequestKey,
            currentRequestKey = requestKey,
        )

        // 상태 전이는 단일 시점 copy로 기록해 이전/다음 사이클 경계를 명확히 유지한다.
        // NOTE: 이 값은 다음 LaunchedEffect cycle에서 read 되므로 dead assignment가 아니다.
        @Suppress("AssignedValueIsNeverRead")
        syncMemory = syncMemory.copy(
            isInitial = false,
            previousSaveMode = appSettings.saveMode,
            previousRequestKey = requestKey,
            lastHandledResumeTick = if (isResumeEvent) resumeTick else syncMemory.lastHandledResumeTick,
            lastHandledCounterEventTick = if (hasCounterEvent) {
                counterEventTick
            } else {
                syncMemory.lastHandledCounterEventTick
            },
        )
    }
}

private data class CameraCounterSyncMemory(
    val isInitial: Boolean = true,
    val lastHandledResumeTick: Int = -1,
    val lastHandledCounterEventTick: Int = -1,
    val previousSaveMode: SaveMode? = null,
    val previousRequestKey: String? = null,
)

