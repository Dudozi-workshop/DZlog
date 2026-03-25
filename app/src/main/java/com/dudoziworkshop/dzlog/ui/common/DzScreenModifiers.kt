package com.dudoziworkshop.dzlog.ui.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private val FallbackThreeButtonNavBarHeight = 48.dp

@SuppressLint("DiscouragedApi", "InternalInsetResource")
private fun Context.resolveThreeButtonBaselinePx(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId <= 0) return 0
    return resources.getDimensionPixelSize(resourceId)
}

@Composable
fun rememberThreeButtonNavEquivalentBottomPadding(): Dp {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val baselinePx = maxOf(
        context.resolveThreeButtonBaselinePx(),
        with(density) { FallbackThreeButtonNavBarHeight.roundToPx() },
    )
    val rootInsets = ViewCompat.getRootWindowInsets(view)
    val currentInsetPx = rootInsets
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())
        ?.bottom
        ?: 0
    val targetBottomPx = maxOf(baselinePx, currentInsetPx)
    return with(density) { targetBottomPx.toDp() }
}

@Composable
fun Modifier.dzScreen(): Modifier =
    this
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        )
    


@Composable
fun Modifier.dzScaffoldContent(): Modifier =
    this
        .fillMaxSize()
        .background(DDZColor.Background)
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        )
