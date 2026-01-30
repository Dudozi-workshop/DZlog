package com.example.dzlog.ui.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.ui.common.DisplayTablePreview
import java.util.Date

/**
 * 축소 표 미리보기.
 * - "표만"(사진 합성 없음)
 * - 카메라 오버레이와 동일 렌더(drawWatermarkTableOnCanvas) 재사용
 * - 배경은 밝은 회색으로 고정(검정/투명 표도 가시성 확보)
 */
@Composable
fun MiniTablePreview(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier
) {
    val rows = templateState.rows.coerceAtLeast(1)
    val cols = templateState.cols.coerceAtLeast(1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cols / rows.toFloat())
            .heightIn(min = 96.dp, max = 200.dp)
    ) {
        DisplayTablePreview(
            templateState = templateState,
            counterDigits = counterDigits,
            now = now,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
