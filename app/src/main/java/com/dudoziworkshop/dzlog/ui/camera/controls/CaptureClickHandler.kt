package com.dudoziworkshop.dzlog.ui.camera.controls

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import com.dudoziworkshop.dzlog.data.repository.CaptureAsyncCaptureService
import com.dudoziworkshop.dzlog.data.repository.CaptureJobSnapshot
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePendingProjection
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationScope
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.naming.resolveGroupValue
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class CaptureClickCallbacks(
    val onAdvancePreviewCounter: (Int) -> Unit,
    val onAddToSessionStack: (List<Uri>) -> Unit,
    val onSetCapturedUri: (Uri?) -> Unit,
    val onSetCapturing: (Boolean) -> Unit,
    val onProgressPersistenceFailed: () -> Unit = {},
    val onCaptureStarted: () -> Unit = {},
    val onImageCaptured: () -> Unit = {},
    val onPendingStateChange: (Int, Int) -> Unit = { _, _ -> },
    val onAsyncFailure: (String) -> Unit = {},
)

/**
 * [handleCaptureClick]
 * - 목적: 촬영 버튼 클릭 시 저장 실행/commit 후처리만 담당한다.
 * - 핵심 정책: 계산은 CameraScreen의 finalCapturePreview에서 단일화하고, 클릭 시 재계산하지 않는다.
 */
internal fun handleCaptureClick(
    context: Context,
    gate: AtomicBoolean,
    imageCapture: ImageCapture?,
    capturedUriPresent: Boolean,
    continuousPreviewMode: ContinuousPreviewMode,
    finalCapturePreview: FinalCapturePreview,
    scopedCounterStream: CaptureScopedCounterStream,
    tableTemplateState: TableTemplateState,
    templateId: String? = null,
    counterDigits: Int,
    fnDelim: String,
    captureAspect: CaptureAspect,
    saveMode: com.dudoziworkshop.dzlog.domain.model.SaveMode,
    photoQualityMode: PhotoQualityMode,
    wmTableAnchor: WatermarkTableAnchor,
    wmOffsetXRatio: Int,
    wmOffsetYRatio: Int,
    wmBoundsOffsetX10000: Int,
    wmBoundsOffsetY10000: Int,
    wmTableWidthRatio: Int,
    wmTableHeightRatio: Int,
    wmBgAlpha: Int,
    wmBgStyle: Int,
    wmValueScale: Int,
    wmTextColorMode: Int,
    wmManualTextColor: Int,
    wmTextAlign: Int,
    wmGridEnabled: Boolean,
    wmRotationCwDeg: Int,
    usableTopRatio: Float,
    usableBottomRatio: Float,
    repository: DzlogRepositoryImpl,
    asyncService: CaptureAsyncCaptureService? = null,
    pendingProjection: CapturePendingProjection? = null,
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
    ) -> com.dudoziworkshop.dzlog.domain.model.WatermarkConfig,
    onApplyTemplatePatch: (TableTemplateState) -> Unit,
    onRequestCounterResync: () -> Unit,
    callbacks: CaptureClickCallbacks
) {
    val imageCaptureNonNull = imageCapture ?: run {
        Toast.makeText(context, "카메라 준비 중", Toast.LENGTH_SHORT).show()
        return
    }

    if (capturedUriPresent) return
    if (!gate.compareAndSet(false, true)) return
    callbacks.onSetCapturing(true)

    val req = com.dudoziworkshop.dzlog.domain.model.CaptureRequest(
        // 주요 정책: 실제 저장경로는 preview pipeline이 계산한 relativePath를 그대로 사용한다.
        relativePath = finalCapturePreview.relativePathPreview,
        group1 = resolveGroupValue(finalCapturePreview.resolvedCells, GroupLevel.G1),
        group2 = resolveGroupValue(finalCapturePreview.resolvedCells, GroupLevel.G2),
        displayName = finalCapturePreview.displayName,
        resolvedCells = finalCapturePreview.resolvedCells,
        watermarkCells = WatermarkBuilder.buildTableCells(finalCapturePreview.resolvedCells),
        saveMode = saveMode,
        captureAspect = captureAspect,
        photoQualityMode = photoQualityMode,
        tableTemplate = tableTemplateState,
        usableTopRatio = usableTopRatio.coerceIn(0f, 1f),
        usableBottomRatio = usableBottomRatio.coerceIn(0f, 1f),
        watermark = buildWatermarkConfig(
            wmTableAnchor,
            wmOffsetXRatio,
            wmOffsetYRatio,
            wmBoundsOffsetX10000,
            wmBoundsOffsetY10000,
            wmTableWidthRatio,
            wmTableHeightRatio,
            wmBgAlpha,
            wmBgStyle,
            wmValueScale,
            wmTextColorMode,
            wmManualTextColor,
            wmTextAlign,
            wmGridEnabled,
            wmRotationCwDeg
        )
    )

    if (continuousPreviewMode == ContinuousPreviewMode.OFF &&
        asyncService != null && pendingProjection != null
    ) {
        try {
            check(!templateId.isNullOrBlank()) {
                "활성 템플릿이 준비되지 않았습니다. 템플릿을 다시 선택해주세요."
            }
            // Capture the exact stream and resolved table snapshot on the shutter press.
            val streamKey = scopedCounterStream.captureStreamKey
            val reservationScope = CaptureReservationScope(
                relativePathKey = streamKey.relativePathKey,
                streamPrefix = streamKey.prefix,
            )
            val reserved = asyncService.reserve(
                scope = reservationScope,
                expectedNextCounter = finalCapturePreview.usedCounter,
                snapshotForCounter = { allocated ->
                    check(allocated == finalCapturePreview.usedCounter) {
                        "촬영 번호 동기화가 필요합니다. 카메라를 다시 열어주세요."
                    }
                    CaptureJobSnapshot(
                        request = req,
                        tablePatch = finalCapturePreview.tablePatch,
                        counterStream = streamKey,
                        templateId = templateId,
                    )
                },
            )
            pendingProjection.add(
                CapturePendingProjection.Shot(
                    id = reserved.reservation.id,
                    scope = reservationScope,
                    counter = reserved.reservation.counter,
                    templatePatch = finalCapturePreview.tablePatch,
                )
            )
            callbacks.onPendingStateChange(
                pendingProjection.pendingCount(),
                reserved.reservation.counter + 1,
            )
            asyncService.captureReserved(
                imageCapture = imageCaptureNonNull,
                reservation = reserved,
                callbacks = CaptureAsyncCaptureService.CaptureCallbacks(
                    onCaptureStarted = callbacks.onCaptureStarted,
                    onImageCaptured = callbacks.onImageCaptured,
                    onQueued = {
                        gate.set(false)
                        callbacks.onSetCapturing(false)
                    },
                    onQueuedLater = {
                        callbacks.onAsyncFailure("저장 대기열이 가득 찼습니다. 원본 사진은 보관되어 있습니다.")
                        gate.set(false)
                        callbacks.onSetCapturing(false)
                    },
                    onFailure = { error ->
                        Log.e("DZlogCapture", "Capture staging failed", error)
                        callbacks.onAsyncFailure("촬영 원본을 안전하게 저장하지 못했습니다. 촬영 상태를 확인해주세요.")
                        gate.set(false)
                        callbacks.onSetCapturing(false)
                    },
                ),
            )
        } catch (error: Exception) {
            Log.e("DZlogCapture", "Capture reservation failed", error)
            callbacks.onAsyncFailure(error.message ?: "촬영 예약 실패")
            gate.set(false)
            callbacks.onSetCapturing(false)
        }
        return
    }

    repository.captureAndSave(
        context = context,
        imageCapture = imageCaptureNonNull,
        request = req,
        onCaptureStarted = callbacks.onCaptureStarted,
        onImageCaptured = callbacks.onImageCaptured,
        onDone = { entry ->
            val committedCounter = CaptureNamingPolicy.parseUsedCounterFromDisplayName(
                displayName = entry.displayName,
                fnDelim = fnDelim,
                counterDigits = counterDigits
            )?.coerceAtLeast(1) ?: finalCapturePreview.usedCounter

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 핵심 수정: Camera read(CameraCounterSyncEffect)와 동일한 scoped stream key로 commit한다.
                    // includePath/includeFilename scope OFF 시에도 read/commit 키가 분리되지 않도록 일치화한다.
                    CaptureCounterPolicy.commit(
                        context = context,
                        scopedStream = scopedCounterStream,
                        usedCounter = committedCounter,
                        mediaStoreId = entry.mediaStoreId
                    )

                    withContext(Dispatchers.Main) {
                        // 정책 유지: 저장 성공 후에만 템플릿 patch/문구 진행/카운터 재동기화를 반영한다.
                        onApplyTemplatePatch(tableTemplateState.applyPatch(finalCapturePreview.tablePatch))
                        // UX 개선: 저장 성공 직후 프리뷰 카운터를 committedCounter + 1로 즉시 반영한다.
                        // 정합성은 기존 CameraCounterSyncEffect 경로가 최종 보정한다.
                        callbacks.onAdvancePreviewCounter(committedCounter + 1)
                        onRequestCounterResync()

                        if (entry.isNameAdjusted) {
                            Toast.makeText(
                                context,
                                "중복 파일명으로 ${entry.displayName} 저장됨",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        val savedUris = entry.savedContentUris
                            .asSequence()
                            .filter { it != Uri.EMPTY }
                            .distinct()
                            .toList()
                        callbacks.onAddToSessionStack(savedUris)

                        if (continuousPreviewMode != ContinuousPreviewMode.OFF) {
                            callbacks.onSetCapturedUri(entry.contentUri)
                        }

                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("DZlogCapture", "사진 저장 후 진행 상태 기록 실패", error)
                    withContext(Dispatchers.Main) {
                        // Media already exists; preserve access/Undo and rescan before another capture.
                        callbacks.onAddToSessionStack(entry.savedContentUris.filter { it != Uri.EMPTY }.distinct())
                        if (continuousPreviewMode != ContinuousPreviewMode.OFF) {
                            callbacks.onSetCapturedUri(entry.contentUri)
                        }
                        callbacks.onProgressPersistenceFailed()
                        Toast.makeText(context, "사진은 저장됐지만 번호·문구 상태를 기록하지 못했습니다. 다시 확인해주세요.", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    withContext(NonCancellable + Dispatchers.Main) {
                        gate.set(false)
                        callbacks.onSetCapturing(false)
                    }
                }
            }
        },
        onFail = { msg ->
            gate.set(false)
            callbacks.onSetCapturing(false)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    )
}

