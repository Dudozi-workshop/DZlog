package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.dudoziworkshop.dzlog.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Editorial introduction. All examples are non-persistent. */
@Composable
internal fun OnboardingBrandIntro(heroHeight: Dp = 300.dp, compact: Boolean = false) {
    Column(
        modifier = Modifier.padding(top = if (compact) 10.dp else 16.dp),
        // Keep the hero directly below the headline; the one-line caption sits below the photo.
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        // Editorial hierarchy is local to onboarding: avoid changing the app-wide
        // HomeMainTitle style used on other screens.
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            val titleSize = when {
                maxWidth < 310.dp -> 26.sp
                maxWidth < 365.dp -> 29.sp
                else -> 31.sp
            }
            val titleStyle = DDZTypography.HomeMainTitle.copy(
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                lineHeight = titleSize * 1.18f,
                letterSpacing = (-0.6).sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "사진에 기록을 더해,",
                    style = titleStyle, color = DDZColor.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "정리를 더 쉽게.",
                    style = titleStyle, color = DDZColor.PrimaryDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        }
        OnboardingPhotoShowcase(
            labels = listOf("기록명", "날짜", "번호"),
            values = listOf("관찰 기록", "26.10.15", "001"),
            height = heroHeight,
            edgeToEdge = true,
        )
    }
}

@Composable
internal fun OnboardingFeatureIntro(previewHeight: Dp = 146.dp, compact: Boolean = false) {
    val image = androidx.compose.ui.graphics.ImageBitmap.imageResource(R.drawable.onboarding_feature_v5)
    Column(
        modifier = Modifier.padding(top = if (compact) 2.dp else 5.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
    ) {
        Text(
            "사진에 기록을 더하고,\n정리까지 한 번에.",
            style = DDZTypography.HomeMainTitle.copy(
                fontSize = if (compact) 26.sp else 28.sp,
                lineHeight = if (compact) 32.sp else 35.sp,
            ),
            color = DDZColor.TextPrimary,
        )
        FeaturePhotoCard(image, 398, 311, "사진에 기록표를 겹친 촬영 예시")
        FeaturePhotoCard(image, 724, 282, "촬영과 동시에, 원하는 곳에.")
        FeaturePhotoCard(image, 1021, 311, "쌓인 기록도, 찾기 쉽게.")
    }
}

@Composable
private fun FeaturePhotoCard(
    image: androidx.compose.ui.graphics.ImageBitmap,
    top: Int,
    sourceHeight: Int,
    label: String,
) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxWidth()
            .aspectRatio(673f / sourceHeight)
            .semantics { contentDescription = label },
    ) {
        drawImage(
            image = image,
            srcOffset = androidx.compose.ui.unit.IntOffset(177, top),
            srcSize = androidx.compose.ui.unit.IntSize(673, sourceHeight),
            dstSize = androidx.compose.ui.unit.IntSize(
                size.width.toInt().coerceAtLeast(1),
                size.height.toInt().coerceAtLeast(1),
            ),
        )
    }
}

@Composable
private fun FeatureModeLabel(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) DDZColor.SelectedSoft else DDZColor.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) DDZColor.Selected else DDZColor.Border),
        modifier = Modifier.clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = isSelected },
    ) {
        Text(
            label, style = DDZTypography.Caption,
            color = if (isSelected) DDZColor.SelectedDark else DDZColor.TextSecondary,
            modifier = Modifier.heightIn(min = 40.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CompactFeature(
    number: String,
    title: String,
    detail: String,
    compact: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DDZColor.Surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 7.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
            ) {
                Text(number + if (number == "02") " · 저장" else " · 앨범",
                    style = DDZTypography.Caption, color = DDZColor.SelectedDark)
                Text(
                    title,
                    style = DDZTypography.SettingLabel.copy(
                        fontSize = if (compact) 13.sp else 14.sp,
                        lineHeight = if (compact) 17.sp else 19.sp,
                    ),
                    color = DDZColor.TextPrimary,
                    maxLines = 2,
                )
                Text(
                    detail,
                    style = DDZTypography.Caption.copy(
                        fontSize = if (compact) 11.sp else 12.sp,
                        lineHeight = if (compact) 15.sp else 16.sp,
                    ),
                    color = DDZColor.TextSecondary,
                    maxLines = 2,
                )
            }
            Box(
                Modifier.size(
                    width = if (compact) 77.dp else 87.dp,
                    height = if (compact) 65.dp else 75.dp,
                ).clip(RoundedCornerShape(12.dp))
                    .background(DDZColor.SelectedSoft),
            ) {
                if (number == "02") {
                    Image(
                        painter = painterResource(R.drawable.onboarding_starter_photo),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(if (compact) 65.dp else 75.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.72f,
                    )
                    Surface(
                        color = DDZColor.Surface.copy(alpha = 0.97f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Folder, contentDescription = null,
                                tint = DDZColor.PrimaryDark, modifier = Modifier.size(13.dp),
                            )
                            Text(
                                "내 폴더",
                                fontSize = 10.sp,
                                color = DDZColor.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.onboarding_hero_photo),
                            contentDescription = null,
                            modifier = Modifier.size(width = 33.dp, height = 53.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Image(
                            painter = painterResource(R.drawable.onboarding_starter_photo),
                            contentDescription = null,
                            modifier = Modifier.size(width = 33.dp, height = 53.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}
