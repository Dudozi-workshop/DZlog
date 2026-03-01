package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.floor

@Composable
fun estimateBudget(
    availWidth: Dp,
    textStyle: TextStyle,
    minBudget: Int,
    maxBudget: Int,
    margin: Int = 2,
): Int {
    val density = LocalDensity.current
    val fontSizeSp = if (textStyle.fontSize.value > 0f) textStyle.fontSize.value else 14f
    val avgCharDp = (fontSizeSp * 0.55f * density.fontScale).dp
    val raw = if (avgCharDp.value > 0f) floor(availWidth.value / avgCharDp.value).toInt() else minBudget
    return (raw - margin).coerceIn(minBudget, maxBudget)
}
