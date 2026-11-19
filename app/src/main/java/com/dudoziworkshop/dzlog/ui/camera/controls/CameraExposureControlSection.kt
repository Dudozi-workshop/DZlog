package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlin.math.roundToInt

/**
 * CameraX AE exposure compensation. Not a dark overlay: the captured image
 * receives the same physical exposure adjustment as the preview.
 * This control does not request an AF lock or switch into manual focus.
 */
@Composable
internal fun CameraExposureControlSection(
    currentIndex: Int,
    minIndex: Int,
    maxIndex: Int,
    stepEv: Float,
    supported: Boolean,
    onIndexChange: (Int) -> Unit,
) {
    val usable = supported && minIndex < maxIndex
    val minValue = if (usable) minIndex.toFloat() else 0f
    val maxValue = if (usable) maxIndex.toFloat() else 1f
    val actual = cameraExposureIndex(currentIndex, usable, minIndex, maxIndex)
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .background(DDZColor.Surface.copy(alpha = 0.96f), shape)
            .border(1.dp, DDZColor.SageBorder, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Default.Brightness6, contentDescription = "전체 노출", tint = DDZColor.SageDarkStrong)
        Text(
            text = if (usable) cameraExposureLabel(actual, stepEv) else "EV 미지원",
            color = DDZColor.SageDarkStrong,
            style = DDZTypography.Caption,
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = if (usable) actual.toFloat() else 0f,
            valueRange = minValue..maxValue,
            enabled = usable,
            onValueChange = { newValue ->
                if (usable) onIndexChange(newValue.roundToInt().coerceIn(minIndex, maxIndex))
            },
        )
        TextButton(
            enabled = usable && actual != 0,
            onClick = { onIndexChange(0) },
        ) {
            Text("0", style = DDZTypography.Caption)
        }
    }
}
