package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun HomeAmbientBackground(
    modifier: Modifier = Modifier,
) {
    // Approved B treatment: original C botanical silhouette, soft edge and reduced opacity
    // are baked into a tiny transparent PNG; do not reconstruct leaves from Compose shapes.
    val motion = rememberInfiniteTransition(label = "Home botanical light")
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
        return value
    }

    val density = LocalDensity.current
    val swayX = drift(HomeAmbientSpec.BranchLegMillis, HomeAmbientSpec.BranchTravelX.value, "leaf sway x") * density.density
    val swayY = drift(HomeAmbientSpec.BranchLegMillis, HomeAmbientSpec.BranchTravelY.value, "leaf sway y") * density.density
    val swayAngle = drift(HomeAmbientSpec.BranchLegMillis, HomeAmbientSpec.BranchRotation, "leaf sway angle")
    val sunlight = drift(HomeAmbientSpec.SunlightLegMillis, 0.06f, "sunlight")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DDZColor.Background, DDZColor.AmbientWarm, DDZColor.Background),
                ),
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // An anchored botanical shadow fades before reaching the *inner*
                    // (left and bottom) boundaries. This mask stays fixed while the
                    // image sways, so its moving edge never hits a visible hard crop.
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.White,
                            1f to Color.White,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White,
                            0.57f to Color.White,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .clipToBounds(),
        ) {
            val viewportWidth = maxWidth * HomeAmbientSpec.VisibleShadowWidthFraction
            val viewportHeight = maxHeight * HomeAmbientSpec.VisibleShadowHeightFraction
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(viewportWidth, viewportHeight)
                    .clipToBounds(),
            ) {
                val imageWidth = viewportWidth * HomeAmbientSpec.ImageToViewportWidthRatio
                Image(
                    painter = painterResource(R.drawable.home_leaf_shadow_c),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = imageWidth * HomeAmbientSpec.ImageOffsetXFraction,
                            y = viewportHeight * HomeAmbientSpec.ImageOffsetYFraction,
                        )
                        .size(
                            width = imageWidth,
                            height = imageWidth * HomeAmbientSpec.ShadowHeightToWidthRatio,
                        )
                        .graphicsLayer {
                            translationX = swayX
                            translationY = swayY
                            rotationZ = swayAngle
                            alpha = HomeAmbientSpec.ShadowOpacity
                            transformOrigin = TransformOrigin(0.85f, 0f)
                        }
                        .blur(HomeAmbientSpec.BlurRadius),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = HomeAmbientSpec.GlowOffsetX, y = HomeAmbientSpec.GlowOffsetY)
                .size(HomeAmbientSpec.GlowWidth, HomeAmbientSpec.GlowHeight)
                .blur(HomeAmbientSpec.GlowBlurRadius)
                .graphicsLayer { alpha = 0.94f + sunlight }
                .background(
                    DDZColor.Primary.copy(alpha = HomeAmbientSpec.GlowAlpha),
                    RoundedCornerShape(50),
                ),
        )
    }
}
