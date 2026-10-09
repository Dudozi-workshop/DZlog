package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CounterAwareFileNameText(
    fileName: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    counterColor: Color = color,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val parts = splitFileNameForDisplay(fileName)
        val counter = parts.counter

        if (counter.isNullOrBlank()) {
            Text(
                text = parts.prefixText,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            return@BoxWithConstraints
        }

        val counterText = "_$counter"
        val counterWidthPx = textMeasurer.measure(
            text = counterText,
            style = style,
        ).size.width
        val counterWidth = (counterWidthPx / density.density).dp
        val prefixMaxWidth = (this@BoxWithConstraints.maxWidth - counterWidth).coerceAtLeast(0.dp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = parts.prefixText,
                modifier = Modifier.widthIn(max = prefixMaxWidth),
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = counterText,
                style = style,
                color = counterColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
