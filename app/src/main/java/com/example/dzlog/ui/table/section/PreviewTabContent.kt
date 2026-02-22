package com.example.dzlog.ui.table.section

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.table.ResolvedCell
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.common.DDZSegmentedControl
import com.example.dzlog.ui.table.TableRowColSizeSection
import com.example.dzlog.ui.table.watermark.CameraLikeWatermarkPlacementPreview
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
fun PreviewTabContent(
    scrollState: ScrollState,
    captureAspect: CaptureAspect,
    templateState: TableTemplateState,
    resolvedCells: List<ResolvedCell>,
    wmAnchor: WatermarkTableAnchor,
    wmOffsetXRatio: Int,
    wmOffsetYRatio: Int,
    wmWidthRatio: Int,
    wmHeightRatio: Int,
    wmBgStyle: Int,
    wmBgAlpha: Int,
    wmValueScale: Int,
    wmTextColorMode: Int,
    wmManualTextColor: Int,
    wmTextAlign: Int,
    onCaptureAspectChange: (CaptureAspect) -> Unit,
    onWatermarkDragPreview: (Int, Int) -> Unit,
    onWatermarkDragCommit: (Int, Int) -> Unit,
    onRowColWeightsChange: (TableTemplateState) -> Unit,
    onWidthRatioChange: (Int) -> Unit,
    onHeightRatioChange: (Int) -> Unit,
    onBgStyleChange: (Int) -> Unit,
    onBgAlphaChange: (Int) -> Unit,
    onValueScaleChange: (Int) -> Unit,
    onTextColorModeChange: (Int) -> Unit,
    onManualTextColorChange: (Int) -> Unit,
    onTextAlignChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val watermarkCells = remember(resolvedCells) { WatermarkBuilder.buildTableCells(resolvedCells) }
    var isWatermarkArmed by remember { mutableStateOf(false) }

    val scrollModifier = if (isWatermarkArmed) Modifier else Modifier.verticalScroll(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(scrollModifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clipToBounds()
                .background(DDZColor.Primary)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("촬영 미리보기", color = DDZColor.Surface, style = DDZTypography.CardTitle)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clipToBounds()
                    .background(DDZColor.Primary)
                    .border(1.dp, DDZColor.Border, RoundedCornerShape(10.dp))
            ) {
                CameraLikeWatermarkPlacementPreview(
                    captureAspect = captureAspect,
                    rows = templateState.rows,
                    cols = templateState.cols,
                    rowWeights = templateState.rowWeights,
                    colWeights = templateState.colWeights,
                    watermarkCells = watermarkCells,
                    anchor = wmAnchor,
                    offsetXRatio = wmOffsetXRatio,
                    offsetYRatio = wmOffsetYRatio,
                    tableWidthRatio = wmWidthRatio,
                    tableHeightRatio = wmHeightRatio,
                    bgStyle = wmBgStyle,
                    bgAlpha = wmBgAlpha,
                    valueScale = wmValueScale,
                    textColorMode = wmTextColorMode,
                    manualTextColor = wmManualTextColor,
                    textAlign = wmTextAlign,
                    armed = isWatermarkArmed,
                    onArmedChange = { isWatermarkArmed = it },
                    onDragPreview = onWatermarkDragPreview,
                    onDragCommit = onWatermarkDragCommit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "비율 설정",
                    color = DDZColor.Card,
                    style = DDZTypography.Caption
                )

                val aspectOptions = listOf(CaptureAspect.R1_1, CaptureAspect.R3_4, CaptureAspect.R9_16)
                val selectedIndex = aspectOptions.indexOf(captureAspect).coerceAtLeast(0)

                DDZSegmentedControl(
                    options = listOf("1:1", "3:4", "9:16"),
                    selectedIndex = selectedIndex,
                    onSelect = { index -> onCaptureAspectChange(aspectOptions[index]) }
                )
            }
        }

        TableRowColSizeSection(
            templateState = templateState,
            onTemplateChange = onRowColWeightsChange
        )

        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DDZColor.Card)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WatermarkPlacementSection(
                wmWidthRatio = wmWidthRatio,
                wmHeightRatio = wmHeightRatio,
                onWidthRatioChange = onWidthRatioChange,
                onHeightRatioChange = onHeightRatioChange
            )

            HorizontalDivider()

            TableStyleSection(
                wmBgStyle = wmBgStyle,
                onBgStyleChange = onBgStyleChange
            )

            HorizontalDivider()

            TableOpacitySection(
                wmBgAlpha = wmBgAlpha,
                onBgAlphaChange = onBgAlphaChange
            )

            HorizontalDivider()

            TableTextStyleSection(
                wmTextColorMode = wmTextColorMode,
                wmManualTextColor = wmManualTextColor,
                wmTextAlign = wmTextAlign,
                wmValueScale = wmValueScale,
                onTextColorModeChange = onTextColorModeChange,
                onManualTextColorChange = onManualTextColorChange,
                onTextAlignChange = onTextAlignChange,
                onValueScaleChange = onValueScaleChange
            )
        }
    }
}
