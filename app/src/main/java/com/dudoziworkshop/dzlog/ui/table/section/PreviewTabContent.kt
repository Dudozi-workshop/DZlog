package com.dudoziworkshop.dzlog.ui.table.section

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControl
import com.dudoziworkshop.dzlog.ui.table.TableRowColSizeSection
import com.dudoziworkshop.dzlog.ui.table.watermark.CameraLikeWatermarkPlacementPreview
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

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
    wmGridEnabled: Boolean,
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
    onGridEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val watermarkCells = remember(resolvedCells) { WatermarkBuilder.buildTableCells(resolvedCells) }
    var isWatermarkArmed by remember { mutableStateOf(false) }

    val settingsScrollModifier = if (isWatermarkArmed) Modifier else Modifier.verticalScroll(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
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
                    drawGrid = wmGridEnabled,
                    armed = isWatermarkArmed,
                    onArmedChange = { isWatermarkArmed = it },
                    onDragPreview = onWatermarkDragPreview,
                    onDragCommit = onWatermarkDragCommit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                Text(
                    text = "비율 설정",
                    color = DDZColor.Card,
                    style = DDZTypography.Caption,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                val aspectOptions = listOf(CaptureAspect.R1_1, CaptureAspect.R3_4, CaptureAspect.R9_16)
                val selectedIndex = aspectOptions.indexOf(captureAspect).coerceAtLeast(0)

                DDZSegmentedControl(
                    options = listOf("1:1", "3:4", "9:16"),
                    selectedIndex = selectedIndex,
                    onSelect = { index -> onCaptureAspectChange(aspectOptions[index]) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(104.dp)
                        .height(24.dp),
                    horizontalPadding = 2.dp,
                    verticalPadding = 1.dp,
                    textStyle = DDZTypography.Caption
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(settingsScrollModifier)
                .background(DDZColor.Card)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                TableRowColSizeSection(
                    templateState = templateState,
                    onTemplateChange = onRowColWeightsChange
                )

                Spacer(Modifier.height(4.dp))

                WatermarkPlacementSection(
                    wmWidthRatio = wmWidthRatio,
                    wmHeightRatio = wmHeightRatio,
                    onWidthRatioChange = onWidthRatioChange,
                    onHeightRatioChange = onHeightRatioChange
                )

                HorizontalDivider()

                TableStyleSection(
                    wmBgStyle = wmBgStyle,
                    wmGridEnabled = wmGridEnabled,
                    onBgStyleChange = onBgStyleChange,
                    onGridEnabledChange = onGridEnabledChange
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
