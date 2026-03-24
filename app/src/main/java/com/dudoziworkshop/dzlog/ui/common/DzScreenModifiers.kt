package com.dudoziworkshop.dzlog.ui.common

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private val FallbackButtonNavBarHeight = 48.dp
private const val SHOW_DZ_SCREEN_INSET_DEBUG = true

private data class DzScreenInsetDebugState(
    val baseline: Int = 0,
    val currentInset: Int = 0,
    val extra: Int = 0,
)

private var dzScreenInsetDebugState by mutableStateOf(DzScreenInsetDebugState())

private fun Context.resolveButtonNavBarHeightPx(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId <= 0) return 0
    return resources.getDimensionPixelSize(resourceId)
}

@Composable
fun Modifier.dzScreen(): Modifier =
    this.let { base ->
        val context = LocalContext.current
        val density = LocalDensity.current
        val fallbackPx = with(density) { FallbackButtonNavBarHeight.roundToPx() }
        val resolvedNavBarHeightPx = context.resolveButtonNavBarHeightPx()
        val baselineBottomPadding = with(density) { maxOf(resolvedNavBarHeightPx, fallbackPx).toDp() }
        val currentInsetBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val extraBottomPadding = if (baselineBottomPadding > currentInsetBottomPadding) {
            baselineBottomPadding - currentInsetBottomPadding
        } else {
            0.dp
        }
        dzScreenInsetDebugState = DzScreenInsetDebugState(
            baseline = baselineBottomPadding.value.toInt(),
            currentInset = currentInsetBottomPadding.value.toInt(),
            extra = extraBottomPadding.value.toInt(),
        )
        base
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        )
    }


@Composable
fun Modifier.dzScaffoldContent(): Modifier =
    this
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        )

@Composable
fun DzScreenInsetDebugOverlay(modifier: Modifier = Modifier) {
    if (!SHOW_DZ_SCREEN_INSET_DEBUG) return
    val state = dzScreenInsetDebugState
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "base=${state.baseline}\ninset=${state.currentInset}\nextra=${state.extra}",
                color = Color.White,
            )
        }
    }
}
