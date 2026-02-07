package com.example.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.ui.common.TableRender
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import java.util.Date

/**
 * 홈 화면용 "표 미리보기"
 * - 표는 "하나의 덩어리"로 렌더링(설정 화면과 동일 계열 TableRender)
 * - 박스 크기가 달라도 종횡비(aspectRatio) 유지 + center-fit
 */
@Composable
fun MiniTablePreview(
    templateState: TableTemplateState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rows = templateState.rows.coerceAtLeast(1)
    val cols = templateState.cols.coerceAtLeast(1)
    val ratio = cols / rows.toFloat()

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxSize()
    ) {
        Text(
            text = "표 미리보기",
            style = DDZTypography.CardTitle,
            color = DDZColor.TextPrimary
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .weight(1f, fill = true)
                .background(DDZColor.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, DDZColor.Border, RoundedCornerShape(10.dp))
                .padding(6.dp)
        ) {
            // "박스 안에서 표 전체 비율 유지"가 목적이므로,
            // inner box를 aspectRatio로 만들고 중앙 배치한다(레터박스 허용).
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                ) {
                    TableRender(
                        templateState = templateState,
                        counterDigits = 0,
                        now = Date(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
