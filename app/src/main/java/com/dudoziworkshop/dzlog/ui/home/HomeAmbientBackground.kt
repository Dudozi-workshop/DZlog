package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun HomeAmbientBackground(
    modifier: Modifier = Modifier,
) {
    val animationEnabled = androidx.compose.ui.platform.LocalView.current.isAttachedToWindow &&
        androidx.compose.ui.platform.LocalContext.current.resources.configuration.fontScale > 0f
    // Compose respects system animator-duration-scale for infinite transitions.
    val motion = rememberInfiniteTransition(label = "Home ambient")
    @Composable
    fun drift(duration: Int, amplitude: Float, label: String): Float {
        val value by motion.animateFloat(
            initialValue = -amplitude,
            targetValue = amplitude,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = label,
        )
        return if (animationEnabled) value else 0f
    }
    val density = LocalDensity.current
    val branchX = with(density) { drift(HomeAmbientSpec.BranchLegMillis, HomeAmbientSpec.BranchTravelX.value, "branch x").dp.toPx() }
    val branchY = with(density) { drift(HomeAmbientSpec.BranchLegMillis, HomeAmbientSpec.BranchTravelY.value, "branch y").dp.toPx() }
    val branchAngle = drift(HomeAmbientSpec.BranchLegMillis, HomeAmbientSpec.BranchRotation, "branch rotation")
    val leaf1 = drift(HomeAmbientSpec.Leaf1LegMillis, HomeAmbientSpec.LeafRotation, "leaf 1")
    val leaf2 = drift(HomeAmbientSpec.Leaf2LegMillis, -HomeAmbientSpec.LeafRotation * 0.75f, "leaf 2")
    val leaf3 = drift(HomeAmbientSpec.Leaf3LegMillis, HomeAmbientSpec.LeafRotation * 0.85f, "leaf 3")
    val leaf4 = drift(HomeAmbientSpec.Leaf4LegMillis, -HomeAmbientSpec.LeafRotation, "leaf 4")
    val sunlight = drift(HomeAmbientSpec.SunlightLegMillis, 0.06f, "sunlight")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DDZColor.Background,
                        DDZColor.AmbientWarm,
                        DDZColor.Background,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = HomeAmbientSpec.CanvasOffsetX, y = HomeAmbientSpec.CanvasOffsetY)
                .size(width = HomeAmbientSpec.CanvasWidth, height = HomeAmbientSpec.CanvasHeight)
                .graphicsLayer {
                    translationX = branchX
                    translationY = branchY
                    rotationZ = branchAngle
                    transformOrigin = TransformOrigin(0.8f, 0f)
                }
                .blur(HomeAmbientSpec.BlurRadius),
        ) {
            val shadow = DDZColor.PrimaryDark.copy(alpha = HomeAmbientSpec.ShadowAlpha)
            val shadowSoft = DDZColor.Primary.copy(alpha = HomeAmbientSpec.StemAlpha)

            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.StemX, y = HomeAmbientSpec.StemY)
                    .size(width = HomeAmbientSpec.StemWidth, height = HomeAmbientSpec.StemHeight)
                    .rotate(HomeAmbientSpec.StemRotation)
                    .background(shadowSoft, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.Leaf1X, y = HomeAmbientSpec.Leaf1Y)
                    .size(width = HomeAmbientSpec.Leaf1Width, height = HomeAmbientSpec.Leaf1Height)
                    .rotate(HomeAmbientSpec.Leaf1Rotation)
                    .graphicsLayer { rotationZ = leaf1 }
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.Leaf2X, y = HomeAmbientSpec.Leaf2Y)
                    .size(width = HomeAmbientSpec.Leaf2Width, height = HomeAmbientSpec.Leaf2Height)
                    .rotate(HomeAmbientSpec.Leaf2Rotation)
                    .graphicsLayer { rotationZ = leaf2 }
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.Leaf3X, y = HomeAmbientSpec.Leaf3Y)
                    .size(width = HomeAmbientSpec.Leaf3Width, height = HomeAmbientSpec.Leaf3Height)
                    .rotate(HomeAmbientSpec.Leaf3Rotation)
                    .graphicsLayer { rotationZ = leaf3 }
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.Leaf4X, y = HomeAmbientSpec.Leaf4Y)
                    .size(width = HomeAmbientSpec.Leaf1Width, height = HomeAmbientSpec.Leaf1Height)
                    .rotate(HomeAmbientSpec.Leaf4Rotation)
                    .graphicsLayer { rotationZ = leaf4 }
                    .background(shadow, RoundedCornerShape(50)),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = HomeAmbientSpec.GlowOffsetX, y = HomeAmbientSpec.GlowOffsetY)
                .size(width = HomeAmbientSpec.GlowWidth, height = HomeAmbientSpec.GlowHeight)
                .blur(HomeAmbientSpec.GlowBlurRadius)
                .graphicsLayer { alpha = 0.94f + sunlight }
                .background(
                    DDZColor.Primary.copy(alpha = HomeAmbientSpec.GlowAlpha),
                    RoundedCornerShape(50),
                ),
        )
    }
}
