package com.example.dzlog.ui.table

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun CameraLikeWatermarkPlacementPreview(
    captureAspect: CaptureAspect,
    watermarkCells: List<WatermarkCell>,
    rows: Int,
    cols: Int,
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableHeightRatio: Int,
    tableWidthRatio: Int,
    bgAlpha: Int,
    labelScale: Int,
    valueScale: Int
) {
    // 촬영 느낌: 레터박스(검정) + 중앙 프레임(캡처 비율) + 워터마크 오버레이
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // 프레임 자체는 최대 너비를 쓰되, 너무 커지지 않게 상한만 둔다.
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aspectRatio(captureAspect.ratioF)
                .border(1.dp, Color(0xFF3A3A3A))
                .background(Color(0xFF101010)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawIntoCanvas { canvas ->
                    val bounds = RectF(0f, 0f, size.width, size.height)
                    drawWatermarkTableOnCanvas(
                        canvas = canvas.nativeCanvas,
                        bounds = bounds,
                        cells = watermarkCells,
                        rows = rows,
                        cols = cols,
                        showLabel = false, // 촬영 프리뷰와 동일
                        anchor = anchor,
                        offsetXRatio = offsetXRatio,
                        offsetYRatio = offsetYRatio,
                        tableHeightRatio = tableHeightRatio,
                        tableWidthRatio = tableWidthRatio,
                        bgAlpha = bgAlpha,
                        labelScale = labelScale,
                        valueScale = valueScale
                    )
                }
            }

            // 가이드 텍스트(프레임 밖이 아니라 안쪽에 최소로)
            Text(
                text = "Preview ${captureAspect.label}",
                fontSize = 11.sp,
                color = Color(0xFFBDBDBD),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
    }
}
