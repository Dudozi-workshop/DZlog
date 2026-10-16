package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.dudoziworkshop.dzlog.domain.gallery.GalleryGridColumnsPolicy

/**
 * Recognizes two-finger pinches over a photo grid, leaving one-finger scrolling
 * and ordinary photo clicks alone. The screen owns columns and their persistence.
 */
@Composable
internal fun Modifier.galleryGridPinch(
    columns: Int,
    enabled: Boolean,
    onColumnsChanged: (Int) -> Unit,
): Modifier {
    val liveColumns by rememberUpdatedState(columns)
    val liveOnColumnsChanged by rememberUpdatedState(onColumnsChanged)
    if (!enabled) return this

    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var isPinching = false
            var startColumns = liveColumns
            var cumulativeScale = 1f
            var lastReportedColumns = startColumns
            var gestureClaimed = false

            do {
                val event = awaitPointerEvent()
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount >= 2) {
                    if (!isPinching) {
                        isPinching = true
                        startColumns = liveColumns
                        lastReportedColumns = startColumns
                        cumulativeScale = 1f
                    } else {
                        cumulativeScale *= event.calculateZoom()
                    }

                    // Ignore tiny finger jitter without stealing scroll/tap gestures.
                    if (cumulativeScale >= 1.08f || cumulativeScale <= (1f / 1.08f)) {
                        gestureClaimed = true
                    }
                    if (gestureClaimed) {
                        val next = GalleryGridColumnsPolicy.columnsForCumulativePinch(
                            startColumns, cumulativeScale
                        )
                        if (next != lastReportedColumns) {
                            lastReportedColumns = next
                            liveOnColumnsChanged(next)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } else if (isPinching) {
                    break
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
