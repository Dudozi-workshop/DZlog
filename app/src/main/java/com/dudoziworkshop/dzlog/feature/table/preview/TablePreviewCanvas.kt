package com.dudoziworkshop.dzlog.feature.table.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.feature.table.render.TableRender
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
fun TablePreviewCanvas(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
    // 워터마크 표 배경 스타일(0=BLACK, 1=WHITE, 2=TRANSPARENT)
    wmBgStyle: Int = 0,
    // 워터마크 표 배경 투명도(0~255)
    wmBgAlpha: Int = 80,
    // 워터마크 표 값 글씨크기(60~160, 기본 100)
    wmValueScale: Int = 100,
    // 워터마크 표 텍스트 색상 모드(0=AUTO, 1=MANUAL)
    wmTextColorMode: Int = 0,
    // 워터마크 표 수동 텍스트 색상(0=WHITE, 1=BLACK)
    wmManualTextColor: Int = 1,
    // 워터마크 표 텍스트 정렬(0=LEFT, 1=CENTER, 2=RIGHT)
    wmTextAlign: Int = 0,
    // 워터마크 표 그리드 표시 여부
    tableDetailGridEnabled: Boolean = true,
    overlay: (@Composable () -> Unit)? = null,
) {
    // ✅ 프리뷰는 "표를 감싸는 내부 사각틀"이 실제 표 렌더 크기와 일치해야 한다.
    // drawWatermarkTableOnCanvas는 tableH를 width(base) 기준으로 계산하며,
    // tableHeightRatio는 (10..100)로 clamp 된다.
    // 홈 프리뷰 frame 종횡비도 내부 표(2:1)와 맞춰 셀 비율/텍스트 크기 체감을 촬영과 가깝게 유지한다.
    val frameAspectRatio = TableLayoutCalculator.defaultAspectRatio

    TablePreviewFrame(
        aspectRatio = frameAspectRatio,
        modifier = modifier
    ) { innerModifier ->
        // ✅ 홈 프리뷰 정책: 스타일은 촬영 프리뷰와 동일 경로를 사용하고,
        // 배치는 중앙/고정(회전/앵커/오프셋/크기비율 미반영)으로 유지한다.
        TableRender(
            templateState = templateState,
            counterDigits = counterDigits,
            now = now,
            bgStyle = wmBgStyle.coerceIn(0, 2),
            bgAlpha = wmBgAlpha.coerceIn(0, 255),
            valueScale = wmValueScale.coerceIn(60, 160),
            textColorMode = wmTextColorMode,
            manualTextColor = wmManualTextColor,
            textAlign = wmTextAlign,
            gridEnabled = tableDetailGridEnabled,
            modifier = innerModifier
                .fillMaxSize()
                // 표 가독성을 위한 미세 여백 (프리뷰 전용)
                .padding(4.dp),
        )
        overlay?.invoke()
    }
}

@Composable
fun TablePreview(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
    wmBgStyle: Int = 0,
    wmBgAlpha: Int = 80,
    wmValueScale: Int = 100,
    wmTextColorMode: Int = 0,
    wmManualTextColor: Int = 1,
    wmTextAlign: Int = 0,
    tableDetailGridEnabled: Boolean = true,
    overlay: (@Composable () -> Unit)? = null,
) {
    TablePreviewCanvas(
        templateState = templateState,
        counterDigits = counterDigits,
        now = now,
        modifier = modifier,
        wmBgStyle = wmBgStyle,
        wmBgAlpha = wmBgAlpha,
        wmValueScale = wmValueScale,
        wmTextColorMode = wmTextColorMode,
        wmManualTextColor = wmManualTextColor,
        wmTextAlign = wmTextAlign,
        tableDetailGridEnabled = tableDetailGridEnabled,
        overlay = overlay,
    )
}
