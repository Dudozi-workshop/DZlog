package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Decorative only. Crops three user-supplied original pictures from the approved page-2 resource. */
@Composable
internal fun OnboardingPermissionArtwork() {
    val source = ImageBitmap.imageResource(R.drawable.onboarding_feature_v5)
    Box(
        modifier = Modifier.fillMaxWidth().height(166.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF9F5EA), Color(0xFFECE2D3)))),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                .size(width = 178.dp, height = 86.dp).graphicsLayer(rotationZ = 6f),
            color = Color(0xFFF8F0E5),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null,
                        tint = Color(0xFFB69678), modifier = Modifier.size(28.dp))
                    Text("DZlog 기록", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = DDZColor.PrimaryDark)
                }
                Text("2026.10 ~", fontSize = 11.sp, color = Color(0xFFA28F7C))
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OriginalPhoto(source, 492, 473, 146, 129, -7f)
            OriginalPhoto(source, 550, 1210, 115, 90, 2f)
            OriginalPhoto(source, 687, 1210, 118, 90, 7f)
        }
    }
}

@Composable
private fun OriginalPhoto(
    image: ImageBitmap, sx: Int, sy: Int, sw: Int, sh: Int, rotation: Float,
) {
    Surface(
        modifier = Modifier.size(91.dp, 77.dp).graphicsLayer(rotationZ = rotation),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 4.dp,
    ) {
        Canvas(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(7.dp))) {
            drawImage(
                image = image,
                srcOffset = IntOffset(sx, sy),
                srcSize = IntSize(sw, sh),
                dstSize = IntSize(
                    size.width.toInt().coerceAtLeast(1),
                    size.height.toInt().coerceAtLeast(1),
                ),
            )
        }
    }
}
