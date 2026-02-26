package com.dudoziworkshop.dzlog.ui.common

import android.graphics.RectF
import androidx.annotation.IntRange
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
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.watermark.drawWatermarkTableOnCanvas
import java.util.Date

/**
 * TableRender
 * - "표 + 값" 만 보여주는 순수 렌더러 (홈/설정/촬영/로그 등 공용)
 * - 라벨/배지/경고색/편집 UI는 절대 포함하지 않음
 */
@Composable
fun TableRender(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
    // 배경이 투명/검정인 테이블도 항상 보이도록, 기본 배경은 밝은 회색
    bgColor: Color = Color(0xFFF2F2F2),
    // 워터마크 표 배경 스타일(0=BLACK, 1=WHITE, 2=TRANSPARENT)
    @IntRange(from = 0, to = 2) bgStyle: Int = 0,
    bgAlpha: Int = 210,
    valueScale: Int = 100,
) {
    // TableRender는 공용 프리뷰 렌더러로, 표 영역은 고정 프리뷰 비율을 사용한다.
    val tableWidthRatio = 100
    val tableHeightRatio = 35
    val offsetXRatio = 0
    val offsetYRatio = 0

    val resolver = remember { TableResolver() }
    val plan = remember(templateState, now, counterDigits) {
        resolver.plan(
            cells = templateState.cells,
            captureNow = now,
            config = TableResolver.Config(
                counterDigits = counterDigits,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_RENDER_COLON
            ),
            phraseSets = templateState.phraseSets
        )
    }

    val cells = remember(plan.resolvedCells) { WatermarkBuilder.buildTableCells(plan.resolvedCells) }

    Box(modifier = modifier.background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val bounds = RectF(0f, 0f, size.width, size.height)
                drawWatermarkTableOnCanvas(
                    canvas = canvas.nativeCanvas,
                    bounds = bounds,
                    cells = cells,
                    rows = templateState.rows,
                    cols = templateState.cols,
                    anchor = WatermarkTableAnchor.TOP_LEFT,
                    offsetXRatio = offsetXRatio,
                    offsetYRatio = offsetYRatio,
                    tableHeightRatio = tableHeightRatio,
                    tableWidthRatio = tableWidthRatio,
                    bgAlpha = bgAlpha,
                    bgStyle = bgStyle,
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
