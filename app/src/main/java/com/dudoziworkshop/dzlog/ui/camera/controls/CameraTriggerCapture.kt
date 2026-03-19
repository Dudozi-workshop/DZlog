package com.dudoziworkshop.dzlog.ui.camera.controls

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.ui.camera.CaptureFeedback
import com.dudoziworkshop.dzlog.ui.camera.effects.LatestImageController
import com.dudoziworkshop.dzlog.ui.camera.state.CameraUiState
import com.dudoziworkshop.dzlog.ui.camera.state.CameraViewModel

internal fun buildCameraTriggerCapture(
    context: Context,
    ui: CameraUiState,
    appSettings: AppSettings,
    boundImageCapture: ImageCapture?,
    finalCapturePreview: FinalCapturePreview?,
    scopedCounterStream: CaptureScopedCounterStream,
    tableTemplateState: TableTemplateState,
    fnDelim: String,
    repository: DzlogRepositoryImpl,
    cameraViewModel: CameraViewModel,
    captureFeedback: CaptureFeedback,
    sessionCaptureStack: SnapshotStateList<List<Uri>>,
    latestImageController: LatestImageController,
    onTemplateChange: (TableTemplateState) -> Unit,
    buildWatermarkConfig: (
        anchor: WatermarkTableAnchor,
        offsetXRatio: Int,
        offsetYRatio: Int,
        boundsOffsetX10000: Int,
        boundsOffsetY10000: Int,
        tableWidthRatio: Int,
        tableHeightRatio: Int,
        tableBgAlpha: Int,
        bgStyle: Int,
        valueScale: Int,
        textColorMode: Int,
        manualTextColor: Int,
        textAlign: Int,
        gridEnabled: Boolean,
        rotationCwDeg: Int
    ) -> WatermarkConfig,
): () -> Unit = trigger@{
    // 오작동 방지: 캡처 불가 상태에서는 입력 피드백/촬영 로직을 모두 실행하지 않는다.
    if (boundImageCapture == null || ui.capture.capturedUri != null || ui.capture.isCapturing) return@trigger
    // counter 미동기화(null) 상태에서는 최종 preview가 없으므로 캡처를 시작하지 않는다.
    val capturePreview = finalCapturePreview ?: return@trigger

    // 정책 변경: 촬영 피드백은 저장 완료가 아니라 촬영 트리거(버튼/음량키) 시점에 즉시 제공한다.
    captureFeedback.play(
        successVibrationEnabled = appSettings.hapticEnabled && appSettings.captureHapticEnabled,
        soundEnabled = appSettings.captureSoundEnabled
    )

    // 구조 정리: capture 후처리 콜백을 하나의 묶음으로 전달해 호출부 가독성을 유지한다.
    val callbacks = CaptureClickCallbacks(
        // 정책 유지: 저장 성공 직후 프리뷰 숫자를 즉시 다음 값으로 반영한다.
        onAdvancePreviewCounter = { nextCounter ->
            val resolved = nextCounter.coerceAtLeast(1)
            ui.counter.scopeNextCounter = resolved
        },
        onAddToSessionStack = { uris ->
            UndoCapturePolicy.pushCapture(sessionCaptureStack, uris)
            // 실제 촬영 저장 완료(세션 stack 반영 완료) 시점 이벤트다.
            // 버튼 클릭 시점이 아니라 완료 시점에만 발행해 본체 카운터 동기화가 즉시 반영되게 한다.
            cameraViewModel.onCaptureCommitted()
            latestImageController.reload()
        },
        onSetCapturedUri = { capturedUri -> ui.capture.capturedUri = capturedUri },
        // 정책 유지: 저장 성공 후 다음 순환문구 cursor를 반영한다.
        onAdvancePhraseProgress = { nextCursor -> cameraViewModel.advancePhraseProgress(nextCursor) },
        onSetCapturing = { ui.capture.isCapturing = it }
    )

    handleCaptureClick(
        context = context,
        gate = ui.capture.captureGate,
        imageCapture = boundImageCapture,
        capturedUriPresent = (ui.capture.capturedUri != null),
        continuousPreviewMode = ui.prefs.continuousPreviewMode,
        finalCapturePreview = capturePreview,
        scopedCounterStream = scopedCounterStream,
        tableTemplateState = tableTemplateState,
        counterDigits = ui.prefs.counterDigits,
        fnDelim = fnDelim,
        captureAspect = ui.prefs.captureAspect,
        saveMode = appSettings.saveMode,
        photoQualityMode = appSettings.photoQualityMode,
        wmTableAnchor = ui.prefs.wmTableAnchor,
        wmOffsetXRatio = ui.prefs.wmOffsetXRatio,
        wmOffsetYRatio = ui.prefs.wmOffsetYRatio,
        wmBoundsOffsetX10000 = ui.prefs.wmBoundsOffsetX10000,
        wmBoundsOffsetY10000 = ui.prefs.wmBoundsOffsetY10000,
        wmTableWidthRatio = ui.prefs.wmTableWidthRatio,
        wmTableHeightRatio = ui.prefs.wmTableHeightRatio,
        wmBgAlpha = ui.prefs.wmBgAlpha,
        wmBgStyle = ui.prefs.wmBgStyle,
        wmValueScale = ui.prefs.wmValueScale,
        wmTextColorMode = ui.prefs.wmTextColorMode,
        wmManualTextColor = ui.prefs.wmManualTextColor,
        wmTextAlign = ui.prefs.wmTextAlign,
        wmGridEnabled = ui.prefs.wmGridEnabled,
        wmRotationCwDeg = ui.prefs.wmRotationCwDeg,
        usableTopRatio = ui.capture.usableTopRatio,
        usableBottomRatio = ui.capture.usableBottomRatio,
        repository = repository,
        buildWatermarkConfig = buildWatermarkConfig,
        onApplyTemplatePatch = { onTemplateChange(it) },
        // 정책 변경: 촬영 성공 직후에는 optimistic UI를 우선하고 즉시 강한 readback resync는 생략한다.
        // 최종 정합성 보정은 resume/undo/saveMode 변경 경로의 CameraCounterSyncEffect가 담당한다.
        onRequestCounterResync = { },
        callbacks = callbacks
    )
}
