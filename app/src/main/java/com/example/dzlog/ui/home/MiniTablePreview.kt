package com.example.dzlog.ui.home

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
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.ui.common.TablePreview
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import java.util.Date

/**
 * 홈 화면용 "표 미리보기"
 * - 프리뷰 경로는 TablePreview 단일 진입점으로 통일
 */
@Composable
fun MiniTablePreview(
    templateState: TableTemplateState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val now = Date()

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
            // 홈/전체설정 모두 동일 경로: TablePreview
            TablePreview(
                templateState = templateState,
                counterDigits = 0,
                now = now,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
