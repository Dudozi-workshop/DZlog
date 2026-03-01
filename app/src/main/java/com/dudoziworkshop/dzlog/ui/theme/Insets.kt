package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun dzTopInset(): Dp {
    val density = LocalDensity.current
    val topPx = WindowInsets.statusBars.getTop(density)
    return with(density) { topPx.toDp() }
}
