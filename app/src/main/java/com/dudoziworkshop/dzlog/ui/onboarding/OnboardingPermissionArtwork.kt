package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Decorative photo collection for the permissions page; no permission side effects. */
@Composable
internal fun OnboardingPermissionArtwork() {
    Row(
        modifier = Modifier.height(112.dp).padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(136.dp).height(106.dp)) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).size(104.dp, 66.dp),
                color = Color(0xFFE4D5C1),
                shape = RoundedCornerShape(17.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Folder, null, tint = DDZColor.PrimaryDark,
                        modifier = Modifier.size(38.dp))
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 9.dp)
                    .size(62.dp, 65.dp),
                color = DDZColor.Surface,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 3.dp,
            ) {
                Image(
                    painter = painterResource(R.drawable.onboarding_hero_photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFE5EAD9), Color(0xFFC2D5BD)),
                            ),
                        ),
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 7.dp, top = 11.dp)
                    .size(53.dp, 62.dp),
                color = Color(0xFFF8F6EE),
                shape = RoundedCornerShape(9.dp),
                shadowElevation = 3.dp,
            ) {
                Box(Modifier.padding(5.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ImageIcon, null, tint = DDZColor.SelectedDark,
                        modifier = Modifier.size(24.dp))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("사진도, 기록도,", style = DDZTypography.SettingLabel,
                color = DDZColor.SelectedDark)
            Text("내 공간에.", style = DDZTypography.SectionTitle,
                color = DDZColor.PrimaryDark)
            Text("한 번 설정하면 편리하게 사용할 수 있어요.",
                style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
    }
}
