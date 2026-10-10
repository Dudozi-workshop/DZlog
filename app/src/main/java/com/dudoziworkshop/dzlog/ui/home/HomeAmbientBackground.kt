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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun HomeAmbientBackground(
    modifier: Modifier = Modifier,
) {
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DDZColor.Background, DDZColor.AmbientWarm, DDZColor.Background),
                ),
            ),
    ) {
        // Both the viewing window and the image use viewport-relative geometry.
        // The drawable is never permanently cropped.
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val windowWidth = maxWidth * HomeAmbientSpec.ViewportWidthFraction
        val windowHeight = maxHeight * HomeAmbientSpec.ViewportHeightFraction
        val imageWidth = maxWidth * HomeAmbientSpec.ImageWidthFraction
        val imageHeight = imageWidth * HomeAmbientSpec.ImageHeightToWidthRatio
        val imageRightOffset = maxWidth * HomeAmbientSpec.ImageRightOffsetFraction
        val imageTopOffset = maxHeight * HomeAmbientSpec.ImageTopOffsetFraction

        // The motion envelope is proportional to the visible window, not device pixels.
        val safeAmplitudeX = minOf(
            HomeAmbientSpec.BranchTravelX.value,
            windowWidth.value * HomeAmbientSpec.MotionSafeXFraction,
        )
        val safeAmplitudeY = minOf(
            HomeAmbientSpec.BranchTravelY.value,
            windowHeight.value * HomeAmbientSpec.MotionSafeYFraction,
        )
        val density = LocalDensity.current.density
        // Independent cycles avoid the mechanical feeling of all axes reversing together.
        val swayX = drift(HomeAmbientSpec.SwayXLegMillis, safeAmplitudeX, "leaf sway x") * density
        val swayY = drift(HomeAmbientSpec.SwayYLegMillis, safeAmplitudeY, "leaf sway y") * density
        val swayAngle = drift(
            HomeAmbientSpec.RotationLegMillis,
            HomeAmbientSpec.BranchRotation,
            "leaf sway angle",
        )
        val swayScale = drift(
            HomeAmbientSpec.ScaleLegMillis,
            HomeAmbientSpec.BranchScale,
            "leaf sway scale",
        )
        val swayOpacity = drift(
            HomeAmbientSpec.OpacityLegMillis,
            HomeAmbientSpec.ShadowOpacityDrift,
            "leaf light variation",
        )
        val sunlight = drift(HomeAmbientSpec.SunlightLegMillis, 0.06f, "sunlight")

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = windowWidth, height = windowHeight)
                .homeShadowBoundaryMask(
                    origin = HomeShadowOrigin.TopEnd,
                    leftFadeStart = HomeAmbientSpec.LeftFadeStart,
                    leftFadeEnd = HomeAmbientSpec.LeftFadeEnd,
                    farFadeStart = HomeAmbientSpec.FarFadeStart,
                    farFadeEnd = HomeAmbientSpec.FarFadeEnd,
                )
                .clipToBounds(),
        ) {
            Image(
                painter = painterResource(R.drawable.home_leaf_shadow_c),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = screenWidth * HomeAmbientSpec.ImageRightOffsetFraction,
                        y = screenHeight * HomeAmbientSpec.ImageTopOffsetFraction,
                    )
                    .size(width = imageWidth, height = imageHeight)
                    .graphicsLayer {
                        translationX = swayX
                        translationY = swayY
                        rotationZ = swayAngle
                        scaleX = 1f + swayScale
                        scaleY = 1f + swayScale
                        alpha = (HomeAmbientSpec.ShadowOpacity + swayOpacity).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.9f, 0f)
                    }
                    .blur(HomeAmbientSpec.BlurRadius),
            )
        }

        // The surrounding light remains separate from the masked leaf silhouette.
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
