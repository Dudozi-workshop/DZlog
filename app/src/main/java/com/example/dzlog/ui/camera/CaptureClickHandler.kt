package com.example.dzlog.ui.camera

import android.content.Context
import android.widget.Toast
import androidx.camera.core.ImageCapture
import com.example.dzlog.data.repository.DzlogRepositoryImpl
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.table.applyPatch
import com.example.dzlog.domain.watermark.WatermarkBuilder
import kotlinx.coroutines.CoroutineScope
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
    scope: CoroutineScope,
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
    onSetCapturedUri: (android.net.Uri?) -> Unit,
    onSetCapturing: (Boolean) -> Unit
) {
    val cap = imageCapture
    if (cap == null) {
        Toast.makeText(context, "카메라 준비 중", Toast.LENGTH_SHORT).show()
        return
    }
    if (capturedUriPresent) return

    // ✅ 중복 촬영 방지: 첫 클릭만 통과
    if (!gate.compareAndSet(false, true)) return
    onSetCapturing(true)

    val captureNow = Date()
    val planForCapture = tableResolver.plan(
        cells = tableTemplateState.cells,
        captureNow = captureNow,
        config = TableResolver.Config(
            counterDigits = counterDigits,
            dateFormat = dateFormat,
            timeFormat = timeFormat
        )
    )

    val hasCounterCell = planForCapture.resolvedCells.any { it.type == TableCellDataType.COUNTER }
    val req = com.example.dzlog.domain.model.CaptureRequest(
        group1 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G1),
        group2 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G2),
        displayName = buildDisplayNameFromResolvedCells(
            resolvedCells = planForCapture.resolvedCells,
            fnDelim = fnDelim,
            includeDate = false,
            includeTime = false,
            counterOverride = if (hasCounterCell) null else scopeNextCounter,
            now = captureNow
        ),
        resolvedCells = planForCapture.resolvedCells,
        watermarkCells = WatermarkBuilder.buildTableCells(planForCapture.resolvedCells),
        saveMode = saveMode,
        captureAspect = captureAspect,
        tableTemplate = tableTemplateState,
        watermark = buildWatermarkConfig(
            anchor = wmTableAnchor,
            offsetXRatio = wmOffsetXRatio,
            offsetYRatio = wmOffsetYRatio,
            tableWidthRatio = wmTableWidthRatio,
            tableHeightRatio = wmTableHeightRatio,
            tableBgAlpha = wmBgAlpha,
            bgStyle = wmBgStyle,
            labelScale = wmLabelScale,
            valueScale = wmValueScale
        )
    )

    repository.captureAndSave(
        context = context,
        imageCapture = cap,
        request = req,
        onDone = { entry ->
            gate.set(false)
            onSetCapturing(false)

            onApplyTemplatePatch(tableTemplateState.applyPatch(planForCapture.patch))

            if (!hasCounterCell) {
                val base = entry.displayName.substringBeforeLast('.', entry.displayName)
                val token = base.substringAfterLast('_', missingDelimiterValue = "").trim()
                val parsed = if (token.all { it.isDigit() }) token.toIntOrNull() else null
                onUpdateScopeNextCounter(((parsed ?: scopeNextCounter) + 1).coerceAtLeast(1))
            }

            if (entry.isNameAdjusted) {
                Toast.makeText(
                    context,
                    "중복 파일명으로 ${entry.displayName} 저장됨",
                    Toast.LENGTH_SHORT
                ).show()
            }

            if (continuousPreviewMode != ContinuousPreviewMode.OFF) {
                onSetCapturedUri(entry.contentUri)
            } else {
                Toast.makeText(context, "저장 완료", Toast.LENGTH_SHORT).show()
            }
        },
        onFail = { msg ->
            gate.set(false)
            onSetCapturing(false)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    )
}
