package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
fun DDZSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 30.dp,
    horizontalPadding: Dp = 0.dp,
    verticalPadding: Dp = 0.dp,
    textStyle: TextStyle = DDZTypography.SegmentSmall
) {
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(DDZColor.Card)
            .border(1.dp, DDZColor.Border, shape),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selected) DDZColor.Surface else DDZColor.Card)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = textStyle,
                    color = if (selected) DDZColor.PrimaryDark else DDZColor.Primary,
                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
                )
            }

            if (index != options.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(DDZColor.Border)
                )
            }
        }
    }
}
