package com.example.dzlog.ui.common

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import java.util.Date

/**
 * "표만" 미리보기 렌더.
 * - 사진 합성 없음
 * - 카메라 오버레이와 동일한 워터마크 테이블 렌더러(drawWatermarkTableOnCanvas) 재사용
 * - 미리보기 컨테이너 배경은 밝은 회색으로 고정(가시성 확보)
 */
@Composable
fun TableOnlyPreview(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
    // 일반 미리보기(홈/설정/촬영 등)는 "실제 표"만 보여야 하므로 기본은 label 숨김.
    // (표 상세설정 편집 화면에서만 showLabel=true 로 사용)
    showLabel: Boolean = false,
    bgColor: Color = Color(0xFFF2F2F2),
    // 미리보기에서는 전체 영역에 표를 최대한 크게 보여주는 쪽이 직관적임
    tableWidthRatio: Int = 92,
    tableHeightRatio: Int = 92,
    bgAlpha: Int = 210,
    labelScale: Int = 100,
    valueScale: Int = 100,
) {
    val resolver = remember { TableResolver() }
    val plan = remember(templateState, now, counterDigits) {
        resolver.plan(
            cells = templateState.cells,
            captureNow = now,
            config = TableResolver.Config(
                counterDigits = counterDigits,
                dateFormat = "yyyy.MM.dd",
                timeFormat = "HH:mm:ss"
            )
        )
    }

    val cells = remember(plan.resolvedCells) { WatermarkBuilder.buildTableCells(plan.resolvedCells) }

    Box(modifier = modifier.background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                // bounds: Canvas 전체
                val bounds = RectF(0f, 0f, size.width, size.height)
                drawWatermarkTableOnCanvas(
                    canvas = canvas.nativeCanvas,
                    bounds = bounds,
                    cells = cells,
                    rows = templateState.rows,
                    cols = templateState.cols,
                    showLabel = showLabel,
                    anchor = com.example.dzlog.domain.model.WatermarkTableAnchor.TOP_LEFT,
                    offsetXRatio = 4,
                    offsetYRatio = 4,
                    tableHeightRatio = tableHeightRatio,
                    tableWidthRatio = tableWidthRatio,
                    bgAlpha = bgAlpha,
                    labelScale = labelScale,
                    valueScale = valueScale
                )
            }

            // 외곽선(회색 배경 위에서 영역 구분)
            val stroke = 1f
            drawLine(Color(0x33000000), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = stroke)
            drawLine(Color(0x33000000), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke)
            drawLine(Color(0x33000000), Offset(0f, 0f), Offset(0f, size.height), strokeWidth = stroke)
            drawLine(Color(0x33000000), Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = stroke)
        }
    }
}
