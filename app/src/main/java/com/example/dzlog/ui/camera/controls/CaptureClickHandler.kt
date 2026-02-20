package com.example.dzlog.ui.camera.controls

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.ImageCapture
import com.example.dzlog.data.repository.DzlogRepositoryImpl
import com.example.dzlog.domain.capturepolicy.CaptureContext
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.table.applyPatch
import com.example.dzlog.domain.watermark.WatermarkBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    captureHapticEnabled: Boolean,
    captureAspect: CaptureAspect,
    saveMode: com.example.dzlog.domain.model.SaveMode,
    wmTableAnchor: WatermarkTableAnchor,
    wmOffsetXRatio: Int,
    wmOffsetYRatio: Int,
    wmTableWidthRatio: Int,
    wmTableHeightRatio: Int,
    wmBgAlpha: Int,
    wmBgStyle: Int,
    wmLabelScale: Int,
    wmValueScale: Int,
    usableTopRatio: Float,
    usableBottomRatio: Float,
    repository: DzlogRepositoryImpl,
    buildWatermarkConfig: (
        anchor: WatermarkTableAnchor,
        offsetXRatio: Int,
        offsetYRatio: Int,
        tableWidthRatio: Int,
        tableHeightRatio: Int,
        tableBgAlpha: Int,
        bgStyle: Int,
        labelScale: Int,
        valueScale: Int
    ) -> com.example.dzlog.domain.model.WatermarkConfig,
    onApplyTemplatePatch: (TableTemplateState) -> Unit,
    onUpdateScopeNextCounter: (Int) -> Unit,
    onAddToSessionStack: (List<Uri>) -> Unit,
    onHaptic: () -> Unit,
    onSetCapturedUri: (Uri?) -> Unit,
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
    val optimisticNext = (scopeNextCounter + 1).coerceAtLeast(1)
    onUpdateScopeNextCounter(optimisticNext)

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
        ),
        usedCounter = scopeNextCounter
    )

    val req = com.example.dzlog.domain.model.CaptureRequest(
        group1 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G1),
        group2 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G2),
        displayName = policyResult.displayName,
        resolvedCells = planForCapture.resolvedCells,
        watermarkCells = WatermarkBuilder.buildTableCells(planForCapture.resolvedCells),
        saveMode = saveMode,
        captureAspect = captureAspect,
        tableTemplate = tableTemplateState,
        usableTopRatio = usableTopRatio.coerceIn(0f, 1f),
        usableBottomRatio = usableBottomRatio.coerceIn(0f, 1f),
        // 함수 타입 호출에서는 named argument 금지 → positional로 호출해야 함
        watermark = buildWatermarkConfig(
            wmTableAnchor,
            wmOffsetXRatio,
            wmOffsetYRatio,
            wmTableWidthRatio,
            wmTableHeightRatio,
            wmBgAlpha,
            wmBgStyle,
            wmLabelScale,
            wmValueScale
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

            CoroutineScope(Dispatchers.IO).launch {
                CaptureCounterPolicy.commitCounter(
                    context = context,
                    streamContext = policyResult.streamContext,
                    usedCounter = committedCounter,
                    mediaStoreId = entry.mediaStoreId
                )
            }

            // ✅ 촬영 후 next counter는 "실제 저장된 파일명 counter" 기준으로 +1 진전
            // (중복 이름 보정으로 displayName이 조정된 경우도 실제 저장값을 반영)
            onUpdateScopeNextCounter((committedCounter + 1).coerceAtLeast(1))

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
        },
        onFail = { msg ->
            onUpdateScopeNextCounter(scopeNextCounter)
            gate.set(false)
            onSetCapturing(false)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    )
}
