package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Editorial introduction. All examples are non-persistent. */
@Composable
internal fun OnboardingBrandIntro(heroHeight: Dp = 300.dp, compact: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 12.dp)) {
        if (!compact) Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("사진을,", style = DDZTypography.HomeMainTitle, color = DDZColor.TextPrimary)
            Text("더 가치 있는 기록으로.", style = DDZTypography.HomeMainTitle,
                color = DDZColor.PrimaryDark)
        }
        Text("일상부터 현장까지,", style = DDZTypography.SectionTitle,
            color = DDZColor.SelectedDark)
        Text("사진과 기록을 함께 남겨보세요.", style = DDZTypography.Body,
            color = DDZColor.TextSecondary)
        OnboardingPhotoShowcase(
            labels = listOf("기록명", "날짜", "번호"),
            values = listOf("관찰 기록", "26.10.15", "001"),
            height = heroHeight,
        )
        Text("한 장의 사진이, 더 오래 기억되는 기록이 됩니다.",
            style = DDZTypography.Caption, color = DDZColor.TextSecondary)
    }
}

@Composable
internal fun OnboardingFeatureIntro(previewHeight: Dp = 146.dp, compact: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp)) {
        Text(
            "사진에 기록을 더하고,\n정리까지 한 번에.",
            style = DDZTypography.HomeMainTitle, color = DDZColor.TextPrimary,
        )
        Text(
            "촬영부터 보관까지, 필요한 기능만 담았어요.",
            style = DDZTypography.Body, color = DDZColor.TextSecondary,
        )
        // The approved reference shows one editorial feature card with the
        // device on the right, not a second full-width fake camera screen.
        Surface(
            color = DDZColor.SelectedSoft.copy(alpha = 0.54f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(if (compact) 9.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("01", style = DDZTypography.Caption, color = DDZColor.SelectedDark)
                    Text(
                        "사진과 기록,\n내 방식대로.",
                        style = DDZTypography.SectionTitle,
                        color = DDZColor.TextPrimary,
                    )
                    Text(
                        "기록표를 함께 담거나\n표 없이 촬영할 수 있어요.",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.onboarding_reference_hero),
                    contentDescription = "사진에 기록표를 겹친 촬영 예시",
                    modifier = Modifier
                        .width(if (compact) 120.dp else 136.dp)
                        .height(if (compact) 136.dp else 160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        CompactFeature(
            icon = Icons.Default.Folder,
            number = "02",
            title = "촬영과 동시에, 원하는 곳에.",
            detail = "내 파일명과 폴더 규칙에 맞춰 저장해요.",
            compact = compact,
        )
        CompactFeature(
            icon = Icons.Default.Description,
            number = "03",
            title = "쌓인 기록도, 찾기 쉽게.",
            detail = "최근 사진과 폴더를 바로 찾아보세요.",
            compact = compact,
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
    icon: ImageVector,
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
            modifier = Modifier.fillMaxWidth().padding(if (compact) 6.dp else 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 5.dp),
            ) {
                Text(number, style = DDZTypography.Caption, color = DDZColor.SelectedDark)
                Text(title, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
                Text(detail, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
            Box(
                Modifier.size(if (compact) 70.dp else 82.dp, if (compact) 60.dp else 78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DDZColor.SelectedSoft),
            ) {
                Image(
                    painter = painterResource(
                        if (number == "02") R.drawable.onboarding_starter_photo
                        else R.drawable.onboarding_gallery_photo
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(if (compact) 60.dp else 78.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.98f,
                )
                if (number == "02") {
                    // Small folder preview for storage rules.
                    Surface(
                        color = DDZColor.Surface.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.align(Alignment.Center).padding(4.dp),
                    ) {
                        Column(
                            Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null,
                                    tint = DDZColor.PrimaryDark, modifier = Modifier.size(13.dp))
                                Text("식물 관찰", style = DDZTypography.Caption,
                                    color = DDZColor.TextPrimary)
                            }
                            Text("2026.10", style = DDZTypography.Caption,
                                color = DDZColor.TextSecondary)
                        }
                    }
                } else {
                    // Contact-sheet preview for photo organization.
                    Row(
                        modifier = Modifier.align(Alignment.Center).padding(7.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        repeat(3) { index ->
                            Image(
                                painter = painterResource(
                                    when (index) {
                                        0 -> R.drawable.onboarding_hero_photo
                                        1 -> R.drawable.onboarding_gallery_photo
                                        else -> R.drawable.onboarding_starter_photo
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(width = 20.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop,
                                alpha = if (index == 1) 0.70f else 1f,
                            )
                        }
                    }
                }
                Surface(
                    color = DDZColor.Surface.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                ) {
                    Icon(icon, null, tint = DDZColor.PrimaryDark,
                        modifier = Modifier.padding(6.dp).size(18.dp))
                }
            }
        }
    }
}
