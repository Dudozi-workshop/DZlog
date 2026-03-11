package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.common.CounterAwareFileNameText
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CompactPathHeader(
    savePath: String,
    fileName: String,
    fileNameRightLabel: String? = null,
    onClickFileNamePreview: () -> Unit = {},
    onClickSavePathPreview: () -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelColumnWidth = remember(textMeasurer, density) {
        val savePathLabelWidth = with(density) {
            textMeasurer.measure(
                text = "저장경로",
                style = DDZTypography.Caption
            ).size.width.toDp()
        }
        val fileNameLabelWidth = with(density) {
            textMeasurer.measure(
                text = "파일명",
                style = DDZTypography.Caption
            ).size.width.toDp()
        }
        maxOf(savePathLabelWidth, fileNameLabelWidth)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Card, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "저장경로",
                modifier = Modifier.width(labelColumnWidth),
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 다음 단계 PATH 슬롯 편집으로 이어질 수 있게 프리뷰 텍스트 자체를 클릭 타겟으로 사용.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DDZColor.Surface.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(onClick = onClickSavePathPreview)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val valueStyle = DDZTypography.Body.copy(fontSize = 11.sp, lineHeight = 14.sp)
                // 경로 배지/프리뷰 불일치 방지: path slot 결과 문자열을 그대로 표시한다.
                val displayPath = remember(savePath) { savePath.ifBlank { "Pictures/DZlog/" }.trimEnd('/') }
                Text(
                    text = displayPath,
                    modifier = Modifier.fillMaxWidth(),
                    style = valueStyle,
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "파일명",
                modifier = Modifier.width(labelColumnWidth),
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 다음 단계 파일명 슬롯 편집으로 이어질 수 있게 프리뷰 텍스트 자체를 클릭 타겟으로 사용.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(DDZColor.Surface.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(onClick = onClickFileNamePreview)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CounterAwareFileNameText(
                    fileName = fileName,
                    modifier = Modifier.weight(1f),
                    style = DDZTypography.Body.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = DDZColor.TextPrimary,
                )
                fileNameRightLabel?.takeIf { it.isNotBlank() }?.let { label ->
                    Text(
                        text = label,
                        style = DDZTypography.Caption,
                        color = DDZColor.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
