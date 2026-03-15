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
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import java.util.Date

private const val HOME_MIN_CONTENT_ASPECT_RATIO = 0.5f
private const val HOME_MAX_CONTENT_ASPECT_RATIO = 2.0f
private const val HOME_MIN_WIDTH_RATIO = 30
private const val HOME_MIN_HEIGHT_RATIO = 30
private const val HOME_MAX_FILL_RATIO = 95

internal fun computeHomePreviewRatio(
    contentAspectRatio: Float,
    boundsWidth: Float,
    boundsHeight: Float,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
): TableShapeRatios {
    val safeAspect = contentAspectRatio.coerceIn(HOME_MIN_CONTENT_ASPECT_RATIO, HOME_MAX_CONTENT_ASPECT_RATIO)
    val safeWidth = boundsWidth.coerceAtLeast(1f)
    val safeHeight = boundsHeight.coerceAtLeast(1f)
    val maxHeightByBounds = ((safeHeight / safeWidth) * 100f).toInt().coerceAtLeast(HOME_MIN_HEIGHT_RATIO)

    return computeShapeLockedRatios(
        contentAspectRatio = safeAspect,
        maxWidthRatio = tableWidthRatio.coerceIn(HOME_MIN_WIDTH_RATIO, HOME_MAX_FILL_RATIO),
        maxHeightRatio = minOf(tableHeightRatio.coerceIn(HOME_MIN_HEIGHT_RATIO, HOME_MAX_FILL_RATIO), maxHeightByBounds.coerceAtMost(HOME_MAX_FILL_RATIO)),
        minWidthRatio = HOME_MIN_WIDTH_RATIO,
        minHeightRatio = HOME_MIN_HEIGHT_RATIO,
        hardMaxRatio = HOME_MAX_FILL_RATIO,
    )
}

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
    val contentAspectRatio = resolveContentAspectRatio(templateState)

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
                val ratio = computeHomePreviewRatio(
                    contentAspectRatio = contentAspectRatio,
                    boundsWidth = bounds.width(),
                    boundsHeight = bounds.height(),
                    tableWidthRatio = tableWidthRatio,
                    tableHeightRatio = tableHeightRatio,
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
                    placement = TableRenderPlacement(
                        // 홈 프리뷰는 placement/rotation 문맥을 배제하고,
                        // 카드 내부에서 center-fit 된 "표 자체"만 보여준다.
                        anchor = WatermarkTableAnchor.CUSTOM,
                        offsetXRatio = 50,
                        offsetYRatio = 50,
                        tableHeightRatio = ratio.tableHeightRatio,
                        tableWidthRatio = ratio.tableWidthRatio,
                        rotationCwDeg = 0,
                    ),
                )
            }

        }
    }
}
