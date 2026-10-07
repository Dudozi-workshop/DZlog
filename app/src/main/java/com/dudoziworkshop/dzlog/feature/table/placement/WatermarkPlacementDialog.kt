package com.dudoziworkshop.dzlog.feature.table.placement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControl
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControlStyles
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlin.math.roundToInt

@Composable
fun TablePlacementPreviewDialog(
    templateState: TableTemplateState,
    resolvedCells: List<ResolvedCell>,
    wmBgStyle: Int,
    wmBgAlpha: Int,
    wmValueScale: Int,
    wmTextColorMode: Int,
    wmManualTextColor: Int,
    wmTextAlign: Int,
    wmGridEnabled: Boolean,
    placementState: TablePlacementState,
    onApplyPlacement: (TablePlacementState) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val watermarkCells = remember(resolvedCells) { WatermarkBuilder.buildTableCells(resolvedCells) }
    val aspectOptions = remember { listOf(CaptureAspect.R1_1, CaptureAspect.R3_4, CaptureAspect.R9_16) }

    val entryPlacement = remember(placementState) { placementState.copy(keepAspectRatio = true) }
    val shapeWidthBase = remember(entryPlacement.wmWidthRatio) { entryPlacement.wmWidthRatio.coerceAtLeast(10) }

    var draftCaptureAspect by remember(entryPlacement.captureAspect) { mutableStateOf(entryPlacement.captureAspect) }
    var draftOffsetX by remember(entryPlacement.wmOffsetXRatio) { mutableIntStateOf(entryPlacement.wmOffsetXRatio.coerceIn(0, 100)) }
    var draftOffsetY by remember(entryPlacement.wmOffsetYRatio) { mutableIntStateOf(entryPlacement.wmOffsetYRatio.coerceIn(0, 100)) }
    var draftRotation by remember(entryPlacement.rotationCwDeg) {
        mutableIntStateOf(if (entryPlacement.rotationCwDeg == 90) 90 else 0)
    }
    var draftScale by remember(entryPlacement.wmWidthRatio, shapeWidthBase) {
        mutableFloatStateOf(entryPlacement.wmWidthRatio.toFloat() / shapeWidthBase.toFloat() * 100f)
    }

    fun resetToEntryPlacement() {
        draftCaptureAspect = entryPlacement.captureAspect
        draftOffsetX = entryPlacement.wmOffsetXRatio.coerceIn(0, 100)
        draftOffsetY = entryPlacement.wmOffsetYRatio.coerceIn(0, 100)
        draftRotation = if (entryPlacement.rotationCwDeg == 90) 90 else 0
        draftScale = entryPlacement.wmWidthRatio.toFloat() / shapeWidthBase.toFloat() * 100f
    }
    fun updateDraftOffset(offsetX: Int, offsetY: Int) {
        draftOffsetX = offsetX.coerceIn(0, 100)
        draftOffsetY = offsetY.coerceIn(0, 100)
    }

    val minScalePercent = ((10f / shapeWidthBase) * 100f).coerceAtLeast(10f)
    val maxScalePercent = ((100f / shapeWidthBase) * 100f).coerceAtMost(300f)
    val previewWidthRatio = (shapeWidthBase * (draftScale / 100f)).roundToInt().coerceIn(10, 100)
    val previewHeightRatio = previewWidthRatio

    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DDZColor.Primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "촬영화면 미리보기",
                color = DDZColor.Card,
                style = DDZTypography.CardTitle
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clipToBounds()
                    .background(DDZColor.Primary)
                    .border(1.dp, DDZColor.Border, RoundedCornerShape(12.dp))
            ) {
                CameraLikeWatermarkPlacementPreview(
                    captureAspect = draftCaptureAspect,
                    rows = templateState.rows,
                    cols = templateState.cols,
                    templateCells = templateState.cells,
                    watermarkCells = watermarkCells,
                    offsetXRatio = draftOffsetX,
                    offsetYRatio = draftOffsetY,
                    tableWidthRatio = previewWidthRatio,
                    tableHeightRatio = previewHeightRatio,
                    rotationCwDeg = draftRotation,
                    bgStyle = wmBgStyle,
                    bgAlpha = wmBgAlpha,
                    valueScale = wmValueScale,
                    textColorMode = wmTextColorMode,
                    manualTextColor = wmManualTextColor,
                    textAlign = wmTextAlign,
                    drawGrid = wmGridEnabled,
                    onDragPreview = ::updateDraftOffset,
                    onDragCommit = ::updateDraftOffset,
                    modifier = Modifier.clipToBounds()
                )
            }

            Text(
                text = "표를 움직여 위치를 이동하세요.",
                color = DDZColor.Card,
                style = DDZTypography.Caption
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "비율 설정",
                    color = DDZColor.Card,
                    style = DDZTypography.Caption,
                    modifier = Modifier.weight(1f)
                )

                DDZSegmentedControl(
                    options = listOf("1:1", "3:4", "9:16"),
                    selectedIndex = aspectOptions.indexOf(draftCaptureAspect).coerceAtLeast(0),
                    onSelect = { index -> draftCaptureAspect = aspectOptions[index] },
                    modifier = Modifier
                        .width(160.dp)
                        .height(28.dp),
                    style = DDZSegmentedControlStyles.PlacementCompact
                )

                TextButton(
                    onClick = { draftRotation = if (draftRotation == 90) 0 else 90 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "회전",
                        color = DDZColor.Card,
                        style = DDZTypography.Caption
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("표 크기", color = DDZColor.Card, style = DDZTypography.Caption)
                Slider(
                    value = draftScale.coerceIn(minScalePercent, maxScalePercent),
                    onValueChange = { requested -> draftScale = requested.coerceIn(minScalePercent, maxScalePercent) },
                    valueRange = minScalePercent..maxScalePercent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { resetToEntryPlacement() }) { Text("초기화") }
                TextButton(onClick = onClose) { Text("닫기") }
                Button(onClick = {
                    onApplyPlacement(
                        entryPlacement.copy(
                            wmOffsetXRatio = draftOffsetX,
                            wmOffsetYRatio = draftOffsetY,
                            wmWidthRatio = previewWidthRatio,
                            wmHeightRatio = previewHeightRatio,
                            rotationCwDeg = draftRotation,
                            captureAspect = draftCaptureAspect,
                            keepAspectRatio = true,
                        )
                    )
                }) {
                    Text("적용")
                }
            }
        }
    }
}
