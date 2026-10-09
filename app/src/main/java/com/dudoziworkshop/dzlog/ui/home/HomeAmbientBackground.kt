package com.dudoziworkshop.dzlog.ui.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun HomeAmbientBackground(
    modifier: Modifier = Modifier,
) {
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
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.Leaf2X, y = HomeAmbientSpec.Leaf2Y)
                    .size(width = HomeAmbientSpec.Leaf2Width, height = HomeAmbientSpec.Leaf2Height)
                    .rotate(HomeAmbientSpec.Leaf2Rotation)
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.Leaf3X, y = HomeAmbientSpec.Leaf3Y)
                    .size(width = HomeAmbientSpec.Leaf3Width, height = HomeAmbientSpec.Leaf3Height)
                    .rotate(HomeAmbientSpec.Leaf3Rotation)
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = HomeAmbientSpec.Leaf4X, y = HomeAmbientSpec.Leaf4Y)
                    .size(width = HomeAmbientSpec.Leaf1Width, height = HomeAmbientSpec.Leaf1Height)
                    .rotate(HomeAmbientSpec.Leaf4Rotation)
                    .background(shadow, RoundedCornerShape(50)),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = HomeAmbientSpec.GlowOffsetX, y = HomeAmbientSpec.GlowOffsetY)
                .size(width = HomeAmbientSpec.GlowWidth, height = HomeAmbientSpec.GlowHeight)
                .blur(HomeAmbientSpec.GlowBlurRadius)
                .background(
                    DDZColor.Primary.copy(alpha = HomeAmbientSpec.GlowAlpha),
                    RoundedCornerShape(50),
                ),
        )
    }
}
