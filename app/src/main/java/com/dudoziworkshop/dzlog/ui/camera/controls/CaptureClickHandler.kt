package com.dudoziworkshop.dzlog.ui.camera.controls

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureContext
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.naming.resolveGroupValue
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [handleCaptureClick]
 * - 목적: 촬영 버튼 클릭 처리 로직을 CameraScreen에서 분리함(동작 불변)
 * - 포함: 게이트(중복 촬영 방지) + CaptureRequest 구성 + captureAndSave 호출 + 후처리 콜백 호출
 * - 제외: UI 렌더링(Compose UI), CameraX 바인딩
 */
internal fun handleCaptureClick(
    context: Context,
    gate: AtomicBoolean,
    imageCapture: ImageCapture?,
    capturedUriPresent: Boolean,
    continuousPreviewMode: ContinuousPreviewMode,
    tableResolver: TableResolver,
    tableTemplateState: TableTemplateState,
    counterDigits: Int,
    dateFormat: String,
    timeFormat: String,
    fnDelim: String,
    scopeNextCounter: Int,
    phraseProgressCounter: Int,
    perPhraseModeEnabled: Boolean,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    dateScopeValues: List<String>,
    timeScopeValues: List<String>,
    phraseScopeValues: List<String>,
    captureHapticEnabled: Boolean,
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
    onAddToSessionStack: (List<Uri>) -> Unit,
    onHaptic: () -> Unit,
    onSetCapturedUri: (Uri?) -> Unit,
    onAdvancePhraseProgress: () -> Unit,
    onSetCapturing: (Boolean) -> Unit
) {
    // ✅ imageCapture null 가드(토스트 + return)
    // (부분 패치 적용으로 imageCaptureNonNull 참조만 남는 케이스 방지)
    val imageCaptureNonNull = imageCapture ?: run {
        Toast.makeText(context, "카메라 준비 중", Toast.LENGTH_SHORT).show()
        return
    }

    if (capturedUriPresent) return

    // ✅ 중복 촬영 방지: 첫 클릭만 통과
    if (!gate.compareAndSet(false, true)) return
    onSetCapturing(true)
    if (captureHapticEnabled) onHaptic()

    if (Log.isLoggable("CaptureFlow", Log.DEBUG)) {
        Log.d(
            "CaptureFlow",
            "clickStart phraseProgressCounter=$phraseProgressCounter scopeNextCounter=$scopeNextCounter perPhraseModeEnabled=$perPhraseModeEnabled"
        )
    }

    val captureNow = Date()
    val planForCapture = tableResolver.plan(
        cells = tableTemplateState.cells,
        captureNow = captureNow,
        config = TableResolver.Config(
            counterDigits = counterDigits,
            dateFormat = dateFormat,
            timeFormat = timeFormat
        ),
        counterSeedOverride = scopeNextCounter,
        phraseProgressCounter = phraseProgressCounter,
        phraseSets = tableTemplateState.phraseSets
    )

    val policyResult = CaptureNamingPolicy.buildForCaptureWithCounter(
        captureContext = CaptureContext(
            resolvedCells = planForCapture.resolvedCells,
            fileNameSlots = tableTemplateState.fileNameSlots,
            fnDelim = fnDelim,
            counterDigits = counterDigits,
            dateFormat = dateFormat,
            timeFormat = timeFormat,
            includePathInCounterScope = includePathInCounterScope,
            includeFilenameInCounterScope = includeFilenameInCounterScope,
            dateScopeValues = dateScopeValues,
            timeScopeValues = timeScopeValues,
            phraseScopeValues = phraseScopeValues,
        ),
        usedCounter = scopeNextCounter
    )


    val req = com.dudoziworkshop.dzlog.domain.model.CaptureRequest(
        group1 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G1),
        group2 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G2),
        displayName = policyResult.displayName,
        resolvedCells = planForCapture.resolvedCells,
        watermarkCells = WatermarkBuilder.buildTableCells(planForCapture.resolvedCells),
        saveMode = saveMode,
        captureAspect = captureAspect,
        photoQualityMode = photoQualityMode,
        tableTemplate = tableTemplateState,
        usableTopRatio = usableTopRatio.coerceIn(0f, 1f),
        usableBottomRatio = usableBottomRatio.coerceIn(0f, 1f),
        // 함수 타입 호출에서는 named argument 금지 → positional로 호출해야 함
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
            onApplyTemplatePatch(tableTemplateState.applyPatch(planForCapture.patch))

            val committedCounter = CaptureNamingPolicy.parseUsedCounterFromDisplayName(
                displayName = entry.displayName,
                fnDelim = fnDelim,
                counterDigits = counterDigits
            )?.coerceAtLeast(1) ?: policyResult.usedCounter
            if (Log.isLoggable("CaptureFlow", Log.DEBUG)) {
                Log.d(
                    "CaptureFlow",
                    "captureSuccess committedCounter=$committedCounter displayName=${entry.displayName} phraseScopeValues=$phraseScopeValues"
                )
            }

            CoroutineScope(Dispatchers.IO).launch {
                CaptureCounterPolicy.commitCounter(
                    context = context,
                    streamContext = policyResult.streamContext,
                    usedCounter = committedCounter,
                    mediaStoreId = entry.mediaStoreId
                )

                withContext(Dispatchers.Main) {
                    if (Log.isLoggable("CaptureFlow", Log.DEBUG)) {
                        Log.d(
                            "CaptureFlow",
                            "beforeMainBranch perPhraseModeEnabled=$perPhraseModeEnabled committedCounter=$committedCounter"
                        )
                    }
                    if (perPhraseModeEnabled) {
                        // 정책: 문구별 모드에서는 파일 카운터를 직접 +1로 밀지 않고,
                        // 문구 진행 → scope resync 순서로 SSOT(next seed)를 반영한다.
                        onAdvancePhraseProgress()
                        onRequestCounterResync()
                    } else {
                        // 정책 변경: 통합 모드도 저장 성공 후 resync로만 next seed를 반영한다(선증가 제거).
                        onRequestCounterResync()
                    }

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

                    // 정책 유지: gate/capturing 해제는 commit 완료 이후에만 수행한다.
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
