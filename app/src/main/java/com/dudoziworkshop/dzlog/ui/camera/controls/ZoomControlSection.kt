package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ZoomControlSection(
    zoomRatioTenths: Int,
    maxZoomTenths: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onZoomTenthsChange: (Int) -> Unit
) {
    val normalizedMaxTenths = maxZoomTenths.coerceAtLeast(10)
    val normalizedTenths = zoomRatioTenths.coerceIn(10, normalizedMaxTenths)
    val zoomLabel = String.format(Locale.US, "%.1f", normalizedTenths / 10f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                color = DDZColor.PrimaryDark.copy(alpha = 0f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 42.dp, minHeight = 30.dp)
                .background(
                    color = DDZColor.Card.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(999.dp)
                )
                .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(999.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = zoomLabel,
                color = DDZColor.TextStrong,
                style = DDZTypography.Caption
            )
        }

        if (expanded) {
            Row(
                modifier = Modifier.padding(start = DDZSpacing.itemGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    modifier = Modifier.width(136.dp),
                    value = normalizedTenths / 10f,
                    onValueChange = {
                        val stepped = (it * 10f).roundToInt().coerceIn(10, normalizedMaxTenths)
                        onZoomTenthsChange(stepped)
                    },
                    valueRange = 1f..(normalizedMaxTenths / 10f),
                    steps = (normalizedMaxTenths - 10).coerceAtLeast(1) - 1
                )
            }
        }
    }
}
