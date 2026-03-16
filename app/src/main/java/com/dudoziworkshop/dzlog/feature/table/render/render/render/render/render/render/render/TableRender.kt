package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.RectF
import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import java.util.Date


/**
 * TableRender
 * - "표 자체 속성"(구조/내부분배/서식)만 렌더하는 순수 렌더러.
 * - Design Preview(상세/홈)와 Camera Preview(미리보기/촬영)가 공통으로 사용한다.
 * - 위치/회전 같은 촬영 배치 속성은 placement에서 주입한다.
 */
@Composable
fun TableRender(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
    // 배경이 투명/검정인 테이블도 항상 보이도록, 기본 배경은 밝은 회색
    bgColor: Color = Color(0xFFF7F4EF),
    // 워터마크 표 배경 스타일(0=BLACK, 1=WHITE, 2=TRANSPARENT)
    @IntRange(from = 0, to = 2) bgStyle: Int = 0,
    bgAlpha: Int = 210,
    valueScale: Int = 100,
    textColorMode: Int = WatermarkTextColorMode.AUTO,
    manualTextColor: Int = WatermarkManualTextColor.BLACK,
    textAlign: Int = WatermarkTextAlign.LEFT,
    gridEnabled: Boolean = true,
    tableWidthRatio: Int = 40,
    tableHeightRatio: Int = 20,
) {
    val resolver = remember { TableResolver() }
    val plan = remember(templateState, now, counterDigits) {
        val selectedPhraseTextByCellId = PhraseResolver.resolveSelectedTextByCellId(
            cells = templateState.cells,
            phraseSets = templateState.phraseSets,
            progressCursor = 1,
        )
        resolver.plan(
            cells = templateState.cells,
            captureNow = now,
            config = TableResolver.Config(
                counterDigits = counterDigits,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_RENDER_COLON
            ),
            selectedPhraseTextByCellId = selectedPhraseTextByCellId,
        )
    }

    val cells = remember(plan.resolvedCells) { WatermarkBuilder.buildTableCells(plan.resolvedCells) }

    Box(modifier = modifier.background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val bounds = RectF(0f, 0f, size.width, size.height)
                val ratio = computeDesignPreviewFitShape(
                    boundsWidth = bounds.width(),
                    boundsHeight = bounds.height(),
                    tableWidthRatio = tableWidthRatio,
                    tableHeightRatio = tableHeightRatio,
                    cellCount = templateState.cells.size,
                )
                TableRenderAdapter.draw(
                    canvas = canvas.nativeCanvas,
                    bounds = bounds,
                    payload = TableRenderPayload(
                        rows = templateState.rows,
                        cols = templateState.cols,
                        rowWeights = templateState.rowWeights,
                        colWeights = templateState.colWeights,
                        cells = cells,
                    ),
                    style = TableRenderStyle(
                        bgStyle = bgStyle,
                        bgAlpha = bgAlpha,
                        valueScale = valueScale,
                        textColorMode = textColorMode,
                        manualTextColor = manualTextColor,
                        textAlign = textAlign,
                        drawGrid = gridEnabled,
                    ),
                    placement = buildDesignPreviewPlacement(
                        tableWidthRatio = ratio.tableWidthRatio,
                        tableHeightRatio = ratio.tableHeightRatio,
                    ),
                )
            }

        }
    }
}
