package com.dudoziworkshop.dzlog.ui.common.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
fun EmptyHint(
    modifier: Modifier = Modifier,
    text: String = "항목을 추가해주세요."
) {
    Text(
        text = text,
        modifier = modifier,
        color = DDZColor.TextMuted,
        style = DDZTypography.Body
    )
}
