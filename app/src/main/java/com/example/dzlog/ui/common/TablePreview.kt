package com.example.dzlog.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    // 워터마크 표 배경 스타일(0=BLACK, 1=WHITE, 2=TRANSPARENT)
    wmBgStyle: Int = 0,
    // 워터마크 표 배경 투명도(0~255)
    wmBgAlpha: Int = 80,
    // 워터마크 표 값 글씨크기(60~160, 기본 100)
    wmValueScale: Int = 100,
    modifier: Modifier = Modifier,
) {
    // ✅ 프리뷰는 "표를 감싸는 내부 사각틀"이 실제 표 렌더 크기와 일치해야 한다.
    // drawWatermarkTableOnCanvas는 tableH를 width(base) 기준으로 계산하며,
    // tableHeightRatio는 (10..35)로 clamp 된다.
    val previewTableWidthRatio = 100
    val previewTableHeightRatio = 35
    val frameAspectRatio = previewTableWidthRatio / previewTableHeightRatio.toFloat()
    RoundedCornerShape(8.dp)

    TablePreviewFrame(
        aspectRatio = frameAspectRatio,
        modifier = modifier
    ) { innerModifier ->
        // ✅ 프리뷰는 "내부 박스에 딱 맞는 표 + 최소 틈"을 목표로 함
        // - 렌더 내부 축소(92%)와 오프셋(4%)을 제거하고
        // - 프리뷰 레벨의 dp padding으로만 미세 여백을 제공
        TableRender(
            templateState = templateState,
            counterDigits = counterDigits,
            now = now,
            bgStyle = wmBgStyle.coerceIn(0, 2),
            modifier = innerModifier
                .fillMaxSize()
                // 표 가독성을 위한 미세 여백 (프리뷰 전용)
                .padding(4.dp),
            tableWidthRatio = previewTableWidthRatio,
            tableHeightRatio = previewTableHeightRatio,
            offsetXRatio = 0,
            offsetYRatio = 0,
            bgAlpha = wmBgAlpha.coerceIn(0, 255),
            valueScale = wmValueScale.coerceIn(60, 160),
        )
    }
}
