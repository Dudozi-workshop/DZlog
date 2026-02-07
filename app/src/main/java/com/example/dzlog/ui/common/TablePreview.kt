package com.example.dzlog.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dzlog.domain.model.TableTemplateState
import java.util.Date

/**
 * TablePreview (공통 프리뷰 진입점)
 *
 * - 표를 "하나의 덩어리"로 렌더링(TableRender)
 * - 컨테이너 크기와 무관하게 표 종횡비를 유지(center-fit, 레터박스 허용)
 *
 * ✅ 규칙
 * - 화면(Home/Settings 등)은 이 Composable만 호출한다.
 * - 프레임(TablePreviewFrame)과 렌더(TableRender) 조합은 여기서만 관리한다.
 */
@Composable
fun TablePreview(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
) {
    val rows = templateState.rows.coerceAtLeast(1)
    val cols = templateState.cols.coerceAtLeast(1)

    TablePreviewFrame(
        aspectRatio = cols / rows.toFloat(),
        modifier = modifier
    ) { innerModifier ->
        TableRender(
            templateState = templateState,
            counterDigits = counterDigits,
            now = now,
            modifier = innerModifier.fillMaxSize()
        )
    }
}
