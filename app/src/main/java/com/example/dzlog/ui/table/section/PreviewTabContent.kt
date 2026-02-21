package com.example.dzlog.ui.table.section

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.table.ResolvedCell
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.table.watermark.CameraLikeWatermarkPlacementPreview
import com.example.dzlog.ui.table.TableRowColSizeSection
import com.example.dzlog.ui.theme.DDZColor

/**
 * 탭1(표 미리보기) UI 전용 컴포넌트.
 * - 상태/저장(DataStore) 로직은 Screen이 소유
 * - 여기서는 UI 조립만 담당
 */
@Composable
fun PreviewTabContent(
    scrollState: ScrollState,
    captureAspect: CaptureAspect,
    templateState: TableTemplateState,
    resolvedCells: List<ResolvedCell>,
    wmAnchor: WatermarkTableAnchor,
    wmWidthRatio: Int,
    wmHeightRatio: Int,
    wmBgStyle: Int,
    wmBgAlpha: Int,
    wmValueScale: Int,
    wmTextColorMode: Int,
    wmManualTextColor: Int,
    wmTextAlign: Int,
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
    // ✅ 프리뷰 렌더용 셀 목록은 resolvedCells가 바뀔 때만 재계산
    val watermarkCells = remember(resolvedCells) { WatermarkBuilder.buildTableCells(resolvedCells) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CameraLikeWatermarkPlacementPreview(
            captureAspect = captureAspect,
            rows = templateState.rows,
            cols = templateState.cols,
            rowWeights = templateState.rowWeights,
            colWeights = templateState.colWeights,
            watermarkCells = watermarkCells,
            anchor = wmAnchor,
            tableWidthRatio = wmWidthRatio,
            tableHeightRatio = wmHeightRatio,
            bgStyle = wmBgStyle,
            bgAlpha = wmBgAlpha,
            valueScale = wmValueScale,
            textColorMode = wmTextColorMode,
            manualTextColor = wmManualTextColor,
            textAlign = wmTextAlign
        )

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
