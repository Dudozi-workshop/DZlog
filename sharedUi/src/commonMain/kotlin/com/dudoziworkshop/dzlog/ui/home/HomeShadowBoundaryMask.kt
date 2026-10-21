package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

// Entry edges may touch the actual screen boundary. The *opposite* edges must fade out.
internal enum class HomeShadowOrigin { TopEnd, BottomEnd }

/**
 * Applies a directional alpha mask to the shadow layer, not to the home background.
 * The complete source image remains in memory and can move behind this viewport.
 * The two DstIn masks multiply, so even moving leaves cannot expose a hard left
 * or far-end crop edge. The offscreen layer confines blending to the shadow.
 */
internal fun Modifier.homeShadowBoundaryMask(
    origin: HomeShadowOrigin,
    leftFadeStart: Float,
    leftFadeEnd: Float,
    farFadeStart: Float,
    farFadeEnd: Float,
): Modifier {
    require(leftFadeStart in 0f..1f && leftFadeEnd in 0f..1f && leftFadeStart < leftFadeEnd)
    require(farFadeStart in 0f..1f && farFadeEnd in 0f..1f && farFadeStart < farFadeEnd)

    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    leftFadeStart to Color.Transparent,
                    leftFadeEnd to Color.White,
                    1f to Color.White,
                ),
                blendMode = BlendMode.DstIn,
            )
            val verticalMask = when (origin) {
                HomeShadowOrigin.TopEnd -> Brush.verticalGradient(
                    0f to Color.White,
                    farFadeStart to Color.White,
                    farFadeEnd to Color.Transparent,
                    1f to Color.Transparent,
                )
                HomeShadowOrigin.BottomEnd -> Brush.verticalGradient(
                    0f to Color.Transparent,
                    (1f - farFadeEnd) to Color.Transparent,
                    (1f - farFadeStart) to Color.White,
                    1f to Color.White,
                )
            }
            drawRect(brush = verticalMask, blendMode = BlendMode.DstIn)
        }
}
