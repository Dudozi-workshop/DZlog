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
import androidx.compose.ui.graphics.Color
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
                        Color(0xFFF7F0E5),
                        DDZColor.Background,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-18).dp)
                .size(width = 250.dp, height = 210.dp)
                .blur(22.dp),
        ) {
            val shadow = DDZColor.PrimaryDark.copy(alpha = 0.12f)
            val shadowSoft = DDZColor.Primary.copy(alpha = 0.075f)

            Box(
                modifier = Modifier
                    .offset(x = 148.dp, y = 0.dp)
                    .size(width = 12.dp, height = 182.dp)
                    .rotate(20f)
                    .background(shadowSoft, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = 84.dp, y = 22.dp)
                    .size(width = 92.dp, height = 34.dp)
                    .rotate(-24f)
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = 143.dp, y = 56.dp)
                    .size(width = 98.dp, height = 36.dp)
                    .rotate(24f)
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = 63.dp, y = 91.dp)
                    .size(width = 102.dp, height = 38.dp)
                    .rotate(-18f)
                    .background(shadow, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .offset(x = 127.dp, y = 128.dp)
                    .size(width = 92.dp, height = 34.dp)
                    .rotate(18f)
                    .background(shadow, RoundedCornerShape(50)),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-52).dp, y = 118.dp)
                .size(width = 170.dp, height = 140.dp)
                .blur(28.dp)
                .background(
                    DDZColor.Primary.copy(alpha = 0.035f),
                    RoundedCornerShape(50),
                ),
        )
    }
}
