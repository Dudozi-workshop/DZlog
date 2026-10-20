package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing

@Composable
fun DDZCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(LocalDDZSpacing.current.cardPadding),
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit
) {
    val colors = LocalDDZColor.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.Card, shape)
            .border(width = 1.dp, color = colors.Border, shape = shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
