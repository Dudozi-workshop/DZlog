package com.example.dzlog.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * 표 프리뷰 컨테이너(프레임).
 *
 * - 부모 박스 크기가 화면마다 달라도, [aspectRatio]를 유지한 채 center-fit으로 그릴 수 있게 함
 * - 남는 공간은 레터박스(여백)로 두며, 실제 표는 중앙 정렬됨
 * - 렌더러(표를 그리는 컴포저블)는 "주어진 크기" 안에 그리기만 하면 됨
 */
@Composable
fun TablePreviewFrame(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    minInnerSize: Dp = 0.dp,
    content: @Composable (innerModifier: Modifier) -> Unit
) {
    val safeRatio = if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else 1f

    BoxWithConstraints(modifier = modifier) {
        val maxW = maxWidth
        val maxH = maxHeight

        // 사용 가능한 박스 안에서 center-fit(비율 유지)로 innerSize 계산
        val fitByWidthH = if (safeRatio == 0f) maxH else maxW / safeRatio
        val useWidth = fitByWidthH <= maxH

        val innerW = if (useWidth) maxW else maxH * safeRatio
        val innerH = if (useWidth) fitByWidthH else maxH

        val minSize = max(minInnerSize.value, 0f).dp
        val finalW = if (innerW < minSize) minSize else innerW
        val finalH = if (innerH < minSize) minSize else innerH

        // 콘텐츠는 "딱 맞는 inner box" 안에서만 그림
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            // 외부 BoxWithConstraints가 이미 clip/배경 등을 담당할 수 있게,
            // 여기서는 크기만 제한한 박스를 중앙에 배치한다.
            Box(
                modifier = Modifier
                    .width(finalW)
                    .height(finalH)
            ) {
                content(Modifier.fillMaxSize())
            }
        }
    }
}
