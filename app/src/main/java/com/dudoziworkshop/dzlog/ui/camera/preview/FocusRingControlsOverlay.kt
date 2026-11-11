package com.dudoziworkshop.dzlog.ui.camera.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The lock is above the focus ring and exposure adjustment below it.
 * Only the interactive controls consume pointer events; the preview remains tappable.
 * A compact lock badge remains visible while the controls are folded away.
 */
@Composable
internal fun FocusRingControlsOverlay(
    focus: TapFocusUiState?,
    locked: Boolean,
    expanded: Boolean,
    exposureIndex: Int,
    minExposureIndex: Int,
    maxExposureIndex: Int,
    exposureStep: Float,
    exposureSupported: Boolean,
    onToggleLock: () -> Unit,
    onExposureIndexChange: (Int) -> Unit,
) {
    if (focus == null) return
    BoxWithConstraints(Modifier.matchParentSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val ringPx = with(density) { 28.dp.toPx() }
        val lockPx = with(density) { 44.dp.toPx() }
        val evWidthPx = with(density) { 226.dp.toPx() }
        val evHeightPx = with(density) { 52.dp.toPx() }
        val spacePx = with(density) { 8.dp.toPx() }
        if (locked) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 10.dp)
                    .background(DDZColor.SelectionEmphasis.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null,
                    modifier = Modifier.size(12.dp), tint = Color.White)
                Text(" AF·AE", color = Color.White, fontSize = 11.sp)
            }
        }
        if (!expanded) return@BoxWithConstraints

        val lockX = (focus.xPx - lockPx / 2f).coerceIn(0f, (widthPx - lockPx).coerceAtLeast(0f))
        val aboveLockY = focus.yPx - ringPx - lockPx - spacePx
        val lockY = if (aboveLockY >= 0f) aboveLockY
            else (focus.yPx - lockPx / 2f).coerceIn(0f, (heightPx - lockPx).coerceAtLeast(0f))
        IconButton(
            modifier = Modifier
                .offsetPx(lockX, lockY)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
            onClick = onToggleLock,
        ) {
            Icon(
                if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (locked) "초점·노출 잠금 해제" else "초점·노출 잠금",
                tint = if (locked) DDZColor.SelectedSoft else Color.White,
            )
        }

        if (exposureSupported && maxExposureIndex > minExposureIndex) {
            val evX = (focus.xPx - evWidthPx / 2f).coerceIn(0f, (widthPx - evWidthPx).coerceAtLeast(0f))
            val belowEvY = focus.yPx + ringPx + spacePx
            val evY = if (belowEvY + evHeightPx <= heightPx) belowEvY
                else (focus.yPx - ringPx - evHeightPx - spacePx).coerceAtLeast(0f)
            Row(
                modifier = Modifier
                    .offsetPx(evX, evY)
                    .width(226.dp)
                    .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.WbSunny, contentDescription = "밝기",
                    tint = DDZColor.SelectedSoft, modifier = Modifier.size(16.dp))
                Slider(
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    value = exposureIndex.toFloat().coerceIn(minExposureIndex.toFloat(), maxExposureIndex.toFloat()),
                    onValueChange = { onExposureIndexChange(it.roundToInt()) },
                    valueRange = minExposureIndex.toFloat()..maxExposureIndex.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = DDZColor.SelectedSoft,
                        activeTrackColor = DDZColor.SelectedSoft,
                        inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                    ),
                )
                val ev = exposureIndex * exposureStep
                val evLabel = if (exposureIndex == 0) "0.0"
                    else String.format(Locale.US, "%+.1f", ev)
                Text(evLabel, color = Color.White, fontSize = 11.sp,
                    modifier = Modifier.width(39.dp))
            }
        }
    }
}

private fun Modifier.offsetPx(x: Float, y: Float): Modifier =
    this.then(Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) })
