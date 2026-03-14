package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Date

/**
 * TablePreviewCard
 *
 * - 화면(Home/Settings 등)에서 "프리뷰 카드 UI"를 공통으로 사용하기 위한 컴포넌트.
 * - 실제 프리뷰 렌더 경로는 TablePreview 단일 진입점을 사용한다.
 *
 * ✅ 규칙
 * - 표를 그리는 로직(TableRender/TablePreviewFrame)은 여기서 직접 다루지 않는다.
 * - 화면에서는 TablePreviewCard만 호출하고, 프리뷰 구현 변경은 common에서만 수행한다.
 */
@Composable
fun TablePreviewCard(
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
    title: String? = null,
    onClick: (() -> Unit)? = null,
    // 홈은 박스(테두리/패딩) 스타일이 필요하고, 설정은 단순 프리뷰만 필요한 경우가 있어 옵션 제공
    chrome: Boolean = true,
    // 프리뷰 영역의 높이/크기 제약을 화면별로 주입
    // - 홈: Modifier.fillMaxSize()
    // - 설정(스크롤 화면): Modifier.heightIn(...)
    previewModifier: Modifier = Modifier,
) {
    val root = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    val topPad = if (title != null) 8.dp else 0.dp

    Column(modifier = root) {
        if (title != null) {
            Text(
                text = title,
                style = DDZTypography.CardTitle,
                color = DDZColor.TextPrimary
            )
        }

        if (chrome) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPad)
                    .background(DDZColor.Surface, RoundedCornerShape(10.dp))
                    .border(1.dp, DDZColor.Border, RoundedCornerShape(10.dp))
                    .padding(6.dp)
                    .then(previewModifier)
            ) {
                TablePreview(
                    templateState = templateState,
                    counterDigits = counterDigits,
                    now = now,
                    wmBgStyle = wmBgStyle,
                    wmBgAlpha = wmBgAlpha,
                    wmValueScale = wmValueScale,
                    wmTextColorMode = wmTextColorMode,
                    wmManualTextColor = wmManualTextColor,
                    wmTextAlign = wmTextAlign,
                    tableDetailGridEnabled = tableDetailGridEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // 설정 화면 등: 별도 박스 크롬 없이 프리뷰만 보여주고 싶을 때 사용
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPad)
                    .then(previewModifier)
            ) {
                TablePreview(
                    templateState = templateState,
                    counterDigits = counterDigits,
                    now = now,
                    wmBgStyle = wmBgStyle,
                    wmBgAlpha = wmBgAlpha,
                    wmValueScale = wmValueScale,
                    wmTextColorMode = wmTextColorMode,
                    wmManualTextColor = wmManualTextColor,
                    wmTextAlign = wmTextAlign,
                    tableDetailGridEnabled = tableDetailGridEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
