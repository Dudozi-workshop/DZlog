package com.dudoziworkshop.dzlog.ui.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.delay

private data class LazyListIndicatorMetrics(
    val canScroll: Boolean,
    val thumbHeightPx: Float,
    val thumbOffsetPx: Float,
)

@Composable
fun LazyListScrollIndicator(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    widthDp: Dp = 3.dp,
    hideDelayMs: Long = 600L
) {
    var showScrollIndicator by remember(listState) { mutableStateOf(false) }
    val indicatorAlpha by animateFloatAsState(
        if (showScrollIndicator) 1f else 0f,
        label = "lazyListScrollIndicator"
    )

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            showScrollIndicator = true
        } else {
            delay(hideDelayMs)
            showScrollIndicator = false
        }
    }

    val metrics by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                return@derivedStateOf LazyListIndicatorMetrics(
                    canScroll = false,
                    thumbHeightPx = 0f,
                    thumbOffsetPx = 0f,
                )
            }
            val canScroll = layoutInfo.totalItemsCount > visibleItems.size
            if (!canScroll || layoutInfo.totalItemsCount <= 0) {
                return@derivedStateOf LazyListIndicatorMetrics(
                    canScroll = false,
                    thumbHeightPx = 0f,
                    thumbOffsetPx = 0f,
                )
            }
            val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
            val avgItemHeightPx = visibleItems.map { it.size }.average().toFloat().takeIf { it > 0f } ?: 1f
            val totalContentHeightPx = (avgItemHeightPx * layoutInfo.totalItemsCount).coerceAtLeast(viewportHeightPx.toFloat())
            val thumbHeightPx = ((viewportHeightPx.toFloat() / totalContentHeightPx) * viewportHeightPx)
                .coerceIn(24f, viewportHeightPx.toFloat())
            val firstVisible = visibleItems.first()
            val scrollOffsetPx = (firstVisible.index * avgItemHeightPx) - firstVisible.offset
            val maxScrollPx = (totalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)
            val thumbOffsetPx = ((scrollOffsetPx / maxScrollPx) * (viewportHeightPx - thumbHeightPx))
                .coerceIn(0f, (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f))

            LazyListIndicatorMetrics(
                canScroll = true,
                thumbHeightPx = thumbHeightPx,
                thumbOffsetPx = thumbOffsetPx,
            )
        }
    }

    if (showScrollIndicator && metrics.canScroll) {
        val density = LocalDensity.current

        Box(
            modifier = modifier
                .padding(end = 1.dp)
                .width(widthDp)
                .height(with(density) { metrics.thumbHeightPx.toDp() })
                .offset(y = with(density) { metrics.thumbOffsetPx.toDp() })
                .alpha(indicatorAlpha)
                .background(DDZColor.TextMuted.copy(alpha = 0.5f), RoundedCornerShape(99.dp))
        )
    }
}
