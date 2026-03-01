package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DDZ_TOP_ADJUST = 0.dp

@Composable
fun dzTopInset(): Dp {
    val density = LocalDensity.current
    val topPx = WindowInsets.statusBars.getTop(density)
    val topDp = with(density) { topPx.toDp() }
    return (topDp - DDZ_TOP_ADJUST).coerceAtLeast(0.dp)
}
