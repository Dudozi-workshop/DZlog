package com.example.dzlog.ui.table.watermark

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas

@Composable
internal fun CameraLikeWatermarkPlacementPreview(
    captureAspect: CaptureAspect,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    watermarkCells: List<WatermarkBuilder.WatermarkCell>,
    anchor: WatermarkTableAnchor,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    bgStyle: Int,
    bgAlpha: Int,
    valueScale: Int
) {
    // bgStyle: 워터마크 표 배경 스타일
    // - 0: BLACK
    // - 1: WHITE
    // - 2: TRANSPARENT (배경 렌더링 안 함)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Primary)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("촬영 미리보기", color = DDZColor.Surface, style = DDZTypography.CardTitle)

        // 카메라 프레임(비율 반영) 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(captureAspect.ratioF)
                .background(DDZColor.Primary)
                .border(1.dp, DDZColor.Border)
                .clipToBounds()
        ) {
            // 실제 카메라 영상 대신 “프레임 느낌” 배경 (단색+가이드 정도)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 아주 약한 가이드(중앙 십자선) – 원하면 나중에 제거 가능
                val w = size.width
                val h = size.height

                // center lines
                drawRect(
                    color = DDZColor.Surface.copy(alpha = 0.13f),
                    topLeft = Offset(w / 2f - 0.5f, 0f),
                    size = Size(1f, h)
                )
                drawRect(
                    color = DDZColor.Surface.copy(alpha = 0.13f),
                    topLeft = Offset(0f, h / 2f - 0.5f),
                    size = Size(w, 1f)
                )

                // 워터마크 표 오버레이 (CameraScreen과 동일 엔진)
                drawIntoCanvas { canvas ->
                    val bounds = RectF(0f, 0f, w, h)

                    drawWatermarkTableOnCanvas(
                        canvas = canvas.nativeCanvas,
                        bounds = bounds,
                        cells = watermarkCells,
                        rows = rows.coerceAtLeast(1),
                        cols = cols.coerceAtLeast(1),
                        showLabel = false,
                        anchor = anchor,
                        offsetXRatio = 0,
                        offsetYRatio = 0,
                        tableHeightRatio = tableHeightRatio,
                        tableWidthRatio = tableWidthRatio,
                        bgAlpha = bgAlpha.coerceIn(0, 255),
                        bgStyle = bgStyle,
                        labelScale = 100,
                        valueScale = valueScale.coerceIn(60, 160),
                        rowWeights = rowWeights,
                        colWeights = colWeights
                    )
                }
            }
        }

        Text(
            "비율: ${captureAspect.label} / 크기: ${tableWidthRatio}%×${tableHeightRatio}% (위치는 카메라 화면에서 드래그)",
            color = DDZColor.IconMuted,
            style = DDZTypography.Caption
        )
    }
}
