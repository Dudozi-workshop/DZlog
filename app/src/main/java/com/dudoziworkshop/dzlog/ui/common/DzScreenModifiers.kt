package com.dudoziworkshop.dzlog.ui.common

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private val FallbackButtonNavBarHeight = 48.dp

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
        base
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        )
        .padding(bottom = extraBottomPadding)
    }


@Composable
fun Modifier.dzScaffoldContent(): Modifier =
    this
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        )
