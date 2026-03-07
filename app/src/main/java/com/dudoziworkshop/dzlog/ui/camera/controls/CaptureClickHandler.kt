package com.dudoziworkshop.dzlog.ui.camera.controls

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.ImageCapture
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.domain.captureplan.CapturePlan
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.naming.resolveGroupValue
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [handleCaptureClick]
 * - 목적: 촬영 버튼 클릭 시 저장 실행/commit 후처리만 담당한다.
 * - 핵심 정책: 계산은 CameraScreen의 activePlan에서 단일화하고, 클릭 시 재계산하지 않는다.
 */
internal fun handleCaptureClick(
    context: Context,
    gate: AtomicBoolean,
    imageCapture: ImageCapture?,
    capturedUriPresent: Boolean,
    continuousPreviewMode: ContinuousPreviewMode,
    activePlan: CapturePlan,
    tableTemplateState: TableTemplateState,
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
    onAdvancePreviewCounter: (Int) -> Unit,
    onAddToSessionStack: (List<Uri>) -> Unit,
    onSetCapturedUri: (Uri?) -> Unit,
    onAdvancePhraseProgress: (Int) -> Unit,
    onSetCapturing: (Boolean) -> Unit
) {
    val imageCaptureNonNull = imageCapture ?: run {
        Toast.makeText(context, "카메라 준비 중", Toast.LENGTH_SHORT).show()
        return
    }

    if (capturedUriPresent) return
    if (!gate.compareAndSet(false, true)) return
    onSetCapturing(true)

    val req = com.dudoziworkshop.dzlog.domain.model.CaptureRequest(
        group1 = resolveGroupValue(activePlan.resolvedCells, GroupLevel.G1),
        group2 = resolveGroupValue(activePlan.resolvedCells, GroupLevel.G2),
        displayName = activePlan.displayName,
        resolvedCells = activePlan.resolvedCells,
        watermarkCells = WatermarkBuilder.buildTableCells(activePlan.resolvedCells),
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

    repository.captureAndSave(
        context = context,
        imageCapture = imageCaptureNonNull,
        request = req,
        onDone = { entry ->
            val committedCounter = CaptureNamingPolicy.parseUsedCounterFromDisplayName(
                displayName = entry.displayName,
                fnDelim = fnDelim,
                counterDigits = counterDigits
            )?.coerceAtLeast(1) ?: activePlan.usedCounter

            CoroutineScope(Dispatchers.IO).launch {
                CaptureCounterPolicy.commitCounter(
                    context = context,
                    streamContext = activePlan.streamContext,
                    usedCounter = committedCounter,
                    mediaStoreId = entry.mediaStoreId
                )

                withContext(Dispatchers.Main) {
                    // 정책 유지: 저장 성공 후에만 템플릿 patch/문구 진행/카운터 재동기화를 반영한다.
                    onApplyTemplatePatch(tableTemplateState.applyPatch(activePlan.tablePatch))
                    // UX 개선: 저장 성공 직후 프리뷰 카운터를 committedCounter + 1로 즉시 반영한다.
                    // 정합성은 기존 onRequestCounterResync() 경로가 최종 보정한다.
                    onAdvancePreviewCounter(committedCounter + 1)
                    // 정책 정리(2차): 문구 진행은 모드와 무관하게 저장 성공 후 plan 기준으로만 전진한다.
                    onAdvancePhraseProgress(activePlan.nextPhraseProgressCursor)
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
                    onAddToSessionStack(savedUris)

                    if (continuousPreviewMode != ContinuousPreviewMode.OFF) {
                        onSetCapturedUri(entry.contentUri)
                    }

                    gate.set(false)
                    onSetCapturing(false)
                }
            }
        },
        onFail = { msg ->
            gate.set(false)
            onSetCapturing(false)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    )
}
