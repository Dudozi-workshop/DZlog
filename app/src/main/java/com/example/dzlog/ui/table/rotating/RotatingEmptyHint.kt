package com.example.dzlog.ui.table.rotating

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
fun RotatingEmptyHint(
    text: String = "항목을 추가해주세요.",
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = DDZColor.TextMuted,
        style = DDZTypography.Body
    )
}
